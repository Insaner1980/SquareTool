package com.finnvek.squaretool.ui.squares

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.SquareDesignEntity
import com.finnvek.squaretool.data.local.SquareRoundEntity
import com.finnvek.squaretool.data.repository.DesignUsage
import com.finnvek.squaretool.data.repository.SquareToolRepository
import com.finnvek.squaretool.render.MotifTemplateRegistry
import com.finnvek.squaretool.ui.moveListItem
import com.finnvek.squaretool.ui.simpleViewModelFactory
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

sealed interface SquaresNotice {
    data object BuiltInCannotDelete : SquaresNotice

    data class DesignInUse(
        val usage: DesignUsage,
    ) : SquaresNotice

    data object SaveFailed : SquaresNotice
}

data class SquaresUiState(
    val query: String = "",
    val selectedFilter: SquareFilter = SquareFilter.ALL,
    val designs: List<SquareDesignListItem> = emptyList(),
    val selectedDesign: SquareDesignListItem? = null,
    val notice: SquaresNotice? = null,
    val isLoading: Boolean = true,
)

@OptIn(FlowPreview::class)
class SquaresViewModel(
    private val repository: SquareToolRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(SquareFilter.ALL)
    private val selectedId = MutableStateFlow<String?>(null)
    private val notice = MutableStateFlow<SquaresNotice?>(null)

    private val allDesigns =
        combine(
            repository.observeDesignsWithRounds(),
            repository.observeColors(),
        ) { designs, colors ->
            val colorsById = colors.associateBy(ColorEntity::id)
            designs.map { relation ->
                SquareDesignListItem(
                    design = relation.design,
                    roundColors =
                        relation.rounds
                            .sortedBy(SquareRoundEntity::roundIndex)
                            .mapNotNull { colorsById[it.colorId] },
                )
            }
        }

    private val criteria = combine(query.debounce(250), selectedFilter) { search, filter -> search to filter }
    private val selection = combine(selectedId, notice) { id, currentNotice -> id to currentNotice }

    val uiState =
        combine(allDesigns, criteria, query, selection) { designs, criteria, currentQuery, selection ->
            val filtered = filterSquareDesigns(designs, criteria.first, criteria.second)
            SquaresUiState(
                query = currentQuery,
                selectedFilter = criteria.second,
                designs = filtered,
                selectedDesign = designs.firstOrNull { it.id == selection.first },
                notice = selection.second,
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SquaresUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: SquareFilter) {
        selectedFilter.value = value
    }

    fun selectDesign(id: String?) {
        selectedId.value = id
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            runCatching {
                val relation = repository.getDesignWithRounds(id) ?: return@runCatching
                repository.saveDesign(
                    relation.design.copy(favorite = !relation.design.favorite, updatedAt = System.currentTimeMillis()),
                    relation.rounds,
                )
            }.onFailure { notice.value = SquaresNotice.SaveFailed }
        }
    }

    fun deleteDesign(id: String) {
        viewModelScope.launch {
            val design = repository.getDesign(id) ?: return@launch
            if (design.builtIn) {
                notice.value = SquaresNotice.BuiltInCannotDelete
                return@launch
            }
            val usage = repository.getDesignUsage(id)
            if (usage.totalReferenceCount > 0) {
                notice.value = SquaresNotice.DesignInUse(usage)
                return@launch
            }
            if (repository.deleteDesignIfUnused(id)) selectedId.compareAndSet(id, null)
        }
    }

    fun clearNotice() {
        notice.value = null
    }

    companion object {
        fun factory(repository: SquareToolRepository): ViewModelProvider.Factory = simpleViewModelFactory { SquaresViewModel(repository) }
    }
}

sealed interface SquareEditorNotice {
    data object InvalidDraft : SquareEditorNotice

    data object InvalidColor : SquareEditorNotice

    data object SaveFailed : SquareEditorNotice
}

data class SquareEditorUiState(
    val draft: SquareEditorDraft = SquareEditorDraft("", "", "classic_granny", emptyList(), "", false, false),
    val colors: List<ColorEntity> = emptyList(),
    val pendingTemplateChange: SquareTemplateChange? = null,
    val validationErrors: Set<SquareDraftError> = emptySet(),
    val notice: SquareEditorNotice? = null,
    val isLoading: Boolean = true,
)

