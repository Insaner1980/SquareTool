package com.finnvek.squaretool.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finnvek.squaretool.R
import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.repository.SquareToolRepository
import com.finnvek.squaretool.ui.SquareToolEditorActions
import com.finnvek.squaretool.ui.SquareToolLoadingIndicator
import com.finnvek.squaretool.ui.SquareToolMenuItem
import com.finnvek.squaretool.ui.SquareToolSearchClearButton
import com.finnvek.squaretool.ui.squares.ColorSwatch
import com.finnvek.squaretool.ui.theme.SquareToolSpacing

@Suppress("kotlin:S107") // Route callbacks keep editor and library destinations independently typed.
@Composable
fun LibraryRoute(
    repository: SquareToolRepository,
    modifier: Modifier = Modifier,
    libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(repository)),
    projectId: String? = null,
    onCreateColor: () -> Unit = {},
    onEditColor: (id: String, duplicate: Boolean) -> Unit = { _, _ -> },
    onCreatePalette: () -> Unit = {},
    onEditPalette: (id: String, duplicate: Boolean) -> Unit = { _, _ -> },
) {
    val state by libraryViewModel.uiState.collectAsStateWithLifecycle()
    LibraryScreen(
        state = state,
        projectId = projectId,
        onSelectTab = libraryViewModel::onTabSelected,
        onQueryChange = libraryViewModel::onQueryChange,
        onCreateColor = onCreateColor,
        onEditColor = onEditColor,
        onDeleteColor = libraryViewModel::deleteColor,
        onCreatePalette = onCreatePalette,
        onEditPalette = onEditPalette,
        onDeletePalette = libraryViewModel::deletePalette,
        onApplyPalette = libraryViewModel::applyPalette,
        onSaveProjectPalette = libraryViewModel::saveProjectPalette,
        onShowNotice = libraryViewModel::clearNotice,
        modifier = modifier,
    )
}

