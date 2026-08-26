package com.finnvek.squaretool.ui.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finnvek.squaretool.R
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.repository.SquareToolRepository
import com.finnvek.squaretool.ui.SquareToolLoadingIndicator
import com.finnvek.squaretool.ui.theme.SquareToolSpacing
import java.text.DateFormat
import java.util.Date

@Suppress("kotlin:S107") // Route callbacks keep project destinations and actions independently typed.
@Composable
fun ProjectsRoute(
    repository: SquareToolRepository,
    modifier: Modifier = Modifier,
    projectsViewModel: ProjectsViewModel = viewModel(factory = ProjectsViewModel.factory(repository)),
    onBack: (() -> Unit)? = null,
    onOpenProject: (String) -> Unit = {},
    onOpenInsights: (String) -> Unit = {},
    onEditProject: (String) -> Unit = {},
    onNewProject: () -> Unit = {},
) {
    val state by projectsViewModel.uiState.collectAsStateWithLifecycle()
    ProjectsScreen(
        state = state,
        onBack = onBack,
        onQueryChange = projectsViewModel::updateQuery,
        onSortChange = projectsViewModel::updateSort,
        onFavoriteOnlyChange = projectsViewModel::updateFavoriteOnly,
        onOpenProject = { projectsViewModel.openProject(it, onOpenProject) },
        onOpenInsights = onOpenInsights,
        onEditProject = onEditProject,
        onToggleFavorite = projectsViewModel::toggleFavorite,
        onRequestDelete = projectsViewModel::requestDelete,
        onRequestDuplicate = projectsViewModel::requestDuplicate,
        onRequestRename = projectsViewModel::requestRename,
        onConfirmDelete = projectsViewModel::confirmDelete,
        onConfirmDuplicate = { projectsViewModel.confirmDuplicate() },
        onConfirmRename = projectsViewModel::confirmRename,
        onCancelConfirmation = projectsViewModel::cancelConfirmation,
        onNewProject = onNewProject,
        modifier = modifier,
    )
}

