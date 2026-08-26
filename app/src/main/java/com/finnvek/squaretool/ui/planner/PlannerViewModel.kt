package com.finnvek.squaretool.ui.planner

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.ProjectCellEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.local.SquareDesignWithRounds
import com.finnvek.squaretool.data.repository.SquareToolRepository
import com.finnvek.squaretool.domain.algorithm.GenerationRequest
import com.finnvek.squaretool.domain.algorithm.LayoutGenerator
import com.finnvek.squaretool.domain.algorithm.LayoutMode
import com.finnvek.squaretool.domain.algorithm.PlannerHistory
import com.finnvek.squaretool.domain.algorithm.WeightedDesign
import com.finnvek.squaretool.domain.model.CellCoordinate
import com.finnvek.squaretool.domain.model.CellState
import com.finnvek.squaretool.domain.model.GradientDirection
import com.finnvek.squaretool.domain.model.GridSnapshot
import com.finnvek.squaretool.domain.model.MirrorDirection
import com.finnvek.squaretool.render.MotifTemplateRegistry
import com.finnvek.squaretool.render.SquareDesignVisual
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Stable
class PlannerViewModel(
    private val repository: SquareToolRepository,
    private val savedStateHandle: SavedStateHandle,
    private val generationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val projectId: String = checkNotNull(savedStateHandle[PROJECT_ID_KEY])
    private val _state =
        MutableStateFlow(
            PlannerUiState(
                projectId = projectId,
                accessibleGridMode = savedStateHandle[ACCESSIBLE_GRID_KEY] ?: false,
            ),
        )
    val state: StateFlow<PlannerUiState> = _state.asStateFlow()

    private val saveRequests = Channel<GridSnapshot>(Channel.CONFLATED)
    private var project: ProjectEntity? = null
    private var history: PlannerHistory? = null
    private var expectedDatabaseSnapshot: GridSnapshot? = null
    private var dragStart: GridSnapshot? = null
    private var dragSnapshot: GridSnapshot? = null
    private var dragTool: PlannerTool = PlannerTool.PAINT
    private val dragVisitedCoordinates = mutableSetOf<CellCoordinate>()
    private var dragGenerationResult: PlannerLayoutGenerationResult? = null
    private var requestedDesignId: String? = null

    init {
        observePlanner()
        persistSnapshots()
    }

    fun selectCell(coordinate: CellCoordinate) {
        if (_state.value.snapshot
                ?.size
                ?.contains(coordinate) != true
        ) {
            return
        }
        _state.update { it.copy(selectedCoordinate = coordinate) }
    }

    fun selectDesign(designId: String?) {
        if (designId != null && _state.value.designs.none { it.id == designId }) {
            requestedDesignId = designId
            return
        }
        requestedDesignId = null
        _state.update { it.copy(selectedDesignId = designId) }
    }

    fun setTool(tool: PlannerTool) {
        if (tool == PlannerTool.PROGRESS && !_state.value.trackingEnabled) return
        _state.update { it.copy(tool = tool) }
    }

    fun beginToolDrag(tool: PlannerTool) {
        if (tool == PlannerTool.SELECT) return
        if (tool == PlannerTool.PROGRESS && !_state.value.trackingEnabled) return
        val snapshot = _state.value.snapshot ?: return
        dragTool = tool
        dragStart = snapshot
        dragSnapshot = snapshot
        dragGenerationResult = _state.value.layoutGenerationResult
        dragVisitedCoordinates.clear()
    }

    fun applyToolDuringDrag(coordinate: CellCoordinate) {
        val working = dragSnapshot ?: return
        if (!working.size.contains(coordinate)) return
        if (!dragVisitedCoordinates.add(coordinate)) return
        val cell = working[coordinate]
        val changed =
            applyPlannerTool(
                cell = cell,
                tool = dragTool,
                selectedDesignId = _state.value.selectedDesignId,
                trackingEnabled = _state.value.trackingEnabled,
            )
        if (changed == cell) return
        val next = working.updated(changed)
        dragSnapshot = next
        publishSnapshot(next, updateHistoryControls = false)
    }

    fun endToolDrag() {
        val before = dragStart
        val after = dragSnapshot
        val completedTool = dragTool
        val previousGenerationResult = dragGenerationResult
        dragStart = null
        dragSnapshot = null
        dragGenerationResult = null
        dragVisitedCoordinates.clear()
        if (before == null || after == null) return
        if (before == after) {
            publishSnapshot(before, clearGenerationResult = false)
            _state.update { it.copy(layoutGenerationResult = previousGenerationResult) }
            return
        }
        history?.record(completedTool.historyLabel, after)
        publishSnapshot(after)
        requestSave(after)
    }

    fun cancelToolDrag() {
        val before = dragStart
        val previousGenerationResult = dragGenerationResult
        dragStart = null
        dragSnapshot = null
        dragGenerationResult = null
        dragVisitedCoordinates.clear()
        if (before != null) {
            publishSnapshot(before, clearGenerationResult = false)
            _state.update { it.copy(layoutGenerationResult = previousGenerationResult) }
        }
    }

    fun beginPaintDrag() = beginToolDrag(PlannerTool.PAINT)

    fun paintDuringDrag(coordinate: CellCoordinate) = applyToolDuringDrag(coordinate)

    fun endPaintDrag() = endToolDrag()

    fun cancelPaintDrag() = cancelToolDrag()

    fun paintCell(coordinate: CellCoordinate) {
        beginPaintDrag()
        paintDuringDrag(coordinate)
        endPaintDrag()
    }

    fun toggleSelectedLock() {
        updateSelected("Lock") { it.copy(locked = !it.locked) }
    }

    fun toggleSelectedCompletion() {
        if (!_state.value.trackingEnabled) return
        updateSelected("Completion") { it.copy(completed = !it.completed) }
    }

    fun clearSelectedCell() {
        updateSelected("Clear cell") { cell ->
            if (cell.locked) cell else cell.copy(designId = null, completed = false)
        }
    }

    fun assignDesignToSelected(designId: String?) {
        selectDesign(designId)
        updateSelected("Assign design") { cell ->
            if (cell.locked) cell else cell.copy(designId = designId)
        }
    }

    fun clearUnlockedCells() {
        val snapshot = _state.value.snapshot ?: return
        val next =
            GridSnapshot.of(
                snapshot.size,
                snapshot.cells.map { cell ->
                    if (cell.locked) cell else cell.copy(designId = null, completed = false)
                },
            )
        recordAndSave("Clear unlocked", next)
    }

    fun undo() {
        val plannerHistory = history ?: return
        if (!plannerHistory.canUndo) return
        val next = plannerHistory.undo()
        publishSnapshot(next)
        requestSave(next)
    }

    fun redo() {
        val plannerHistory = history ?: return
        if (!plannerHistory.canRedo) return
        val next = plannerHistory.redo()
        publishSnapshot(next)
        requestSave(next)
    }

    fun setAccessibleGridMode(enabled: Boolean) {
        savedStateHandle[ACCESSIBLE_GRID_KEY] = enabled
        _state.update { it.copy(accessibleGridMode = enabled) }
    }

    fun setDefaultOverwriteCompleted(enabled: Boolean) {
        _state.update { current ->
            if (current.generator.overwriteCompleted == enabled) {
                current
            } else {
                current.copy(
                    generator = current.generator.copy(overwriteCompleted = enabled, issue = null),
                    layoutGenerationResult = null,
                )
            }
        }
    }

    fun openGenerator() {
        _state.update { it.copy(generator = it.generator.copy(isOpen = true, issue = null)) }
    }

    fun closeGenerator() {
        _state.update { it.copy(generator = it.generator.copy(isOpen = false, issue = null)) }
    }

    fun setGeneratorMode(mode: PlannerGeneratorMode) {
        _state.update {
            it.copy(
                generator = it.generator.copy(mode = mode, issue = null),
                layoutGenerationResult = null,
            )
        }
    }

    fun setGeneratorSeed(seed: String) {
        savedStateHandle[GENERATOR_SEED_KEY] = seed
        _state.update {
            it.copy(
                generator = it.generator.copy(seed = seed, issue = null),
                layoutGenerationResult = null,
            )
        }
    }

    fun setGeneratorBandWidth(width: Int) {
        _state.update {
            it.copy(
                generator = it.generator.copy(bandWidth = width.coerceIn(1, 50), issue = null),
                layoutGenerationResult = null,
            )
        }
    }

    fun setAvoidNeighbors(enabled: Boolean) {
        _state.update {
            it.copy(
                generator = it.generator.copy(avoidOrthogonalNeighbors = enabled, issue = null),
                layoutGenerationResult = null,
            )
        }
    }

    fun setOverwriteCompleted(enabled: Boolean) {
        _state.update {
            it.copy(
                generator = it.generator.copy(overwriteCompleted = enabled, issue = null),
                layoutGenerationResult = null,
            )
        }
    }

    fun toggleGeneratorDesign(designId: String) {
        _state.update { current ->
            current.copy(
                designs =
                    current.designs.map { option ->
                        if (option.id == designId) {
                            option.copy(includedInGeneration = !option.includedInGeneration)
                        } else {
                            option
                        }
                    },
                generator = current.generator.copy(issue = null),
                layoutGenerationResult = null,
            )
        }
    }

    fun adjustGeneratorWeight(
        designId: String,
        delta: Double,
    ) {
        _state.update { current ->
            current.copy(
                designs =
                    current.designs.map { option ->
                        if (option.id == designId) {
                            option.copy(weight = (option.weight + delta).coerceIn(0.25, 20.0))
                        } else {
                            option
                        }
                    },
                generator = current.generator.copy(issue = null),
                layoutGenerationResult = null,
            )
        }
    }

    fun moveGeneratorDesign(
        designId: String,
        delta: Int,
    ) {
        _state.update { current ->
            val mutable = current.designs.toMutableList()
            val from = mutable.indexOfFirst { it.id == designId }
            if (from < 0) return@update current
            val to = (from + delta).coerceIn(0, mutable.lastIndex)
            if (from == to) return@update current
            val option = mutable.removeAt(from)
            mutable.add(to, option)
            current.copy(
                designs = mutable,
                generator = current.generator.copy(issue = null),
                layoutGenerationResult = null,
            )
        }
    }

    fun generateLayout() {
        val current = _state.value
        val snapshot = current.snapshot ?: return
        val seed = current.generator.seed.toLongOrNull()
        if (seed == null) {
            setGeneratorIssue(PlannerGeneratorIssue.INVALID_SEED)
            return
        }
        val selected = current.designs.filter(PlannerDesignOption::includedInGeneration)
        val needsDesigns =
            !current.generator.mode.name
                .startsWith("MIRROR_")
        if (needsDesigns && selected.isEmpty()) {
            setGeneratorIssue(PlannerGeneratorIssue.SELECT_A_DESIGN)
            return
        }
        if (current.generator.mode == PlannerGeneratorMode.CHECKER && selected.size != 2) {
            setGeneratorIssue(PlannerGeneratorIssue.CHECKER_REQUIRES_TWO_DESIGNS)
            return
        }

        _state.update { it.copy(isGenerating = true, generator = it.generator.copy(issue = null)) }
        viewModelScope.launch {
            val result =
                withContext(generationDispatcher) {
                    LayoutGenerator.generate(
                        GenerationRequest(
                            snapshot = snapshot,
                            designs = selected.map { WeightedDesign(it.id, it.weight) },
                            mode = current.generator.mode.toLayoutMode(current.generator.bandWidth),
                            seed = seed,
                            avoidOrthogonalNeighbors = current.generator.avoidOrthogonalNeighbors,
                            overwriteCompleted = current.generator.overwriteCompleted,
                        ),
                    )
                }
            val latest = _state.value
            if (latest.generator != current.generator || latest.designs != current.designs) {
                _state.update { it.copy(isGenerating = false) }
                return@launch
            }
            _state.update {
                it.copy(
                    isGenerating = false,
                    generator =
                        it.generator.copy(
                            issue = PlannerGeneratorIssue.NO_CHANGES.takeIf { result.changedCellCount == 0 },
                        ),
                    layoutGenerationResult =
                        PlannerLayoutGenerationResult(
                            snapshot = result.snapshot,
                            cells = result.snapshot.toUiCells(current.designs),
                            designCounts = result.designCounts,
                            orthogonalConflictCount = result.orthogonalConflictCount,
                            changedCellCount = result.changedCellCount,
                        ),
                )
            }
        }
    }

    fun regenerateLayout() {
        val seed =
            _state.value.generator.seed
                .toLongOrNull()
        if (seed == null) {
            generateLayout()
            return
        }
        setGeneratorSeed((seed + 1L).toString())
        generateLayout()
    }

    fun applyGeneratedLayout() {
        val current = _state.value
        val result = current.layoutGenerationResult ?: return
        if (result.changedCellCount == 0) return
        history?.record("Generate", result.snapshot)
        publishSnapshot(result.snapshot, clearGenerationResult = false)
        requestSave(result.snapshot)
        val seed = current.generator.seed.toLongOrNull()
        val currentProject = project
        if (seed != null && currentProject != null && currentProject.generationSeed != seed) {
            viewModelScope.launch {
                runCatching { repository.updateProject(currentProject.copy(generationSeed = seed)) }
            }
        }
        _state.update {
            it.copy(
                generator = it.generator.copy(isOpen = false, issue = null),
                layoutGenerationResult = it.layoutGenerationResult?.copy(applied = true),
            )
        }
    }

    @Suppress("kotlin:S3776") // One combined snapshot prevents partially updated planner state.
    private fun observePlanner() {
        viewModelScope.launch {
            combine(
                repository.observeProject(projectId),
                repository.observeGrid(projectId),
                repository.observeDesigns(),
                repository.observeColors(),
            ) { observedProject, grid, _, colors ->
                PlannerSource(observedProject, grid, colors)
            }.collect { source ->
                val observedProject = source.project
                val snapshot = source.snapshot
                if (observedProject == null || snapshot == null) {
                    project = null
                    history = null
                    _state.update { it.copy(isLoading = false, projectMissing = true) }
                    return@collect
                }
                project = observedProject
                val designRelations = repository.getDesignsWithRounds()
                val options =
                    mergeDesignOptions(
                        previous = _state.value.designs,
                        relations = designRelations,
                        colors = source.colors,
                    )

                val expected = expectedDatabaseSnapshot
                if (expected != null && snapshot != expected) return@collect
                if (snapshot == expected) expectedDatabaseSnapshot = null
                if (history == null || history?.current != snapshot) history = PlannerHistory(snapshot)

                val restoredSeed = savedStateHandle.get<String>(GENERATOR_SEED_KEY)
                val generatorSeed =
                    _state.value.generator.seed.ifBlank {
                        restoredSeed ?: observedProject.generationSeed.toString()
                    }
                val requestedDesign = requestedDesignId?.takeIf { id -> options.any { it.id == id } }
                if (requestedDesign != null) requestedDesignId = null
                _state.update { current ->
                    val selectedDesign =
                        requestedDesign
                            ?: current.selectedDesignId
                                ?.takeIf { id -> options.any { it.id == id } }
                            ?: observedProject.defaultSquareDesignId?.takeIf { id -> options.any { it.id == id } }
                            ?: options.firstOrNull()?.id
                    current.copy(
                        isLoading = false,
                        projectMissing = false,
                        projectName = observedProject.name,
                        rows = observedProject.rowCount,
                        columns = observedProject.columnCount,
                        trackingEnabled = observedProject.trackingEnabled,
                        snapshot = snapshot,
                        cells = snapshot.toUiCells(options),
                        designs = options,
                        selectedCoordinate =
                            current.selectedCoordinate
                                ?.takeIf(snapshot.size::contains),
                        selectedDesignId = selectedDesign,
                        canUndo = history?.canUndo == true,
                        canRedo = history?.canRedo == true,
                        tool =
                            current.tool.takeUnless {
                                it == PlannerTool.PROGRESS && !observedProject.trackingEnabled
                            } ?: PlannerTool.SELECT,
                        generator = current.generator.copy(seed = generatorSeed),
                        layoutGenerationResult =
                            if (
                                current.snapshot != null && current.snapshot != snapshot
                            ) {
                                null
                            } else {
                                current.layoutGenerationResult?.copy(
                                    cells = current.layoutGenerationResult.snapshot.toUiCells(options),
                                )
                            },
                    )
                }
            }
        }
    }

    private fun persistSnapshots() {
        viewModelScope.launch {
            for (snapshot in saveRequests) {
                _state.update { it.copy(isSaving = true, saveFailed = false) }
                runCatching {
                    repository.replaceProjectCells(projectId, snapshot.toEntities(projectId))
                }.onSuccess {
                    _state.update { it.copy(isSaving = false, saveFailed = false) }
                }.onFailure {
                    expectedDatabaseSnapshot = null
                    _state.update { it.copy(isSaving = false, saveFailed = true) }
                }
            }
        }
    }

    private fun updateSelected(
        label: String,
        transform: (CellState) -> CellState,
    ) {
        val current = _state.value
        val coordinate = current.selectedCoordinate ?: return
        val snapshot = current.snapshot ?: return
        val next = snapshot.updated(transform(snapshot[coordinate]))
        recordAndSave(label, next)
    }

    private fun recordAndSave(
        label: String,
        next: GridSnapshot,
    ) {
        val plannerHistory = history ?: return
        val recorded = plannerHistory.record(label, next)
        if (recorded == _state.value.snapshot) return
        publishSnapshot(recorded)
        requestSave(recorded)
    }

    private fun requestSave(snapshot: GridSnapshot) {
        expectedDatabaseSnapshot = snapshot
        saveRequests.trySend(snapshot)
    }

    private fun publishSnapshot(
        snapshot: GridSnapshot,
        updateHistoryControls: Boolean = true,
        clearGenerationResult: Boolean = true,
    ) {
        _state.update { current ->
            current.copy(
                snapshot = snapshot,
                cells = snapshot.toUiCells(current.designs),
                canUndo = if (updateHistoryControls) history?.canUndo == true else current.canUndo,
                canRedo = if (updateHistoryControls) history?.canRedo == true else current.canRedo,
                layoutGenerationResult = if (clearGenerationResult) null else current.layoutGenerationResult,
            )
        }
    }

    private fun setGeneratorIssue(issue: PlannerGeneratorIssue) {
        _state.update { it.copy(generator = it.generator.copy(issue = issue)) }
    }

    private fun mergeDesignOptions(
        previous: List<PlannerDesignOption>,
        relations: List<SquareDesignWithRounds>,
        colors: List<ColorEntity>,
    ): List<PlannerDesignOption> {
        val colorById = colors.associateBy(ColorEntity::id)
        val newById =
            relations.associate { relation ->
                val rounds = relation.rounds.sortedBy { it.roundIndex }
                val template = MotifTemplateRegistry.find(relation.design.motifTemplateId)
                val visual =
                    if (template != null && template.supportsRoundCount(rounds.size)) {
                        SquareDesignVisual(
                            templateId = template.id,
                            roundColors =
                                rounds.map { round ->
                                    colorById[round.colorId]?.argb?.toInt() ?: FALLBACK_ROUND_COLOR
                                },
                        )
                    } else {
                        null
                    }
                relation.design.id to
                    PlannerDesignOption(
                        id = relation.design.id,
                        name = relation.design.name,
                        visual = visual,
                    )
            }
        val previousById = previous.associateBy(PlannerDesignOption::id)
        val orderedIds =
            previous.map(PlannerDesignOption::id).filter(newById::containsKey) +
                newById.keys.filterNot(previousById::containsKey)
        return orderedIds.map { id ->
            val fresh = newById.getValue(id)
            val old = previousById[id]
            if (old == null) {
                fresh
            } else {
                fresh.copy(
                    includedInGeneration = old.includedInGeneration,
                    weight = old.weight,
                )
            }
        }
    }

    private data class PlannerSource(
        val project: ProjectEntity?,
        val snapshot: GridSnapshot?,
        val colors: List<ColorEntity>,
    )

    companion object {
        const val PROJECT_ID_KEY = "projectId"
        private const val ACCESSIBLE_GRID_KEY = "accessibleGridMode"
        private const val GENERATOR_SEED_KEY = "generatorSeed"
        private const val FALLBACK_ROUND_COLOR = 0xFF9BA77A.toInt()

        fun factory(
            repository: SquareToolRepository,
            projectId: String,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T {
                    val savedStateHandle = extras.createSavedStateHandle()
                    if (!savedStateHandle.contains(PROJECT_ID_KEY)) {
                        savedStateHandle[PROJECT_ID_KEY] = projectId
                    }
                    return PlannerViewModel(
                        repository = repository,
                        savedStateHandle = savedStateHandle,
                    ) as T
                }
            }
    }
}