@Suppress("kotlin:S107", "kotlin:S3776") // Declarative library branches mirror tabs, selection, and dialogs.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    projectId: String?,
    onSelectTab: (LibraryTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onCreateColor: () -> Unit,
    onEditColor: (String, Boolean) -> Unit,
    onDeleteColor: (String) -> Unit,
    onCreatePalette: () -> Unit,
    onEditPalette: (String, Boolean) -> Unit,
    onDeletePalette: (String) -> Unit,
    onApplyPalette: (String, String) -> Unit,
    onSaveProjectPalette: (String, String) -> Unit,
    onShowNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSaveProjectPalette by remember { mutableStateOf(false) }
    var projectPaletteName by remember { mutableStateOf("") }
    val snackbarHost = remember { SnackbarHostState() }
    val currentOnShowNotice by rememberUpdatedState(onShowNotice)
    val noticeText =
        state.notice?.let { notice ->
            when (notice) {
                LibraryNotice.BuiltInCannotDelete -> {
                    stringResource(R.string.library_builtin_delete_error)
                }

                is LibraryNotice.ColorInUse -> {
                    stringResource(
                        R.string.library_color_in_use_error,
                        notice.usage.squareRoundCount,
                        notice.usage.paletteCount,
                        notice.usage.projectCount,
                    )
                }

                LibraryNotice.SaveFailed -> {
                    stringResource(R.string.library_save_failed)
                }

                LibraryNotice.ProjectPaletteSaved -> {
                    stringResource(R.string.library_project_palette_saved)
                }

                LibraryNotice.PaletteApplied -> {
                    stringResource(R.string.library_palette_applied)
                }
            }
        }
    LaunchedEffect(noticeText) {
        if (noticeText != null) {
            snackbarHost.showSnackbar(noticeText)
            currentOnShowNotice()
        }
    }

    Scaffold(
        modifier = modifier.testTag("library_screen"),
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = if (state.selectedTab == LibraryTab.COLORS) onCreateColor else onCreatePalette,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        stringResource(
                            if (state.selectedTab ==
                                LibraryTab.COLORS
                            ) {
                                R.string.library_add_color
                            } else {
                                R.string.library_add_palette
                            },
                        ),
                    )
                },
                modifier = Modifier.testTag(if (state.selectedTab == LibraryTab.COLORS) "add_color" else "add_palette"),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier.padding(horizontal = SquareToolSpacing.Standard),
                verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small),
            ) {
                Text(
                    stringResource(R.string.library_title),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.library_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth().testTag("library_search"),
                    label = { Text(stringResource(R.string.library_search_label)) },
                    placeholder = { Text(stringResource(R.string.library_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon =
                        if (state.query.isNotEmpty()) {
                            {
                                SquareToolSearchClearButton(R.string.library_clear_search, onClick = { onQueryChange("") })
                            }
                        } else {
                            null
                        },
                    singleLine = true,
                )
            }
            PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal, modifier = Modifier.padding(top = SquareToolSpacing.Small)) {
                LibraryTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        text = {
                            Text(
                                stringResource(
                                    if (tab ==
                                        LibraryTab.COLORS
                                    ) {
                                        R.string.library_tab_colors
                                    } else {
                                        R.string.library_tab_palettes
                                    },
                                ),
                            )
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("library_tab_${tab.name.lowercase()}"),
                    )
                }
            }
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (state.selectedTab == LibraryTab.COLORS) {
                if (state.colors.isEmpty()) {
                    LibraryEmptyState(state.query.isNotBlank(), LibraryTab.COLORS, onCreateColor)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("colors_list"),
                        contentPadding =
                            PaddingValues(
                                SquareToolSpacing.Standard,
                                SquareToolSpacing.Medium,
                                SquareToolSpacing.Standard,
                                104.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium),
                    ) {
                        items(state.colors, key = ColorEntity::id) { color ->
                            ColorLibraryCard(
                                color = color,
                                onEdit = { onEditColor(color.id, false) },
                                onDuplicate = { onEditColor(color.id, true) },
                                onDelete = { onDeleteColor(color.id) },
                            )
                        }
                    }
                }
            } else {
                if (state.palettes.isEmpty()) {
                    Column(Modifier.fillMaxSize()) {
                        if (projectId != null) {
                            OutlinedButton(
                                onClick = { showSaveProjectPalette = true },
                                modifier = Modifier.fillMaxWidth().padding(SquareToolSpacing.Standard).heightIn(min = 56.dp),
                            ) { Text(stringResource(R.string.library_save_project_palette)) }
                        }
                        LibraryEmptyState(
                            state.query.isNotBlank(),
                            LibraryTab.PALETTES,
                            onCreatePalette,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("palettes_list"),
                        contentPadding =
                            PaddingValues(
                                SquareToolSpacing.Standard,
                                SquareToolSpacing.Medium,
                                SquareToolSpacing.Standard,
                                104.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium),
                    ) {
                        if (projectId != null) {
                            item {
                                OutlinedButton(
                                    onClick = { showSaveProjectPalette = true },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                ) { Text(stringResource(R.string.library_save_project_palette)) }
                            }
                        }
                        items(state.palettes, key = { it.palette.id }) { item ->
                            PaletteLibraryCard(
                                item = item,
                                canApply = projectId != null,
                                onApply = { projectId?.let { onApplyPalette(it, item.palette.id) } },
                                onEdit = { onEditPalette(item.palette.id, false) },
                                onDuplicate = { onEditPalette(item.palette.id, true) },
                                onDelete = { onDeletePalette(item.palette.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSaveProjectPalette && projectId != null) {
        AlertDialog(
            onDismissRequest = { showSaveProjectPalette = false },
            title = { Text(stringResource(R.string.library_save_project_palette_title)) },
            text = {
                OutlinedTextField(
                    projectPaletteName,
                    { projectPaletteName = it },
                    label = { Text(stringResource(R.string.library_palette_name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveProjectPalette(projectId, projectPaletteName)
                        projectPaletteName = ""
                        showSaveProjectPalette = false
                    },
                    enabled = projectPaletteName.isNotBlank(),
                ) { Text(stringResource(R.string.library_save_palette)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveProjectPalette = false },
                ) { Text(stringResource(R.string.color_editor_cancel)) }
            },
        )
    }
}

@Composable
private fun LibraryEmptyState(
    hasQuery: Boolean,
    tab: LibraryTab,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().padding(SquareToolSpacing.Section), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val title =
                when {
                    hasQuery -> R.string.library_no_results_title
                    tab == LibraryTab.COLORS -> R.string.library_empty_colors_title
                    else -> R.string.library_empty_palettes_title
                }
            val body =
                when {
                    hasQuery -> R.string.library_no_results_body
                    tab == LibraryTab.COLORS -> R.string.library_empty_colors_body
                    else -> R.string.library_empty_palettes_body
                }
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!hasQuery) {
                Button(onClick = onCreate) {
                    Text(stringResource(if (tab == LibraryTab.COLORS) R.string.library_add_color else R.string.library_add_palette))
                }
            }
        }
    }
}

@Composable
private fun ColorLibraryCard(
    color: ColorEntity,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().testTag("color_card_${color.id}")) {
        Row(
            Modifier.fillMaxWidth().padding(SquareToolSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium),
        ) {
            ColorSwatch(color, Modifier.size(64.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(color.name, style = MaterialTheme.typography.titleMedium)
                Text(formatHexColor(color.argb), style = MaterialTheme.typography.bodyLarge)
                val yarn = listOfNotNull(color.yarnBrand, color.yarnLine, color.shadeName, color.shadeCode).filter(String::isNotBlank)
                if (yarn.isNotEmpty()) {
                    Text(
                        yarn.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    stringResource(if (color.builtIn) R.string.library_builtin else R.string.library_custom),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.library_more_actions, color.name))
                }
                DropdownMenu(expanded, { expanded = false }) {
                    LibraryItemMenuActions(color.builtIn, { expanded = false }, onEdit, onDuplicate, onDelete)
                }
            }
        }
    }
}

@Composable
private fun PaletteLibraryCard(
    item: PaletteListItem,
    canApply: Boolean,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().testTag("palette_card_${item.palette.id}")) {
        Column(Modifier.fillMaxWidth().padding(SquareToolSpacing.Medium), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.palette.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        pluralStringResource(R.plurals.library_color_count, item.colors.size, item.colors.size),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.library_more_actions, item.palette.name))
                    }
                    DropdownMenu(expanded, { expanded = false }) {
                        LibraryItemMenuActions(item.palette.builtIn, { expanded = false }, onEdit, onDuplicate, onDelete)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item.colors.take(8).forEach { ColorSwatch(it, Modifier.size(44.dp)) }
            }
            if (canApply) {
                Button(onClick = onApply, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.library_apply_to_project))
                }
            }
        }
    }
}

