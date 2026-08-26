package com.finnvek.squaretool.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.PaletteColorCrossRef
import com.finnvek.squaretool.data.local.PaletteEntity
import com.finnvek.squaretool.data.repository.ColorUsage
import com.finnvek.squaretool.data.repository.SquareToolRepository
import com.finnvek.squaretool.ui.moveListItem
import com.finnvek.squaretool.ui.simpleViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

data class PaletteListItem(
    val palette: PaletteEntity,
    val colors: List<ColorEntity>,
)

sealed interface LibraryNotice {
    data object BuiltInCannotDelete : LibraryNotice

    data class ColorInUse(
        val usage: ColorUsage,
    ) : LibraryNotice

    data object SaveFailed : LibraryNotice

    data object ProjectPaletteSaved : LibraryNotice

    data object PaletteApplied : LibraryNotice
}

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.COLORS,
    val query: String = "",
    val colors: List<ColorEntity> = emptyList(),
    val palettes: List<PaletteListItem> = emptyList(),
    val notice: LibraryNotice? = null,
    val isLoading: Boolean = true,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: SquareToolRepository,
) : ViewModel() {
    private val selectedTab = MutableStateFlow(LibraryTab.COLORS)
    private val query = MutableStateFlow("")
    private val notice = MutableStateFlow<LibraryNotice?>(null)

    private val results =
        query.debounce(250).flatMapLatest(repository::searchLibrary).mapLatest { result ->
            val palettes =
                result.palettes.map { palette ->
                    PaletteListItem(palette, repository.getPaletteColors(palette.id))
                }
            result.colors to palettes
        }

    private val presentation = combine(selectedTab, notice) { tab, currentNotice -> tab to currentNotice }

    val uiState =
        combine(results, query, presentation) { result, currentQuery, presentation ->
            LibraryUiState(
                selectedTab = presentation.first,
                query = currentQuery,
                colors = result.first,
                palettes = result.second,
                notice = presentation.second,
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onTabSelected(tab: LibraryTab) {
        selectedTab.value = tab
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun deleteColor(id: String) {
        viewModelScope.launch {
            val color = repository.getColor(id) ?: return@launch
            if (color.builtIn) {
                notice.value = LibraryNotice.BuiltInCannotDelete
                return@launch
            }
            val usage = repository.getColorUsage(id)
            if (usage.totalReferenceCount > 0) {
                notice.value = LibraryNotice.ColorInUse(usage)
            } else if (!repository.deleteColorIfUnused(id)) {
                notice.value = LibraryNotice.ColorInUse(repository.getColorUsage(id))
            }
        }
    }

    fun deletePalette(id: String) {
        viewModelScope.launch {
            val palette = repository.getPalette(id) ?: return@launch
            if (palette.builtIn) notice.value = LibraryNotice.BuiltInCannotDelete else repository.deletePalette(id)
        }
    }

    fun applyPalette(
        projectId: String,
        paletteId: String,
    ) {
        viewModelScope.launch {
            runCatching { repository.applyPaletteToProject(projectId, paletteId) }
                .onSuccess { notice.value = LibraryNotice.PaletteApplied }
                .onFailure { notice.value = LibraryNotice.SaveFailed }
        }
    }

    fun saveProjectPalette(
        projectId: String,
        name: String,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val colors = repository.getProjectPalette(projectId)
                require(colors.isNotEmpty())
                val now = System.currentTimeMillis()
                val palette = PaletteEntity(UUID.randomUUID().toString(), name.trim(), false, now, now)
                repository.savePalette(
                    palette,
                    colors.mapIndexed { index, color -> PaletteColorCrossRef(palette.id, color.id, index) },
                )
            }.onSuccess { notice.value = LibraryNotice.ProjectPaletteSaved }
                .onFailure { notice.value = LibraryNotice.SaveFailed }
        }
    }

    fun clearNotice() {
        notice.value = null
    }

    companion object {
        fun factory(repository: SquareToolRepository): ViewModelProvider.Factory = simpleViewModelFactory { LibraryViewModel(repository) }
    }
}

sealed interface ColorEditorNotice {
    data object InvalidDraft : ColorEditorNotice

    data object SaveFailed : ColorEditorNotice
}

data class ColorEditorUiState(
    val draft: ColorEditorDraft = ColorEditorDraft(),
    val validationErrors: Set<ColorDraftError> = emptySet(),
    val notice: ColorEditorNotice? = null,
    val isLoading: Boolean = true,
)

class ColorEditorViewModel(
    private val repository: SquareToolRepository,
    private val colorId: String?,
    private val duplicate: Boolean,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ColorEditorUiState())
    val uiState = mutableState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), mutableState.value)
    private val originalCreatedAt = AtomicReference<Long?>(null)

    init {
        viewModelScope.launch {
            val source = colorId?.let { repository.getColor(it) }
            val shouldDuplicate = duplicate || source?.builtIn == true
            if (source != null && !shouldDuplicate) originalCreatedAt.set(source.createdAt)
            mutableState.value =
                ColorEditorUiState(
                    draft =
                        source?.let {
                            ColorEditorDraft(
                                id = if (shouldDuplicate) UUID.randomUUID().toString() else it.id,
                                name = it.name,
                                hex = formatHexColor(it.argb),
                                yarnBrand = it.yarnBrand.orEmpty(),
                                yarnLine = it.yarnLine.orEmpty(),
                                shadeName = it.shadeName.orEmpty(),
                                shadeCode = it.shadeCode.orEmpty(),
                                skeinWeightGrams = it.skeinWeightGrams?.toString().orEmpty(),
                                yarnLength = it.yarnLength?.toString().orEmpty(),
                                yarnLengthUnit = it.yarnLengthUnit ?: "m",
                                notes = it.notes,
                            )
                        } ?: ColorEditorDraft(id = UUID.randomUUID().toString()),
                    isLoading = false,
                )
        }
    }

    fun updateDraft(value: ColorEditorDraft) {
        mutableState.update { it.copy(draft = value, validationErrors = emptySet()) }
    }

    fun updateHsl(
        hue: Float? = null,
        saturation: Float? = null,
        lightness: Float? = null,
    ) {
        val current = parseHexColor(mutableState.value.draft.hex) ?: return
        val hsl = argbToHsl(current)
        val updated =
            hsl.copy(
                hue = hue ?: hsl.hue,
                saturation = saturation ?: hsl.saturation,
                lightness = lightness ?: hsl.lightness,
            )
        updateDraft(mutableState.value.draft.copy(hex = formatHexColor(hslToArgb(updated))))
    }

    fun save(onSaved: () -> Unit) {
        val draft = mutableState.value.draft
        val errors = draft.validationErrors()
        if (errors.isNotEmpty()) {
            mutableState.update { it.copy(validationErrors = errors, notice = ColorEditorNotice.InvalidDraft) }
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity =
                ColorEntity(
                    id = draft.id,
                    name = draft.name.trim(),
                    argb = requireNotNull(parseHexColor(draft.hex)),
                    yarnBrand = draft.yarnBrand.trim().ifBlank { null },
                    yarnLine = draft.yarnLine.trim().ifBlank { null },
                    shadeName = draft.shadeName.trim().ifBlank { null },
                    shadeCode = draft.shadeCode.trim().ifBlank { null },
                    skeinWeightGrams = draft.skeinWeightGrams.toDoubleOrNull(),
                    yarnLength = draft.yarnLength.toDoubleOrNull(),
                    yarnLengthUnit = draft.yarnLengthUnit,
                    notes = draft.notes.trim(),
                    builtIn = false,
                    createdAt = originalCreatedAt.get() ?: now,
                    updatedAt = now,
                )
            runCatching { repository.saveColor(entity) }
                .onSuccess { onSaved() }
                .onFailure { mutableState.update { it.copy(notice = ColorEditorNotice.SaveFailed) } }
        }
    }

    companion object {
        fun factory(
            repository: SquareToolRepository,
            colorId: String?,
            duplicate: Boolean,
        ): ViewModelProvider.Factory = simpleViewModelFactory { ColorEditorViewModel(repository, colorId, duplicate) }
    }
}