private fun GridSnapshot.toUiCells(designs: List<PlannerDesignOption>): List<PlannerUiCell> {
    val designById = designs.associateBy(PlannerDesignOption::id)
    return cells.map { cell ->
        val design = cell.designId?.let(designById::get)
        PlannerUiCell(
            coordinate = cell.coordinate,
            designId = cell.designId,
            designName = design?.name,
            visual = design?.visual,
            locked = cell.locked,
            completed = cell.completed,
        )
    }
}

private fun GridSnapshot.toEntities(projectId: String): List<ProjectCellEntity> =
    cells.map { cell ->
        ProjectCellEntity(
            projectId = projectId,
            rowIndex = cell.coordinate.row,
            columnIndex = cell.coordinate.column,
            squareDesignId = cell.designId,
            locked = cell.locked,
            completed = cell.completed,
            gramsPerSquareOverride = cell.gramsPerSquareOverride,
        )
    }

private fun PlannerGeneratorMode.toLayoutMode(bandWidth: Int): LayoutMode =
    when (this) {
        PlannerGeneratorMode.RANDOM -> LayoutMode.Random
        PlannerGeneratorMode.BALANCED_RANDOM -> LayoutMode.BalancedRandom
        PlannerGeneratorMode.CHECKER -> LayoutMode.Checker
        PlannerGeneratorMode.ALTERNATING_ROWS -> LayoutMode.AlternatingRows
        PlannerGeneratorMode.ALTERNATING_COLUMNS -> LayoutMode.AlternatingColumns
        PlannerGeneratorMode.DIAGONAL -> LayoutMode.Diagonal
        PlannerGeneratorMode.HORIZONTAL_STRIPES -> LayoutMode.HorizontalStripes(bandWidth)
        PlannerGeneratorMode.VERTICAL_STRIPES -> LayoutMode.VerticalStripes(bandWidth)
        PlannerGeneratorMode.MIRROR_LEFT_TO_RIGHT -> LayoutMode.Mirror(MirrorDirection.LEFT_TO_RIGHT)
        PlannerGeneratorMode.MIRROR_RIGHT_TO_LEFT -> LayoutMode.Mirror(MirrorDirection.RIGHT_TO_LEFT)
        PlannerGeneratorMode.MIRROR_TOP_TO_BOTTOM -> LayoutMode.Mirror(MirrorDirection.TOP_TO_BOTTOM)
        PlannerGeneratorMode.MIRROR_BOTTOM_TO_TOP -> LayoutMode.Mirror(MirrorDirection.BOTTOM_TO_TOP)
        PlannerGeneratorMode.GRADIENT_LEFT_TO_RIGHT -> LayoutMode.Gradient(GradientDirection.LEFT_TO_RIGHT)
        PlannerGeneratorMode.GRADIENT_RIGHT_TO_LEFT -> LayoutMode.Gradient(GradientDirection.RIGHT_TO_LEFT)
        PlannerGeneratorMode.GRADIENT_TOP_TO_BOTTOM -> LayoutMode.Gradient(GradientDirection.TOP_TO_BOTTOM)
        PlannerGeneratorMode.GRADIENT_BOTTOM_TO_TOP -> LayoutMode.Gradient(GradientDirection.BOTTOM_TO_TOP)
        PlannerGeneratorMode.GRADIENT_DIAGONAL -> LayoutMode.Gradient(GradientDirection.DIAGONAL)
        PlannerGeneratorMode.RADIAL -> LayoutMode.Radial
    }

private val PlannerTool.historyLabel: String
    get() =
        when (this) {
            PlannerTool.SELECT -> "Select"
            PlannerTool.PAINT -> "Paint"
            PlannerTool.LOCK -> "Lock"
            PlannerTool.PROGRESS -> "Progress"
        }
