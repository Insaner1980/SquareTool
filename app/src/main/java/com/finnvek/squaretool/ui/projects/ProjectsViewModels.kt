package com.finnvek.squaretool.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.ProjectCellEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.local.SquareDesignWithRounds
import com.finnvek.squaretool.data.repository.AppSettings
import com.finnvek.squaretool.data.repository.SquareToolRepository
import com.finnvek.squaretool.domain.model.MeasurementUnit
import com.finnvek.squaretool.ui.simpleViewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

sealed interface ProjectConfirmation {
    val project: ProjectEntity

    data class Delete(
        override val project: ProjectEntity,
    ) : ProjectConfirmation

    data class Duplicate(
        override val project: ProjectEntity,
    ) : ProjectConfirmation

    data class Rename(
        override val project: ProjectEntity,
    ) : ProjectConfirmation
}

enum class ProjectsNotice { SAVE_FAILED }

data class ProjectsUiState(
    val query: String = "",
    val sort: ProjectSort = ProjectSort.RECENT,
    val favoriteOnly: Boolean = false,
    val projects: List<ProjectCardModel> = emptyList(),
    val confirmation: ProjectConfirmation? = null,
    val notice: ProjectsNotice? = null,
    val isLoading: Boolean = true,
)

class ProjectsViewModel(
    private val repository: SquareToolRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(ProjectSort.RECENT)
    private val favoriteOnly = MutableStateFlow(false)
    private val confirmation = MutableStateFlow<ProjectConfirmation?>(null)
    private val notice = MutableStateFlow<ProjectsNotice?>(null)

    private val cards: Flow<List<ProjectCardModel>> =
        repository
            .observeProjectCardData()
            .map(::buildProjectCardModels)

    private val filters =
        combine(query, sort, favoriteOnly, confirmation, notice) {
            currentQuery,
            currentSort,
            onlyFavorites,
            currentConfirmation,
            currentNotice,
            ->
            ProjectsFilters(
                query = currentQuery,
                sort = currentSort,
                favoriteOnly = onlyFavorites,
                confirmation = currentConfirmation,
                notice = currentNotice,
            )
        }

    val uiState =
        combine(cards, filters) { loaded, currentFilters ->
            ProjectsUiState(
                query = currentFilters.query,
                sort = currentFilters.sort,
                favoriteOnly = currentFilters.favoriteOnly,
                projects =
                    filterAndSortProjectCards(
                        loaded,
                        currentFilters.query,
                        currentFilters.favoriteOnly,
                        currentFilters.sort,
                    ),
                confirmation = currentFilters.confirmation,
                notice = currentFilters.notice,
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectsUiState())

    fun updateQuery(value: String) {
        query.value = value
    }

    fun updateSort(value: ProjectSort) {
        sort.value = value
    }

    fun updateFavoriteOnly(value: Boolean) {
        favoriteOnly.value = value
    }

    fun requestDelete(project: ProjectEntity) {
        confirmation.value = ProjectConfirmation.Delete(project)
    }

    fun requestDuplicate(project: ProjectEntity) {
        confirmation.value = ProjectConfirmation.Duplicate(project)
    }

    fun requestRename(project: ProjectEntity) {
        confirmation.value = ProjectConfirmation.Rename(project)
    }

    fun cancelConfirmation() {
        confirmation.value = null
    }

    fun clearNotice() {
        notice.value = null
    }

    fun openProject(
        projectId: String,
        onOpen: (String) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching { repository.markProjectOpened(projectId) }
                .onSuccess { onOpen(projectId) }
                .onFailure { notice.value = ProjectsNotice.SAVE_FAILED }
        }
    }

    fun toggleFavorite(projectId: String) {
        viewModelScope.launch {
            runCatching {
                val project = repository.getProject(projectId) ?: return@runCatching
                repository.updateProject(
                    project.copy(favorite = !project.favorite, updatedAt = System.currentTimeMillis()),
                )
            }.onFailure { notice.value = ProjectsNotice.SAVE_FAILED }
        }
    }

    fun confirmDelete() {
        val project = (confirmation.value as? ProjectConfirmation.Delete)?.project ?: return
        confirmation.value = null
        viewModelScope.launch {
            runCatching { repository.deleteProject(project.id) }
                .onFailure { notice.value = ProjectsNotice.SAVE_FAILED }
        }
    }

    fun confirmDuplicate(onDuplicated: (String) -> Unit = {}) {
        val project = (confirmation.value as? ProjectConfirmation.Duplicate)?.project ?: return
        confirmation.value = null
        viewModelScope.launch {
            runCatching {
                repository.duplicateProject(
                    sourceProjectId = project.id,
                    newProjectId = UUID.randomUUID().toString(),
                    newName = project.name,
                )
            }.onSuccess { onDuplicated(it.id) }
                .onFailure { notice.value = ProjectsNotice.SAVE_FAILED }
        }
    }

    fun confirmRename(newName: String) {
        val project = (confirmation.value as? ProjectConfirmation.Rename)?.project ?: return
        if (newName.isBlank()) return
        confirmation.value = null
        viewModelScope.launch {
            runCatching {
                repository.updateProject(
                    project.copy(name = newName.trim(), updatedAt = System.currentTimeMillis()),
                )
            }.onFailure { notice.value = ProjectsNotice.SAVE_FAILED }
        }
    }

    companion object {
        fun factory(repository: SquareToolRepository): ViewModelProvider.Factory =
            simpleViewModelFactory {
                ProjectsViewModel(repository)
            }
    }
}