sealed interface PaletteEditorNotice {
    data object InvalidDraft : PaletteEditorNotice

    data object SaveFailed : PaletteEditorNotice

    data object PaletteApplied : PaletteEditorNotice
}

data class PaletteEditorUiState(
    val draft: PaletteEditorDraft = PaletteEditorDraft(),
    val availableColors: List<ColorEntity> = emptyList(),
    val notice: PaletteEditorNotice? = null,
    val isLoading: Boolean = true,
)

class PaletteEditorViewModel(
    private val repository: SquareToolRepository,
    private val paletteId: String?,
    private val duplicate: Boolean,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PaletteEditorUiState())
    val uiState =
        combine(mutableState, repository.observeColors()) { state, colors -> state.copy(availableColors = colors) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), mutableState.value)
    private val originalCreatedAt = AtomicReference<Long?>(null)

    init {
        viewModelScope.launch {
            val source = paletteId?.let { repository.getPalette(it) }
            val colors = source?.let { repository.getPaletteColors(it.id) }.orEmpty()
            val shouldDuplicate = duplicate || source?.builtIn == true
            if (source != null && !shouldDuplicate) originalCreatedAt.set(source.createdAt)
            mutableState.value =
                PaletteEditorUiState(
                    draft =
                        PaletteEditorDraft(
                            id = if (source == null || shouldDuplicate) UUID.randomUUID().toString() else source.id,
                            name =
                                when {
                                    source == null -> ""
                                    shouldDuplicate -> source.name
                                    else -> source.name
                                },
                            colorIds = colors.map(ColorEntity::id),
                        ),
                    availableColors = repository.getColors(),
                    isLoading = false,
                )
        }
    }

    fun updateName(value: String) = updateDraft { copy(name = value) }

    fun toggleColor(id: String) =
        updateDraft {
            copy(colorIds = if (id in colorIds) colorIds - id else colorIds + id)
        }

    fun removeColor(id: String) = updateDraft { copy(colorIds = colorIds - id) }

    fun moveColor(
        fromIndex: Int,
        toIndex: Int,
    ) = updateDraft {
        copy(colorIds = moveListItem(colorIds, fromIndex, toIndex))
    }

    fun save(onSaved: () -> Unit) {
        val draft = mutableState.value.draft
        if (!draft.isValid) {
            mutableState.update { it.copy(notice = PaletteEditorNotice.InvalidDraft) }
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity = PaletteEntity(draft.id, draft.name.trim(), false, originalCreatedAt.get() ?: now, now)
            runCatching {
                repository.savePalette(
                    entity,
                    draft.colorIds.mapIndexed { index, colorId -> PaletteColorCrossRef(draft.id, colorId, index) },
                )
            }.onSuccess { onSaved() }
                .onFailure { mutableState.update { it.copy(notice = PaletteEditorNotice.SaveFailed) } }
        }
    }

    fun applyToProject(projectId: String) {
        val draft = mutableState.value.draft
        if (!draft.isValid) {
            mutableState.update { it.copy(notice = PaletteEditorNotice.InvalidDraft) }
            return
        }
        viewModelScope.launch {
            runCatching { repository.setProjectPalette(projectId, draft.colorIds) }
                .onSuccess { mutableState.update { it.copy(notice = PaletteEditorNotice.PaletteApplied) } }
                .onFailure { mutableState.update { it.copy(notice = PaletteEditorNotice.SaveFailed) } }
        }
    }

    private fun updateDraft(change: PaletteEditorDraft.() -> PaletteEditorDraft) {
        mutableState.update { it.copy(draft = it.draft.change()) }
    }

    companion object {
        fun factory(
            repository: SquareToolRepository,
            paletteId: String?,
            duplicate: Boolean,
        ): ViewModelProvider.Factory = simpleViewModelFactory { PaletteEditorViewModel(repository, paletteId, duplicate) }
    }
}