@Suppress("kotlin:S107", "kotlin:S3776") // Project list states and actions are explicit declarative branches.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    state: ProjectsUiState,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onQueryChange: (String) -> Unit = {},
    onSortChange: (ProjectSort) -> Unit = {},
    onFavoriteOnlyChange: (Boolean) -> Unit = {},
    onOpenProject: (String) -> Unit = {},
    onOpenInsights: (String) -> Unit = {},
    onEditProject: (String) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onRequestDelete: (ProjectEntity) -> Unit = {},
    onRequestDuplicate: (ProjectEntity) -> Unit = {},
    onRequestRename: (ProjectEntity) -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onConfirmDuplicate: () -> Unit = {},
    onConfirmRename: (String) -> Unit = {},
    onCancelConfirmation: () -> Unit = {},
    onNewProject: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.testTag("projects_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.projects_title)) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.projects_back))
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewProject, modifier = Modifier.testTag("projects_new_project")) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.projects_create))
            }
        },
    ) { padding ->
        if (state.isLoading) {
            SquareToolLoadingIndicator(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        start = SquareToolSpacing.Standard,
                        end = SquareToolSpacing.Standard,
                        top = SquareToolSpacing.Small,
                        bottom = 96.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Standard),
            ) {
                item {
                    Text(
                        stringResource(R.string.projects_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.fillMaxWidth().testTag("projects_search"),
                        label = { Text(stringResource(R.string.projects_search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small),
                    ) {
                        FilterChip(
                            selected = state.sort == ProjectSort.RECENT,
                            onClick = { onSortChange(ProjectSort.RECENT) },
                            label = { Text(stringResource(R.string.projects_sort_recent)) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                        FilterChip(
                            selected = state.sort == ProjectSort.ALPHABETICAL,
                            onClick = { onSortChange(ProjectSort.ALPHABETICAL) },
                            label = { Text(stringResource(R.string.projects_sort_alphabetical)) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
                item {
                    FilterChip(
                        selected = state.favoriteOnly,
                        onClick = { onFavoriteOnlyChange(!state.favoriteOnly) },
                        label = { Text(stringResource(R.string.projects_favorites)) },
                        leadingIcon = {
                            Icon(
                                if (state.favoriteOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                if (state.notice != null) {
                    item {
                        Text(
                            stringResource(R.string.projects_save_failed),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                if (state.projects.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = SquareToolSpacing.ExtraLarge),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Standard),
                        ) {
                            Text(
                                stringResource(
                                    if (state.query.isBlank() && !state.favoriteOnly) {
                                        R.string.projects_empty
                                    } else {
                                        R.string.projects_no_results
                                    },
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Button(onClick = onNewProject, modifier = Modifier.heightIn(min = 56.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(SquareToolSpacing.Small))
                                Text(stringResource(R.string.projects_create))
                            }
                        }
                    }
                } else {
                    items(state.projects, key = { it.project.id }) { project ->
                        ProjectManagementCard(
                            model = project,
                            onOpen = { onOpenProject(project.project.id) },
                            onInsights = { onOpenInsights(project.project.id) },
                            onEdit = { onEditProject(project.project.id) },
                            onFavorite = { onToggleFavorite(project.project.id) },
                            onRename = { onRequestRename(project.project) },
                            onDuplicate = { onRequestDuplicate(project.project) },
                            onDelete = { onRequestDelete(project.project) },
                        )
                    }
                }
            }
        }
    }

    when (val confirmation = state.confirmation) {
        is ProjectConfirmation.Delete -> {
            AlertDialog(
                modifier = Modifier.testTag("project_delete_confirmation"),
                onDismissRequest = onCancelConfirmation,
                title = { Text(stringResource(R.string.projects_delete_title, confirmation.project.name)) },
                text = { Text(stringResource(R.string.projects_delete_body)) },
                confirmButton = {
                    TextButton(onClick = onConfirmDelete, modifier = Modifier.testTag("project_confirm_delete")) {
                        Text(stringResource(R.string.projects_delete_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onCancelConfirmation) { Text(stringResource(R.string.projects_cancel)) }
                },
            )
        }

        is ProjectConfirmation.Duplicate -> {
            AlertDialog(
                onDismissRequest = onCancelConfirmation,
                title = { Text(stringResource(R.string.projects_duplicate_title, confirmation.project.name)) },
                text = { Text(stringResource(R.string.projects_duplicate_body)) },
                confirmButton = {
                    TextButton(onClick = onConfirmDuplicate) { Text(stringResource(R.string.projects_duplicate_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = onCancelConfirmation) { Text(stringResource(R.string.projects_cancel)) }
                },
            )
        }

        is ProjectConfirmation.Rename -> {
            var name by remember(confirmation.project.id) { mutableStateOf(confirmation.project.name) }
            AlertDialog(
                onDismissRequest = onCancelConfirmation,
                title = { Text(stringResource(R.string.projects_rename_title, confirmation.project.name)) },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.projects_rename_name)) },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onConfirmRename(name) }, enabled = name.isNotBlank()) {
                        Text(stringResource(R.string.projects_rename_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onCancelConfirmation) { Text(stringResource(R.string.projects_cancel)) }
                },
            )
        }

        null -> {
            return
        }
    }
}

@Suppress("kotlin:S107", "kotlin:S3776") // Card actions remain explicit for accessibility and test semantics.
@Composable
private fun ProjectManagementCard(
    model: ProjectCardModel,
    onOpen: () -> Unit,
    onInsights: () -> Unit,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val project = model.project
    val editedDate =
        remember(project.updatedAt) {
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(project.updatedAt))
        }
    Card(Modifier.fillMaxWidth().testTag("project_card_${project.id}")) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(SquareToolSpacing.Standard),
            verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            project.name,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f).semantics { heading() },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (project.demoProject) {
                            Text(
                                stringResource(R.string.projects_sample),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.projects_last_edited, editedDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (project.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription =
                            stringResource(
                                if (project.favorite) R.string.projects_remove_favorite else R.string.projects_add_favorite,
                            ),
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.projects_actions, project.name))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.projects_edit)) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.projects_rename)) },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.projects_duplicate)) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.projects_delete)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        )
                    }
                }
            }
            ProjectBlanketPreview(
                project = model,
                contentDescription = project.name,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.projects_grid_size, project.columnCount, project.rowCount))
                Text(pluralStringResource(R.plurals.projects_square_count, model.totalSquares, model.totalSquares))
            }
            model.progress?.let { progress ->
                Column(verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.ExtraSmall)) {
                    Text(stringResource(R.string.projects_progress, progress.percentage))
                    LinearProgressIndicator(
                        progress = { progress.percentage / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium),
            ) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("project_open_${project.id}"),
                ) { Text(stringResource(R.string.projects_open)) }
                OutlinedButton(
                    onClick = onInsights,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("project_insights_${project.id}"),
                ) {
                    Icon(Icons.Default.Insights, contentDescription = null)
                    Spacer(Modifier.width(SquareToolSpacing.Small))
                    Text(stringResource(R.string.projects_insights))
                }
            }
        }
    }
}