@Composable
private fun LibraryItemMenuActions(
    builtIn: Boolean,
    closeMenu: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    if (!builtIn) {
        SquareToolMenuItem(R.string.library_edit, Icons.Default.Edit, onClick = {
            closeMenu()
            onEdit()
        })
    }
    SquareToolMenuItem(R.string.library_duplicate, Icons.Default.SwapHoriz, onClick = {
        closeMenu()
        onDuplicate()
    })
    if (!builtIn) {
        SquareToolMenuItem(R.string.library_delete, Icons.Default.Delete, onClick = {
            closeMenu()
            onDelete()
        })
    }
}

@Composable
fun ColorEditorRoute(
    repository: SquareToolRepository,
    modifier: Modifier = Modifier,
    colorId: String? = null,
    duplicate: Boolean = false,
    colorEditorViewModel: ColorEditorViewModel =
        viewModel(
            key = "color-editor-$colorId-$duplicate",
            factory = ColorEditorViewModel.factory(repository, colorId, duplicate),
        ),
    onClose: () -> Unit = {},
) {
    val state by colorEditorViewModel.uiState.collectAsStateWithLifecycle()
    ColorEditorScreen(
        state = state,
        isNew = colorId == null,
        isDuplicate = duplicate,
        onDraftChange = colorEditorViewModel::updateDraft,
        onHueChange = { colorEditorViewModel.updateHsl(hue = it) },
        onSaturationChange = { colorEditorViewModel.updateHsl(saturation = it) },
        onLightnessChange = { colorEditorViewModel.updateHsl(lightness = it) },
        onSave = { colorEditorViewModel.save(onClose) },
        onCancel = onClose,
        modifier = modifier,
    )
}

