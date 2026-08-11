package com.finnvek.squaretool.ui.squares

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
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
import com.finnvek.squaretool.render.MotifRenderConfig
import com.finnvek.squaretool.render.MotifSurface
import com.finnvek.squaretool.render.MotifTemplate
import com.finnvek.squaretool.render.MotifTemplateRegistry
import com.finnvek.squaretool.render.SquareDesignVisual
import com.finnvek.squaretool.render.drawMotif
import com.finnvek.squaretool.ui.theme.SquareToolSpacing
import androidx.compose.foundation.lazy.grid.items as gridItems

@Composable
fun SquaresRoute(
    repository: SquareToolRepository,
    modifier: Modifier = Modifier,
    onCreateDesign: () -> Unit = {},
    onEditDesign: (id: String, duplicate: Boolean) -> Unit = { _, _ -> },
    onUseInProject: (id: String) -> Unit = {},
) {
    val viewModel: SquaresViewModel = viewModel(factory = SquaresViewModel.factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SquaresScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onSelectDesign = viewModel::selectDesign,
        onFavorite = viewModel::toggleFavorite,
        onCreateDesign = onCreateDesign,
        onEditDesign = onEditDesign,
        onUseInProject = onUseInProject,
        onDeleteDesign = viewModel::deleteDesign,
        onNoticeShown = viewModel::clearNotice,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquaresScreen(
    state: SquaresUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SquareFilter) -> Unit,
    onSelectDesign: (String?) -> Unit,
    onFavorite: (String) -> Unit,
    onCreateDesign: () -> Unit,
    onEditDesign: (String, Boolean) -> Unit,
    onUseInProject: (String) -> Unit,
    onDeleteDesign: (String) -> Unit,
    onNoticeShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val noticeMessage =
        state.notice?.let { notice ->
            when (notice) {
                SquaresNotice.BuiltInCannotDelete -> {
                    stringResource(R.string.squares_built_in_delete_error)
                }

                is SquaresNotice.DesignInUse -> {
                    stringResource(
                        R.string.squares_design_in_use_error,
                        notice.usage.projectCellCount,
                        notice.usage.defaultProjectCount,
                    )
                }

                SquaresNotice.SaveFailed -> {
                    stringResource(R.string.squares_save_failed)
                }
            }
        }
    LaunchedEffect(noticeMessage) {
        if (noticeMessage != null) {
            snackbarHostState.showSnackbar(noticeMessage)
            onNoticeShown()
        }
    }

    Scaffold(
        modifier = modifier.testTag("squares_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateDesign,
                modifier = Modifier.testTag("add_square"),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.squares_add))
            }
        },
    ) { contentPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = SquareToolSpacing.Standard),
                verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small),
            ) {
                Text(
                    text = stringResource(R.string.squares_title),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.squares_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth().testTag("squares_search"),
                    label = { Text(stringResource(R.string.squares_search_label)) },
                    placeholder = { Text(stringResource(R.string.squares_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon =
                        if (state.query.isNotEmpty()) {
                            {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.squares_clear_search))
                                }
                            }
                        } else {
                            null
                        },
                    singleLine = true,
                )
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = SquareToolSpacing.Standard, vertical = SquareToolSpacing.Medium),
                horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small),
            ) {
                items(SquareFilter.entries) { filter ->
                    FilterChip(
                        selected = state.selectedFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(stringResource(filter.labelRes())) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("square_filter_${filter.name.lowercase()}"),
                    )
                }
            }
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.designs.isEmpty() -> {
                    SquareEmptyState(
                        hasCriteria = state.query.isNotBlank() || state.selectedFilter != SquareFilter.ALL,
                        onCreateDesign = onCreateDesign,
                    )
                }

                else -> {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val columns = if (maxWidth >= 600.dp) 3 else 2
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize().testTag("squares_grid"),
                            contentPadding =
                                PaddingValues(
                                    start = SquareToolSpacing.Standard,
                                    end = SquareToolSpacing.Standard,
                                    bottom = 96.dp,
                                ),
                            verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium),
                            horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium),
                        ) {
                            gridItems(state.designs, key = SquareDesignListItem::id) { item ->
                                SquareDesignCard(
                                    item = item,
                                    onSelect = { onSelectDesign(item.id) },
                                    onFavorite = { onFavorite(item.id) },
                                    onEdit = { onEditDesign(item.id, false) },
                                    onDuplicate = { onEditDesign(item.id, true) },
                                    onUseInProject = { onUseInProject(item.id) },
                                    onDelete = { onDeleteDesign(item.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    state.selectedDesign?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { onSelectDesign(null) },
            modifier = Modifier.testTag("square_details"),
        ) {
            SquareDetails(
                item = item,
                onFavorite = { onFavorite(item.id) },
                onEdit = { onEditDesign(item.id, false) },
                onDuplicate = { onEditDesign(item.id, true) },
                onUseInProject = { onUseInProject(item.id) },
            )
        }
    }
}

@Composable
private fun SquareEmptyState(
    hasCriteria: Boolean,
    onCreateDesign: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(SquareToolSpacing.Section), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(if (hasCriteria) R.string.squares_no_results_title else R.string.squares_empty_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(if (hasCriteria) R.string.squares_no_results_body else R.string.squares_empty_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!hasCriteria) Button(onClick = onCreateDesign) { Text(stringResource(R.string.squares_add)) }
        }
    }
}

@Composable
private fun SquareDesignCard(
    item: SquareDesignListItem,
    onSelect: () -> Unit,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onUseInProject: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth().testTag("square_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(Modifier.fillMaxWidth().padding(SquareToolSpacing.Medium)) {
            Column(verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small)) {
                MotifPreview(
                    templateId = item.design.motifTemplateId,
                    colors = item.roundColors,
                    contentDescription = item.design.name,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
                Text(item.design.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    pluralStringResource(R.plurals.squares_round_count, item.roundColors.size, item.roundColors.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onFavorite,
                modifier = Modifier.align(Alignment.TopEnd).background(MaterialTheme.colorScheme.surface.copy(alpha = .88f), CircleShape),
            ) {
                Icon(
                    if (item.design.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription =
                        stringResource(
                            if (item.design.favorite) R.string.squares_unfavorite else R.string.squares_favorite,
                        ),
                    tint = if (item.design.favorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                )
            }
            Box(Modifier.align(Alignment.BottomEnd)) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.squares_more_actions, item.design.name))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (item.canEdit) {
                        MenuAction(R.string.squares_edit, Icons.Default.Edit) {
                            menuExpanded = false
                            onEdit()
                        }
                    }
                    MenuAction(R.string.squares_duplicate, Icons.Default.SwapHoriz) {
                        menuExpanded = false
                        onDuplicate()
                    }
                    MenuAction(R.string.squares_use_in_project, Icons.Default.Add) {
                        menuExpanded = false
                        onUseInProject()
                    }
                    MenuAction(
                        if (item.design.favorite) R.string.squares_unfavorite else R.string.squares_favorite,
                        if (item.design.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    ) {
                        menuExpanded = false
                        onFavorite()
                    }
                    if (item.canEdit) {
                        MenuAction(R.string.squares_delete, Icons.Default.Delete) {
                            menuExpanded = false
                            onDelete()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuAction(
    @StringRes label: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        onClick = onClick,
        leadingIcon = { Icon(icon, contentDescription = null) },
    )
}

@Composable
private fun SquareDetails(
    item: SquareDesignListItem,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onUseInProject: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SquareToolSpacing.Standard, vertical = SquareToolSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Standard), verticalAlignment = Alignment.CenterVertically) {
            MotifPreview(
                templateId = item.design.motifTemplateId,
                colors = item.roundColors,
                contentDescription = item.design.name,
                modifier = Modifier.size(128.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(item.design.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Text(pluralStringResource(R.plurals.squares_round_count, item.roundColors.size, item.roundColors.size))
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (item.design.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription =
                        stringResource(
                            if (item.design.favorite) R.string.squares_unfavorite else R.string.squares_favorite,
                        ),
                )
            }
        }
        Text(stringResource(R.string.squares_round_colors), style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium)) {
            itemsIndexed(item.roundColors) { index, color ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ColorSwatch(color, Modifier.size(48.dp))
                    Text(stringResource(R.string.square_editor_round_number, index + 1), style = MaterialTheme.typography.bodyMedium)
                    Text(color.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
            }
        }
        if (item.design.note.isNotBlank()) {
            Text(stringResource(R.string.squares_notes), style = MaterialTheme.typography.titleMedium)
            Text(item.design.note, style = MaterialTheme.typography.bodyLarge)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium)) {
            OutlinedButton(
                onClick = if (item.canEdit) onEdit else onDuplicate,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            ) { Text(stringResource(if (item.canEdit) R.string.squares_edit else R.string.squares_duplicate)) }
            Button(onClick = onUseInProject, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                Text(stringResource(R.string.squares_use_in_project))
            }
        }
        Spacer(Modifier.height(SquareToolSpacing.Standard))
    }
}

@Composable
fun MotifPreview(
    templateId: String,
    colors: List<ColorEntity>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val template = MotifTemplateRegistry.find(templateId) ?: MotifTemplateRegistry.templates.first()
    val fallbackArgb = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val resolved = colors.map { it.argb.toInt() }.take(template.maxRounds).toMutableList()
    while (resolved.size < template.minRounds) resolved += (resolved.lastOrNull() ?: fallbackArgb)
    val surface = if (MaterialTheme.colorScheme.surface.luminance() < .5f) MotifSurface.DARK else MotifSurface.LIGHT
    Canvas(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics { this.contentDescription = contentDescription },
    ) {
        drawMotif(SquareDesignVisual(template.id, resolved), MotifRenderConfig(surface = surface))
    }
}

@Composable
fun ColorSwatch(
    color: ColorEntity,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clip(CircleShape)
                .background(Color(color.argb.toInt()))
                .semantics { contentDescription = color.name },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquareEditorRoute(
    repository: SquareToolRepository,
    modifier: Modifier = Modifier,
    designId: String? = null,
    duplicate: Boolean = false,
    onClose: () -> Unit = {},
) {
    val editorViewModel: SquareEditorViewModel =
        viewModel(
            key = "square-editor-$designId-$duplicate",
            factory = SquareEditorViewModel.factory(repository, designId, duplicate),
        )
    val state by editorViewModel.uiState.collectAsStateWithLifecycle()
    SquareEditorScreen(
        state = state,
        isNew = designId == null,
        isDuplicate = duplicate,
        onNameChange = editorViewModel::updateName,
        onNotesChange = editorViewModel::updateNotes,
        onFavoriteChange = editorViewModel::updateFavorite,
        onTemplateSelected = editorViewModel::selectTemplate,
        onAssignColor = editorViewModel::assignColor,
        onAddRound = editorViewModel::addRound,
        onRemoveRound = editorViewModel::removeRound,
        onMoveRound = editorViewModel::moveRound,
        onCreateColor = editorViewModel::createAndAssignColor,
        onSave = { editorViewModel.save(onClose) },
        onCancel = onClose,
        onConfirmTemplate = editorViewModel::confirmTemplateChange,
        onCancelTemplate = editorViewModel::cancelTemplateChange,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquareEditorScreen(
    state: SquareEditorUiState,
    isNew: Boolean,
    isDuplicate: Boolean,
    onNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onFavoriteChange: (Boolean) -> Unit,
    onTemplateSelected: (String) -> Unit,
    onAssignColor: (Int, String) -> Unit,
    onAddRound: () -> Unit,
    onRemoveRound: (Int) -> Unit,
    onMoveRound: (Int, Int) -> Unit,
    onCreateColor: (Int, String, String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onConfirmTemplate: () -> Unit,
    onCancelTemplate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var choosingRound by remember { mutableIntStateOf(-1) }
    var creatingColorForRound by remember { mutableIntStateOf(-1) }
    var newColorName by remember { mutableStateOf("") }
    var newColorHex by remember { mutableStateOf("#6B8A2E") }
    val title =
        when {
            isDuplicate -> R.string.square_editor_duplicate_title
            isNew -> R.string.square_editor_new_title
            else -> R.string.square_editor_edit_title
        }
    Scaffold(
        modifier = modifier.testTag("square_editor"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.square_editor_cancel))
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = SquareToolSpacing.Standard, vertical = SquareToolSpacing.Medium),
                verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Section),
            ) {
                Text(
                    stringResource(
                        R.string.square_editor_live_preview,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    modifier =
                        Modifier.semantics {
                            heading()
                        },
                )
                val colorMap = state.colors.associateBy(ColorEntity::id)
                MotifPreview(
                    templateId = state.draft.templateId,
                    colors = state.draft.roundColorIds.mapNotNull(colorMap::get),
                    contentDescription = state.draft.name.ifBlank { stringResource(R.string.square_editor_live_preview) },
                    modifier =
                        Modifier
                            .widthIn(max = 360.dp)
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                            .aspectRatio(1f),
                )
                OutlinedTextField(
                    value = state.draft.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.square_editor_name)) },
                    modifier = Modifier.fillMaxWidth().testTag("square_name"),
                    isError = SquareDraftError.NAME in state.validationErrors,
                    supportingText =
                        if (SquareDraftError.NAME in state.validationErrors) {
                            { Text(stringResource(R.string.square_editor_name_error)) }
                        } else {
                            null
                        },
                    singleLine = true,
                )
                Text(
                    stringResource(
                        R.string.square_editor_template,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    modifier =
                        Modifier.semantics {
                            heading()
                        },
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium)) {
                    items(MotifTemplateRegistry.templates, key = MotifTemplate::id) { template ->
                        TemplateCard(
                            template = template,
                            selected = template.id == state.draft.templateId,
                            colors = state.draft.roundColorIds.mapNotNull(colorMap::get),
                            onClick = { onTemplateSelected(template.id) },
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(
                            R.string.square_editor_rounds,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        modifier =
                            Modifier.weight(1f).semantics {
                                heading()
                            },
                    )
                    FilledTonalButton(onClick = onAddRound, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.square_editor_add_round))
                    }
                }
                state.draft.roundColorIds.forEachIndexed { index, colorId ->
                    val color = colorMap[colorId]
                    RoundEditorRow(
                        index = index,
                        color = color,
                        canRemove = state.draft.roundColorIds.size > MotifTemplateRegistry.require(state.draft.templateId).minRounds,
                        onChange = { choosingRound = index },
                        onRemove = { onRemoveRound(index) },
                        onMoveUp = { onMoveRound(index, index - 1) },
                        onMoveDown = { onMoveRound(index, index + 1) },
                        isFirst = index == 0,
                        isLast = index == state.draft.roundColorIds.lastIndex,
                    )
                }
                if (SquareDraftError.ROUND_COUNT in state.validationErrors || SquareDraftError.ROUND_COLOR in state.validationErrors) {
                    Text(stringResource(R.string.square_editor_round_error), color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    value = state.draft.notes,
                    onValueChange = onNotesChange,
                    label = { Text(stringResource(R.string.square_editor_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.square_editor_favorite),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = state.draft.favorite, onCheckedChange = onFavoriteChange)
                }
                Text(
                    stringResource(R.string.square_editor_disclaimer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.notice?.let { notice ->
                    Text(
                        stringResource(
                            when (notice) {
                                SquareEditorNotice.InvalidDraft -> R.string.square_editor_round_error
                                SquareEditorNotice.InvalidColor -> R.string.square_editor_color_error
                                SquareEditorNotice.SaveFailed -> R.string.squares_save_failed
                            },
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium)) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                        Text(stringResource(R.string.square_editor_cancel))
                    }
                    Button(onClick = onSave, modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("save_square")) {
                        Text(stringResource(R.string.square_editor_save))
                    }
                }
                Spacer(Modifier.height(SquareToolSpacing.Section))
            }
        }
    }

    if (choosingRound >= 0) {
        AlertDialog(
            onDismissRequest = { choosingRound = -1 },
            title = { Text(stringResource(R.string.square_editor_change_color, choosingRound + 1)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(state.colors, key = ColorEntity::id) { color ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable(role = Role.Button) {
                                    onAssignColor(choosingRound, color.id)
                                    choosingRound = -1
                                }.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ColorSwatch(color, Modifier.size(40.dp))
                            Text(color.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    creatingColorForRound = choosingRound
                    choosingRound = -1
                }) {
                    Text(stringResource(R.string.square_editor_new_color))
                }
            },
            dismissButton = { TextButton(onClick = { choosingRound = -1 }) { Text(stringResource(R.string.square_editor_cancel)) } },
        )
    }
    if (creatingColorForRound >= 0) {
        AlertDialog(
            onDismissRequest = { creatingColorForRound = -1 },
            title = { Text(stringResource(R.string.square_editor_new_color_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        newColorName,
                        { newColorName = it },
                        label = { Text(stringResource(R.string.square_editor_color_name)) },
                    )
                    OutlinedTextField(newColorHex, { newColorHex = it }, label = { Text(stringResource(R.string.square_editor_color_hex)) })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onCreateColor(creatingColorForRound, newColorName, newColorHex)
                    creatingColorForRound = -1
                    newColorName = ""
                    newColorHex = "#6B8A2E"
                }) { Text(stringResource(R.string.square_editor_create_color)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { creatingColorForRound = -1 },
                ) { Text(stringResource(R.string.square_editor_cancel)) }
            },
        )
    }
    state.pendingTemplateChange?.let {
        AlertDialog(
            onDismissRequest = onCancelTemplate,
            title = { Text(stringResource(R.string.square_editor_confirm_template_title)) },
            text = { Text(stringResource(R.string.square_editor_confirm_template_body)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmTemplate,
                ) { Text(stringResource(R.string.square_editor_confirm_template_action)) }
            },
            dismissButton = { TextButton(onClick = onCancelTemplate) { Text(stringResource(R.string.square_editor_cancel)) } },
        )
    }
}

@Composable
private fun TemplateCard(
    template: MotifTemplate,
    selected: Boolean,
    colors: List<ColorEntity>,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(152.dp).testTag("template_${template.id}"),
        colors =
            CardDefaults.cardColors(
                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MotifPreview(template.id, colors, stringResource(template.nameRes()), Modifier.fillMaxWidth().aspectRatio(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(template.nameRes()),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
                if (selected) {
                    Icon(Icons.Default.Check, contentDescription = stringResource(R.string.square_editor_selected_template))
                }
            }
            Text(
                stringResource(R.string.square_editor_template_round_range, template.minRounds, template.maxRounds),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun RoundEditorRow(
    index: Int,
    color: ColorEntity?,
    canRemove: Boolean,
    onChange: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean,
) {
    Card(Modifier.fillMaxWidth().testTag("round_$index")) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (color !=
                null
            ) {
                ColorSwatch(color, Modifier.size(48.dp))
            } else {
                Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.square_editor_round_number, index + 1), style = MaterialTheme.typography.titleMedium)
                Text(color?.name ?: stringResource(R.string.square_editor_color_not_selected), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onChange) { Text(stringResource(R.string.square_editor_change_color, index + 1)) }
            }
            Column {
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.square_editor_move_round_up, index + 1))
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = stringResource(R.string.square_editor_move_round_down, index + 1),
                    )
                }
            }
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.square_editor_remove_round, index + 1))
                }
            }
        }
    }
}

@StringRes
private fun SquareFilter.labelRes(): Int =
    when (this) {
        SquareFilter.ALL -> R.string.squares_filter_all
        SquareFilter.FAVORITES -> R.string.squares_filter_favorites
        SquareFilter.FLORAL -> R.string.squares_filter_floral
        SquareFilter.GEOMETRIC -> R.string.squares_filter_geometric
        SquareFilter.SIMPLE -> R.string.squares_filter_simple
        SquareFilter.CUSTOM -> R.string.squares_filter_custom
    }

@StringRes
private fun MotifTemplate.nameRes(): Int =
    when (id) {
        "classic_granny" -> R.string.square_editor_template_classic_granny
        "sunburst" -> R.string.square_editor_template_sunburst
        "daisy" -> R.string.square_editor_template_daisy
        "flower_medallion" -> R.string.square_editor_template_flower_medallion
        "solid_center" -> R.string.square_editor_template_solid_center
        "star_bloom" -> R.string.square_editor_template_star_bloom
        "diamond_layers" -> R.string.square_editor_template_diamond_layers
        "pinwheel" -> R.string.square_editor_template_pinwheel
        "corner_accent" -> R.string.square_editor_template_corner_accent
        else -> R.string.square_editor_template_simple_rounds
    }
