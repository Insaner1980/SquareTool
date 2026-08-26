package com.finnvek.squaretool.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finnvek.squaretool.R
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.repository.SquareToolRepository
import com.finnvek.squaretool.render.MotifRenderConfig
import com.finnvek.squaretool.render.SquareDesignVisual
import com.finnvek.squaretool.render.drawMotif
import com.finnvek.squaretool.ui.SquareToolLoadingIndicator
import com.finnvek.squaretool.ui.projects.ProjectBlanketPreview
import com.finnvek.squaretool.ui.projects.ProjectCardModel
import com.finnvek.squaretool.ui.theme.LocalReduceMotion
import com.finnvek.squaretool.ui.theme.SquareToolMotion
import com.finnvek.squaretool.ui.theme.SquareToolSpacing

@Suppress("kotlin:S107") // Route callbacks expose the home destinations without an untyped action bag.
@Composable
fun HomeRoute(
    repository: SquareToolRepository,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(repository)),
    onOpenPlanner: (String) -> Unit = {},
    onOpenInsights: (String) -> Unit = {},
    onEditProject: (String) -> Unit = {},
    onViewAllProjects: () -> Unit = {},
    onNewProject: () -> Unit = {},
    onSampleCreate: (String) -> Unit = {},
) {
    val state by homeViewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onQueryChange = homeViewModel::updateQuery,
        onOpenPlanner = { homeViewModel.openProject(it, onOpenPlanner) },
        onOpenInsights = onOpenInsights,
        onEditProject = onEditProject,
        onDuplicateProject = { homeViewModel.duplicate(it) },
        onToggleFavorite = homeViewModel::toggleFavorite,
        onRequestDelete = homeViewModel::requestDelete,
        onConfirmDelete = homeViewModel::confirmDelete,
        onCancelDelete = homeViewModel::cancelDelete,
        onViewAllProjects = onViewAllProjects,
        onNewProject = onNewProject,
        onCreateSample = { homeViewModel.createSample(onSampleCreate) },
        modifier = modifier,
    )
}

@Suppress("kotlin:S107", "kotlin:S3776") // The screen renders independent home states and explicit user actions.
@Composable
fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit = {},
    onOpenPlanner: (String) -> Unit = {},
    onOpenInsights: (String) -> Unit = {},
    onEditProject: (String) -> Unit = {},
    onDuplicateProject: (String) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onRequestDelete: (ProjectEntity) -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onCancelDelete: () -> Unit = {},
    onViewAllProjects: () -> Unit = {},
    onNewProject: () -> Unit = {},
    onCreateSample: () -> Unit = {},
) {
    var searchExpanded by rememberSaveable { mutableStateOf(state.query.isNotBlank()) }
    Scaffold(
        modifier = modifier.testTag("home_screen"),
        topBar = {
            HomeBrandHeader(
                searchExpanded = searchExpanded,
                query = state.query,
                onSearchExpandedChange = {
                    searchExpanded = it
                    if (!it) onQueryChange("")
                },
                onQueryChange = onQueryChange,
            )
        },
    ) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding)
        if (state.isLoading) {
            SquareToolLoadingIndicator(contentModifier)
        } else {
            LazyColumn(
                modifier = contentModifier,
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        horizontal = SquareToolSpacing.Standard,
                        vertical = SquareToolSpacing.Standard,
                    ),
                verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Section),
            ) {
                if (state.query.isNotBlank()) {
                    item {
                        Text(
                            stringResource(R.string.home_search_results),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    if (state.searchResults.isEmpty()) {
                        item { Text(stringResource(R.string.home_no_search_results), style = MaterialTheme.typography.bodyLarge) }
                    } else {
                        items(state.searchResults, key = { "search-${it.project.id}" }) { project ->
                            SearchProjectRow(project, onClick = { onOpenPlanner(project.project.id) })
                        }
                    }
                } else if (state.current == null) {
                    item {
                        HomeEmptyState(onNewProject = onNewProject, onCreateSample = onCreateSample)
                    }
                } else {
                    item {
                        CurrentProjectCard(
                            model = state.current,
                            onOpenPlanner = { onOpenPlanner(state.current.project.id) },
                            onInsights = { onOpenInsights(state.current.project.id) },
                            onEdit = { onEditProject(state.current.project.id) },
                            onDuplicate = { onDuplicateProject(state.current.project.id) },
                            onFavorite = { onToggleFavorite(state.current.project.id) },
                            onDelete = { onRequestDelete(state.current.project) },
                        )
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.home_recent_projects),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f).semantics { heading() },
                            )
                            TextButton(onClick = onViewAllProjects) {
                                Text(stringResource(R.string.home_view_all_projects))
                            }
                        }
                    }
                    if (state.recent.isNotEmpty()) {
                        items(state.recent, key = { "recent-${it.project.id}" }) { project ->
                            SearchProjectRow(project, onClick = { onOpenPlanner(project.project.id) })
                        }
                    }
                    item {
                        FilledTonalButton(
                            onClick = onNewProject,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("home_new_project"),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(SquareToolSpacing.Small))
                            Text(stringResource(R.string.home_new_project))
                        }
                    }
                    item { YarnColorSummary(state.current) }
                }
                state.notice?.let {
                    item { Text(stringResource(R.string.home_save_failed), color = MaterialTheme.colorScheme.error) }
                }
                item { Spacer(Modifier.size(SquareToolSpacing.ExtraLarge)) }
            }
        }
    }

    state.pendingDelete?.let { project ->
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text(stringResource(R.string.home_delete_title, project.name)) },
            text = { Text(stringResource(R.string.home_delete_body)) },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) { Text(stringResource(R.string.home_delete)) }
            },
            dismissButton = {
                TextButton(onClick = onCancelDelete) { Text(stringResource(R.string.home_cancel)) }
            },
        )
    }
}