class SquareEditorViewModel(
    private val repository: SquareToolRepository,
    private val designId: String?,
    private val duplicate: Boolean,
) : ViewModel() {
    private val draft = MutableStateFlow(SquareEditorUiState())
    private val originalCreatedAt = AtomicReference<Long?>(null)
    private val gramsPerSquareOverride = AtomicReference<Double?>(null)

    val uiState =
        combine(draft, repository.observeColors()) { state, colors ->
            state.copy(colors = colors)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SquareEditorUiState())

    init {
        viewModelScope.launch {
            val colors = repository.getColors()
            val source = designId?.let { repository.getDesignWithRounds(it) }
            val shouldDuplicate = duplicate || source?.design?.builtIn == true
            gramsPerSquareOverride.set(source?.design?.gramsPerSquareOverride)
            val initial =
                if (source == null) {
                    val firstColor = colors.firstOrNull()?.id.orEmpty()
                    SquareEditorDraft(
                        id = UUID.randomUUID().toString(),
                        name = "",
                        templateId = MotifTemplateRegistry.templates.first().id,
                        roundColorIds = List(MotifTemplateRegistry.templates.first().minRounds) { firstColor },
                        notes = "",
                        favorite = false,
                        sourceBuiltIn = false,
                    )
                } else {
                    originalCreatedAt.set(if (shouldDuplicate) null else source.design.createdAt)
                    SquareEditorDraft(
                        id = if (shouldDuplicate) UUID.randomUUID().toString() else source.design.id,
                        name = source.design.name,
                        templateId = source.design.motifTemplateId,
                        roundColorIds = source.rounds.sortedBy { it.roundIndex }.map { it.colorId },
                        notes = source.design.note,
                        favorite = !shouldDuplicate && source.design.favorite,
                        sourceBuiltIn = false,
                    )
                }
            draft.value = SquareEditorUiState(draft = initial, colors = colors, isLoading = false)
        }
    }

    fun updateName(value: String) = updateDraft { copy(name = value) }

    fun updateNotes(value: String) = updateDraft { copy(notes = value) }

    fun updateFavorite(value: Boolean) = updateDraft { copy(favorite = value) }

    fun selectTemplate(templateId: String) {
        val change = draft.value.draft.planTemplateChange(templateId)
        if (change.requiresRoundTruncationConfirmation) {
            draft.update { it.copy(pendingTemplateChange = change) }
        } else {
            draft.update { it.copy(draft = change.updatedDraft) }
        }
    }

    fun confirmTemplateChange() {
        draft.update { state ->
            val change = state.pendingTemplateChange ?: return@update state
            state.copy(draft = change.updatedDraft, pendingTemplateChange = null)
        }
    }

    fun cancelTemplateChange() {
        draft.update { it.copy(pendingTemplateChange = null) }
    }

    fun assignColor(
        roundIndex: Int,
        colorId: String,
    ) = updateDraft {
        copy(roundColorIds = roundColorIds.mapIndexed { index, existing -> if (index == roundIndex) colorId else existing })
    }

    fun addRound() =
        updateDraft {
            val template = MotifTemplateRegistry.require(templateId)
            if (roundColorIds.size >= template.maxRounds) {
                this
            } else {
                val colorId =
                    roundColorIds.lastOrNull() ?: draft.value.colors
                        .firstOrNull()
                        ?.id
                        .orEmpty()
                copy(roundColorIds = roundColorIds + colorId)
            }
        }

    fun removeRound(index: Int) =
        updateDraft {
            val template = MotifTemplateRegistry.require(templateId)
            if (roundColorIds.size <= template.minRounds || index !in roundColorIds.indices) {
                this
            } else {
                copy(roundColorIds = roundColorIds.toMutableList().apply { removeAt(index) })
            }
        }

    fun moveRound(
        fromIndex: Int,
        toIndex: Int,
    ) = updateDraft {
        copy(roundColorIds = moveListItem(roundColorIds, fromIndex, toIndex))
    }

    fun createAndAssignColor(
        roundIndex: Int,
        name: String,
        hex: String,
    ) {
        val argb =
            com.finnvek.squaretool.ui.library
                .parseHexColor(hex)
        if (name.isBlank() || argb == null) {
            draft.update { it.copy(notice = SquareEditorNotice.InvalidColor) }
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val color =
                ColorEntity(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    argb = argb,
                    builtIn = false,
                    createdAt = now,
                    updatedAt = now,
                )
            runCatching { repository.saveColor(color) }
                .onSuccess { assignColor(roundIndex, color.id) }
                .onFailure { draft.update { it.copy(notice = SquareEditorNotice.SaveFailed) } }
        }
    }

    fun save(onSaved: () -> Unit) {
        val current = draft.value.draft
        val errors = current.validationErrors()
        if (errors.isNotEmpty()) {
            draft.update { it.copy(validationErrors = errors, notice = SquareEditorNotice.InvalidDraft) }
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val template = MotifTemplateRegistry.require(current.templateId)
            val design =
                SquareDesignEntity(
                    id = current.id,
                    name = current.name.trim(),
                    motifTemplateId = current.templateId,
                    note = current.notes.trim(),
                    favorite = current.favorite,
                    builtIn = false,
                    category =
                        template.category.name
                            .lowercase()
                            .replaceFirstChar(Char::titlecase),
                    gramsPerSquareOverride = gramsPerSquareOverride.get(),
                    createdAt = originalCreatedAt.get() ?: now,
                    updatedAt = now,
                )
            val rounds =
                current.roundColorIds.mapIndexed { index, colorId ->
                    SquareRoundEntity(current.id, index, colorId)
                }
            runCatching { repository.saveDesign(design, rounds) }
                .onSuccess { onSaved() }
                .onFailure { draft.update { it.copy(notice = SquareEditorNotice.SaveFailed) } }
        }
    }

    fun clearNotice() {
        draft.update { it.copy(notice = null) }
    }

    private fun updateDraft(change: SquareEditorDraft.() -> SquareEditorDraft) {
        draft.update { it.copy(draft = it.draft.change(), validationErrors = emptySet()) }
    }

    companion object {
        fun factory(
            repository: SquareToolRepository,
            designId: String?,
            duplicate: Boolean,
        ): ViewModelProvider.Factory =
            simpleViewModelFactory {
                SquareEditorViewModel(repository, designId, duplicate)
            }
    }
}