private data class ProjectsFilters(
    val query: String,
    val sort: ProjectSort,
    val favoriteOnly: Boolean,
    val confirmation: ProjectConfirmation?,
    val notice: ProjectsNotice?,
)

enum class ProjectEditorNotice { INVALID_DRAFT, SAVE_FAILED }

data class ProjectEditorUiState(
    val draft: ProjectEditorDraft =
        initialProjectEditorDraft(
            project = null,
            selectedColorIds = emptySet(),
            settings = AppSettings(),
            newProjectId = "",
        ),
    val colors: List<ColorEntity> = emptyList(),
    val designs: List<SquareDesignWithRounds> = emptyList(),
    val basePreview: ProjectCardModel? = null,
    val validationErrors: Set<ProjectDraftError> = emptySet(),
    val pendingShrink: ShrinkImpact? = null,
    val notice: ProjectEditorNotice? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
)

class ProjectEditorViewModel(
    private val repository: SquareToolRepository,
    private val projectId: String?,
    private val settings: AppSettings,
) : ViewModel() {
    private val state = MutableStateFlow(ProjectEditorUiState())
    private val originalProject = AtomicReference<ProjectEntity?>(null)
    private val originalCells = AtomicReference<List<ProjectCellEntity>>(emptyList())

    val uiState = state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    init {
        viewModelScope.launch {
            val loaded =
                runCatching {
                    val colors = repository.getColors()
                    val designs = repository.getDesignsWithRounds()
                    val project = projectId?.let { repository.getProject(it) }
                    val cells = project?.let { repository.getProjectCells(it.id) }.orEmpty()
                    val selectedColors =
                        project?.let { repository.getProjectPalette(it.id).map(ColorEntity::id).toSet() }
                            ?: colors.take(7).map(ColorEntity::id).toSet()
                    val palette = project?.let { repository.getProjectPalette(it.id) }.orEmpty()
                    originalProject.set(project)
                    originalCells.set(cells)
                    val draft =
                        initialProjectEditorDraft(
                            project = project,
                            selectedColorIds = selectedColors,
                            settings = settings,
                            newProjectId = UUID.randomUUID().toString(),
                        )
                    ProjectEditorUiState(
                        draft = draft,
                        colors = colors,
                        designs = designs,
                        basePreview =
                            project?.let {
                                buildProjectCardModel(it, cells, designs, colors, palette)
                            },
                        isLoading = false,
                    )
                }
            state.value =
                loaded.getOrElse {
                    ProjectEditorUiState(notice = ProjectEditorNotice.SAVE_FAILED, isLoading = false)
                }
        }
    }

    fun updateName(value: String) = updateDraft { copy(name = value) }

    fun updateRows(value: String) = updateDraft { copy(rows = value.toIntOrNull() ?: 0) }

    fun updateColumns(value: String) = updateDraft { copy(columns = value.toIntOrNull() ?: 0) }

    fun updateUnit(value: MeasurementUnit) = updateDraft { copy(measurementUnit = value) }

    fun updateSquareWidth(value: String) = updateDraft { copy(squareWidth = value) }

    fun updateSquareHeight(value: String) = updateDraft { copy(squareHeight = value) }

    fun updateJoiningGap(value: String) = updateDraft { copy(joiningGap = value) }

    fun updateTracking(value: Boolean) = updateDraft { copy(trackingEnabled = value) }

    fun updateInitialFill(value: InitialProjectFill) = updateDraft { copy(initialFill = value) }

    fun updateGlobalGrams(value: String) = updateDraft { copy(globalGramsPerSquare = value) }

    fun updateSkeinWeight(value: String) = updateDraft { copy(skeinWeightGrams = value) }

    fun updateBuffer(value: String) = updateDraft { copy(bufferPercent = value) }

    fun updateNotes(value: String) = updateDraft { copy(notes = value) }

    fun toggleColor(id: String) =
        updateDraft {
            copy(selectedColorIds = selectedColorIds.toggle(id))
        }

    fun toggleDesign(id: String) =
        updateDraft {
            copy(
                selectedDesignIds =
                    if (initialFill == InitialProjectFill.FILL_ONE) {
                        if (selectedDesignIds == setOf(id)) emptySet() else setOf(id)
                    } else {
                        selectedDesignIds.toggle(id)
                    },
            )
        }

    fun clearNotice() {
        state.update { it.copy(notice = null) }
    }

    fun cancelShrink() {
        state.update { it.copy(pendingShrink = null) }
    }

    fun save(onSaved: (String) -> Unit) {
        saveInternal(onSaved, shrinkConfirmed = false)
    }

    fun confirmShrink(onSaved: (String) -> Unit) {
        state.update { it.copy(pendingShrink = null) }
        saveInternal(onSaved, shrinkConfirmed = true)
    }

    private fun saveInternal(
        onSaved: (String) -> Unit,
        shrinkConfirmed: Boolean,
    ) {
        val draft = state.value.draft
        val errors = draft.validationErrors()
        if (errors.isNotEmpty()) {
            state.update { it.copy(validationErrors = errors, notice = ProjectEditorNotice.INVALID_DRAFT) }
            return
        }
        val original = originalProject.get()
        if (original != null && !shrinkConfirmed &&
            (draft.rows < original.rowCount || draft.columns < original.columnCount)
        ) {
            val impact = calculateShrinkImpact(originalCells.get(), draft.rows, draft.columns)
            if (impact.lostCellCount > 0) {
                state.update { it.copy(pendingShrink = impact) }
                return
            }
        }
        state.update { it.copy(isSaving = true, validationErrors = emptySet()) }
        viewModelScope.launch {
            runCatching { persist(draft) }
                .onSuccess { onSaved(it.id) }
                .onFailure {
                    state.update { current ->
                        current.copy(isSaving = false, notice = ProjectEditorNotice.SAVE_FAILED)
                    }
                }
        }
    }

    private suspend fun persist(draft: ProjectEditorDraft): ProjectEntity {
        val now = System.currentTimeMillis()
        val original = originalProject.get()
        val project =
            ProjectEntity(
                id = draft.id,
                name = draft.name.trim(),
                rowCount = draft.rows,
                columnCount = draft.columns,
                squareWidthValue = draft.squareWidth.toDoubleOrNull(),
                squareHeightValue = draft.squareHeight.toDoubleOrNull(),
                measurementUnit = draft.measurementUnit.name.lowercase(),
                joiningGapValue = draft.joiningGap.toDoubleOrNull(),
                trackingEnabled = draft.trackingEnabled,
                favorite = original?.favorite ?: false,
                notes = draft.notes.trim(),
                createdAt = original?.createdAt ?: now,
                updatedAt = now,
                lastOpenedAt = original?.lastOpenedAt ?: now,
                generationSeed = original?.generationSeed ?: now,
                defaultSquareDesignId = draft.selectedDesignIds.firstOrNull(),
                globalGramsPerSquare = draft.globalGramsPerSquare.toDoubleOrNull(),
                skeinWeightGrams = draft.skeinWeightGrams.toDoubleOrNull(),
                joiningAndEdgingBufferPercent = draft.bufferPercent.toDouble(),
                demoProject = original?.demoProject ?: false,
            )
        return repository.saveProjectWithLayoutAndPalette(
            project = project,
            orderedColorIds = draft.selectedColorIds.toList(),
            initialAssignments = if (original == null) draft.initialAssignments() else null,
        )
    }

    private fun updateDraft(change: ProjectEditorDraft.() -> ProjectEditorDraft) {
        state.update { it.copy(draft = it.draft.change(), validationErrors = emptySet(), notice = null) }
    }

    companion object {
        fun factory(
            repository: SquareToolRepository,
            projectId: String?,
            settings: AppSettings,
        ): ViewModelProvider.Factory =
            simpleViewModelFactory {
                ProjectEditorViewModel(repository, projectId, settings)
            }
    }
}

private fun Set<String>.toggle(value: String): Set<String> = if (value in this) this - value else this + value