@Composable
private fun HomeBrandHeader(
    searchExpanded: Boolean,
    query: String,
    onSearchExpandedChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Column(
            modifier =
                Modifier.fillMaxWidth().padding(
                    horizontal = SquareToolSpacing.Standard,
                    vertical = SquareToolSpacing.Medium,
                ),
            verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMotif(Modifier.size(56.dp))
                Spacer(Modifier.width(SquareToolSpacing.Medium))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.home_brand_title),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        stringResource(R.string.home_brand_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                IconButton(onClick = { onSearchExpandedChange(!searchExpanded) }) {
                    Icon(
                        if (searchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription =
                            stringResource(
                                if (searchExpanded) R.string.home_close_search else R.string.home_search,
                            ),
                    )
                }
            }
            if (searchExpanded) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text(stringResource(R.string.home_search_hint)) },
                    modifier = Modifier.fillMaxWidth().testTag("home_search"),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                )
            }
        }
    }
}

@Composable
private fun BrandMotif(modifier: Modifier = Modifier) {
    Canvas(modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surface)) {
        drawMotif(
            SquareDesignVisual(
                templateId = "classic_granny",
                roundColors =
                    listOf(
                        0xFFD75A1F.toInt(),
                        0xFFF3E6C9.toInt(),
                        0xFF6B8A2E.toInt(),
                    ),
            ),
            MotifRenderConfig(),
        )
    }
}

@Composable
private fun HomeEmptyState(
    onNewProject: () -> Unit,
    onCreateSample: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().testTag("home_empty_state")) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(SquareToolSpacing.Section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Standard),
        ) {
            BrandMotif(Modifier.size(112.dp))
            Text(
                stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.home_empty_body), style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = onNewProject,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("home_create_project"),
            ) { Text(stringResource(R.string.home_create_project)) }
            OutlinedButton(
                onClick = onCreateSample,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("home_create_sample"),
            ) { Text(stringResource(R.string.home_create_sample)) }
        }
    }
}

@Suppress("kotlin:S3776") // Card branches directly mirror the project's progress and menu state.
@Composable
private fun CurrentProjectCard(
    model: ProjectCardModel,
    onOpenPlanner: () -> Unit,
    onInsights: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(SquareToolSpacing.Standard),
            verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Standard),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.home_current_project),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        model.project.name,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (model.project.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription =
                            stringResource(
                                if (model.project.favorite) R.string.home_remove_favorite else R.string.home_add_favorite,
                            ),
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.home_project_actions))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_view_insights)) },
                            onClick = {
                                menuExpanded = false
                                onInsights()
                            },
                            leadingIcon = { Icon(Icons.Default.Insights, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_edit_project)) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_duplicate_project)) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (model.project.favorite) R.string.home_remove_favorite else R.string.home_add_favorite,
                                    ),
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onFavorite()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_delete_project)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.home_grid_size, model.project.columnCount, model.project.rowCount))
                Text(pluralStringResource(R.plurals.home_total_squares, model.totalSquares, model.totalSquares))
            }
            ProjectBlanketPreview(
                project = model,
                contentDescription = model.project.name,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.45f),
            )
            model.progress?.let { progress ->
                val reduceMotion = LocalReduceMotion.current
                val renderedProgress by animateFloatAsState(
                    targetValue = progress.percentage / 100f,
                    animationSpec =
                        tween(
                            durationMillis = if (reduceMotion) 0 else SquareToolMotion.Standard,
                        ),
                    label = "project progress",
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { renderedProgress },
                            modifier = Modifier.fillMaxSize(),
                        )
                        Text(stringResource(R.string.home_progress, progress.percentage), style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(SquareToolSpacing.Medium))
                    Text(
                        stringResource(R.string.home_progress, progress.percentage),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Button(
                onClick = onOpenPlanner,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("home_open_planner"),
            ) { Text(stringResource(R.string.home_open_planner)) }
        }
    }
}

@Composable
private fun SearchProjectRow(
    model: ProjectCardModel,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SquareToolSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium),
        ) {
            ProjectBlanketPreview(
                project = model,
                contentDescription = model.project.name,
                modifier = Modifier.size(72.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(model.project.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.home_grid_size, model.project.columnCount, model.project.rowCount))
            }
            model.progress?.let { Text(stringResource(R.string.home_progress, it.percentage)) }
        }
    }
}

@Composable
private fun YarnColorSummary(model: ProjectCardModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(SquareToolSpacing.Standard),
            verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.home_yarn_color_summary),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Text(pluralStringResource(R.plurals.home_color_count, model.palette.size, model.palette.size))
            }
            if (model.palette.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small)) {
                    model.palette.take(7).forEach { color ->
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(color.argb.toInt()))
                                .semantics { contentDescription = color.name },
                        )
                    }
                }
            }
            Text(
                model.yarnEstimate?.let { stringResource(R.string.home_estimated_skeins, it.equivalentSkeins) }
                    ?: stringResource(R.string.home_yarn_not_configured),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