@Suppress("kotlin:S107", "kotlin:S3776") // Explicit draft actions keep color editing type-safe and locally visible.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorEditorScreen(
    state: ColorEditorUiState,
    isNew: Boolean,
    isDuplicate: Boolean,
    onDraftChange: (ColorEditorDraft) -> Unit,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onLightnessChange: (Float) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title =
        when {
            isDuplicate -> R.string.color_editor_duplicate_title
            isNew -> R.string.color_editor_new_title
            else -> R.string.color_editor_edit_title
        }
    val parsedColor = parseHexColor(state.draft.hex)
    val hsl = parsedColor?.let(::argbToHsl) ?: HslColor(0f, 0f, .5f)
    Scaffold(
        modifier = modifier.testTag("color_editor"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.color_editor_cancel))
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            SquareToolLoadingIndicator(Modifier.padding(padding))
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SquareToolSpacing.Standard, vertical = SquareToolSpacing.Medium),
                verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium),
            ) {
                OutlinedTextField(
                    state.draft.name,
                    { onDraftChange(state.draft.copy(name = it)) },
                    label = { Text(stringResource(R.string.color_editor_name)) },
                    modifier = Modifier.fillMaxWidth().testTag("color_name"),
                    isError = ColorDraftError.NAME in state.validationErrors,
                    supportingText =
                        if (ColorDraftError.NAME in state.validationErrors) {
                            { Text(stringResource(R.string.color_editor_name_error)) }
                        } else {
                            null
                        },
                    singleLine = true,
                )
                OutlinedTextField(
                    state.draft.hex,
                    { onDraftChange(state.draft.copy(hex = it)) },
                    label = { Text(stringResource(R.string.color_editor_hex)) },
                    modifier = Modifier.fillMaxWidth().testTag("color_hex"),
                    isError = ColorDraftError.HEX in state.validationErrors,
                    supportingText = {
                        Text(
                            stringResource(
                                if (ColorDraftError.HEX in
                                    state.validationErrors
                                ) {
                                    R.string.color_editor_hex_error
                                } else {
                                    R.string.color_editor_hex_support
                                },
                            ),
                        )
                    },
                    singleLine = true,
                )
                Text(
                    stringResource(
                        R.string.color_editor_preview,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    modifier =
                        Modifier.semantics {
                            heading()
                        },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium)) {
                    ColorSurfacePreview(parsedColor, true, Modifier.weight(1f))
                    ColorSurfacePreview(parsedColor, false, Modifier.weight(1f))
                }
                ColorSlider(R.string.color_editor_hue, hsl.hue, 360f, onHueChange)
                ColorSlider(R.string.color_editor_saturation, hsl.saturation, 1f, onSaturationChange)
                ColorSlider(R.string.color_editor_lightness, hsl.lightness, 1f, onLightnessChange)
                HorizontalDivider()
                Text(
                    stringResource(
                        R.string.color_editor_yarn_information,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    modifier =
                        Modifier.semantics {
                            heading()
                        },
                )
                OutlinedTextField(state.draft.yarnBrand, {
                    onDraftChange(state.draft.copy(yarnBrand = it))
                }, label = { Text(stringResource(R.string.color_editor_brand)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(state.draft.yarnLine, {
                    onDraftChange(state.draft.copy(yarnLine = it))
                }, label = { Text(stringResource(R.string.color_editor_product_line)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(state.draft.shadeName, {
                    onDraftChange(state.draft.copy(shadeName = it))
                }, label = { Text(stringResource(R.string.color_editor_shade_name)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(state.draft.shadeCode, {
                    onDraftChange(state.draft.copy(shadeCode = it))
                }, label = { Text(stringResource(R.string.color_editor_shade_code)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(state.draft.skeinWeightGrams, {
                    onDraftChange(state.draft.copy(skeinWeightGrams = it))
                }, label = { Text(stringResource(R.string.color_editor_skein_weight)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(state.draft.yarnLength, {
                    onDraftChange(state.draft.copy(yarnLength = it))
                }, label = { Text(stringResource(R.string.color_editor_length)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(state.draft.yarnLengthUnit, {
                    onDraftChange(state.draft.copy(yarnLengthUnit = it))
                }, label = { Text(stringResource(R.string.color_editor_length_unit)) }, modifier = Modifier.fillMaxWidth())
                if (ColorDraftError.SKEIN_WEIGHT in state.validationErrors || ColorDraftError.YARN_LENGTH in state.validationErrors) {
                    Text(stringResource(R.string.color_editor_number_error), color = MaterialTheme.colorScheme.error)
                }
                if (state.notice == ColorEditorNotice.SaveFailed) {
                    Text(stringResource(R.string.library_save_failed), color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    state.draft.notes,
                    { onDraftChange(state.draft.copy(notes = it)) },
                    label = { Text(stringResource(R.string.color_editor_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                SquareToolEditorActions(
                    cancelLabel = R.string.color_editor_cancel,
                    saveLabel = R.string.color_editor_save,
                    saveTestTag = "save_color",
                    onCancel = onCancel,
                    onSave = onSave,
                )
                Spacer(Modifier.height(SquareToolSpacing.Section))
            }
        }
    }
}

@Composable
private fun ColorSurfacePreview(
    argb: Long?,
    light: Boolean,
    modifier: Modifier = Modifier,
) {
    val surface = if (light) Color(0xFFFFFDF7) else Color(0xFF292A1C)
    val swatch = argb?.let { Color(it.toInt()) } ?: MaterialTheme.colorScheme.surfaceVariant
    val effectiveSwatch = swatch.compositeOver(surface)
    val label = stringResource(if (light) R.string.color_editor_light_surface else R.string.color_editor_dark_surface)
    OutlinedCard(
        modifier = modifier.height(120.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = surface),
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(72.dp)
                    .border(1.dp, if (light) Color(0xFF747668) else Color(0xFF959784), CircleShape)
                    .clip(CircleShape)
                    .background(swatch)
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (effectiveSwatch.luminance() > .5f) Color(0xFF1E1E12) else Color(0xFFFFFDF7),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun ColorSlider(
    labelRes: Int,
    value: Float,
    maxValue: Float,
    onChange: (Float) -> Unit,
) {
    val label = stringResource(labelRes)
    val range = 0f..maxValue
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                stringResource(
                    if (range.endInclusive > 1f) R.string.color_editor_degrees_value else R.string.color_editor_percent_value,
                    if (range.endInclusive > 1f) value.toInt() else (value * 100).toInt(),
                ),
            )
        }
        Slider(
            value = value.coerceIn(range),
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = label },
        )
    }
}

@Composable
fun PaletteEditorRoute(
    repository: SquareToolRepository,
    modifier: Modifier = Modifier,
    paletteId: String? = null,
    duplicate: Boolean = false,
    projectId: String? = null,
    paletteEditorViewModel: PaletteEditorViewModel =
        viewModel(
            key = "palette-editor-$paletteId-$duplicate",
            factory = PaletteEditorViewModel.factory(repository, paletteId, duplicate),
        ),
    onClose: () -> Unit = {},
) {
    val state by paletteEditorViewModel.uiState.collectAsStateWithLifecycle()
    PaletteEditorScreen(
        state = state,
        isNew = paletteId == null,
        isDuplicate = duplicate,
        projectId = projectId,
        onNameChange = paletteEditorViewModel::updateName,
        onToggleColor = paletteEditorViewModel::toggleColor,
        onRemoveColor = paletteEditorViewModel::removeColor,
        onMoveColor = paletteEditorViewModel::moveColor,
        onSave = { paletteEditorViewModel.save(onClose) },
        onApply = { projectId?.let(paletteEditorViewModel::applyToProject) },
        onCancel = onClose,
        modifier = modifier,
    )
}

@Suppress("kotlin:S107", "kotlin:S3776") // Explicit draft actions keep palette editing type-safe and locally visible.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteEditorScreen(
    state: PaletteEditorUiState,
    isNew: Boolean,
    isDuplicate: Boolean,
    projectId: String?,
    onNameChange: (String) -> Unit,
    onToggleColor: (String) -> Unit,
    onRemoveColor: (String) -> Unit,
    onMoveColor: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title =
        when {
            isDuplicate -> R.string.palette_editor_duplicate_title
            isNew -> R.string.palette_editor_new_title
            else -> R.string.palette_editor_edit_title
        }
    val colorsById = state.availableColors.associateBy(ColorEntity::id)
    Scaffold(
        modifier = modifier.testTag("palette_editor"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.palette_editor_cancel))
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding =
                    PaddingValues(
                        SquareToolSpacing.Standard,
                        SquareToolSpacing.Medium,
                        SquareToolSpacing.Standard,
                        SquareToolSpacing.Section,
                    ),
                verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium),
            ) {
                item {
                    OutlinedTextField(
                        state.draft.name,
                        onNameChange,
                        label = { Text(stringResource(R.string.palette_editor_name)) },
                        modifier = Modifier.fillMaxWidth().testTag("palette_name"),
                        singleLine = true,
                    )
                }
                item {
                    Text(
                        stringResource(
                            R.string.palette_editor_saved_colors,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        modifier =
                            Modifier.semantics {
                                heading()
                            },
                    )
                }
                items(state.availableColors, key = ColorEntity::id) { color ->
                    val checked = color.id in state.draft.colorIds
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(
                                min = 56.dp,
                            ).clickable(role = Role.Checkbox) { onToggleColor(color.id) }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Checkbox(checked, { onToggleColor(color.id) })
                        ColorSwatch(color, Modifier.size(40.dp))
                        Text(color.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    }
                }
                item { HorizontalDivider() }
                item {
                    Text(
                        stringResource(
                            R.string.palette_editor_selected_colors,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        modifier =
                            Modifier.semantics {
                                heading()
                            },
                    )
                }
                itemsIndexed(state.draft.colorIds, key = { _, id -> id }) { index, id ->
                    colorsById[id]?.let { color ->
                        SelectedPaletteColorRow(
                            color = color,
                            index = index,
                            isFirst = index == 0,
                            isLast = index == state.draft.colorIds.lastIndex,
                            onRemove = { onRemoveColor(id) },
                            onMoveUp = { onMoveColor(index, index - 1) },
                            onMoveDown = { onMoveColor(index, index + 1) },
                        )
                    }
                }
                if (state.notice == PaletteEditorNotice.InvalidDraft) {
                    item { Text(stringResource(R.string.palette_editor_error), color = MaterialTheme.colorScheme.error) }
                }
                if (state.notice == PaletteEditorNotice.SaveFailed) {
                    item { Text(stringResource(R.string.library_save_failed), color = MaterialTheme.colorScheme.error) }
                }
                if (state.notice == PaletteEditorNotice.PaletteApplied) {
                    item { Text(stringResource(R.string.library_palette_applied), color = MaterialTheme.colorScheme.primary) }
                }
                item {
                    SquareToolEditorActions(
                        cancelLabel = R.string.palette_editor_cancel,
                        saveLabel = R.string.palette_editor_save,
                        saveTestTag = "save_palette",
                        onCancel = onCancel,
                        onSave = onSave,
                    )
                }
                if (projectId != null) {
                    item {
                        OutlinedButton(onClick = onApply, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Text(stringResource(R.string.palette_editor_apply))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedPaletteColorRow(
    color: ColorEntity,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().testTag("palette_color_$index")) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColorSwatch(color, Modifier.size(48.dp))
            Text(
                color.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onMoveUp, enabled = !isFirst) {
                Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.palette_editor_move_color_up, color.name))
            }
            IconButton(onClick = onMoveDown, enabled = !isLast) {
                Icon(Icons.Default.ArrowDownward, contentDescription = stringResource(R.string.palette_editor_move_color_down, color.name))
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.palette_editor_remove_color, color.name))
            }
        }
    }
}
