package com.finnvek.squaretool.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finnvek.squaretool.R
import com.finnvek.squaretool.data.repository.AppSettings
import com.finnvek.squaretool.ui.export.ExportProjectRoute
import com.finnvek.squaretool.ui.home.HomeRoute
import com.finnvek.squaretool.ui.insights.InsightsRoute
import com.finnvek.squaretool.ui.library.ColorEditorRoute
import com.finnvek.squaretool.ui.library.LibraryRoute
import com.finnvek.squaretool.ui.library.PaletteEditorRoute
import com.finnvek.squaretool.ui.navigation.SquareToolNavigationBar
import com.finnvek.squaretool.ui.navigation.SquareToolNavigationRail
import com.finnvek.squaretool.ui.navigation.TopLevelDestination
import com.finnvek.squaretool.ui.planner.PlannerRoute
import com.finnvek.squaretool.ui.projects.ProjectEditorRoute
import com.finnvek.squaretool.ui.projects.ProjectsRoute
import com.finnvek.squaretool.ui.settings.AboutScreen
import com.finnvek.squaretool.ui.settings.BackupRestoreScreen
import com.finnvek.squaretool.ui.settings.SettingsRoute
import com.finnvek.squaretool.ui.squares.SquareEditorRoute
import com.finnvek.squaretool.ui.squares.SquaresRoute
import kotlinx.coroutines.launch

@Suppress("kotlin:S3776") // The navigation host keeps route ownership and back-stack transitions in one registry.
@Composable
internal fun SquareToolNavigationHost(
    container: AppContainer,
    settings: AppSettings,
    initialActiveProjectId: String?,
    postOnboardingRoute: String?,
    onConsumePostOnboardingRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val currentOnConsumePostOnboardingRoute by rememberUpdatedState(onConsumePostOnboardingRoute)
    val projects by container.repository.observeProjects().collectAsStateWithLifecycle(
        initialValue = emptyList(),
    )
    var activeProjectId by rememberSaveable { mutableStateOf(initialActiveProjectId) }
    var pendingPlannerDesignId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentProjectId =
        remember(projects, activeProjectId) {
            projects.firstOrNull { it.id == activeProjectId }?.id
                ?: projects.maxByOrNull { it.lastOpenedAt }?.id
        }
    val initialDestination =
        AppRoute.topLevelDestination(
            settings.lastSelectedNavigationDestination,
        ) ?: TopLevelDestination.Home

    LaunchedEffect(currentProjectId) {
        if (activeProjectId != currentProjectId) activeProjectId = currentProjectId
    }
    LaunchedEffect(postOnboardingRoute) {
        if (postOnboardingRoute != null) {
            navController.navigate(postOnboardingRoute) { launchSingleTop = true }
            currentOnConsumePostOnboardingRoute()
        }
    }

    fun performTopLevelNavigation(destination: TopLevelDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        scope.launch {
            container.settingsRepository.setLastSelectedNavigationDestination(destination.route)
        }
    }

    fun navigateTopLevel(destination: TopLevelDestination) {
        if (destination == TopLevelDestination.Planner && currentProjectId == null) {
            navController.navigate(AppRoute.projectEditor()) { launchSingleTop = true }
        } else {
            performTopLevelNavigation(destination)
        }
    }

    fun openPlanner(projectId: String) {
        activeProjectId = projectId
        performTopLevelNavigation(TopLevelDestination.Planner)
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val navigationDestination =
        if (backStackEntry == null) {
            initialDestination
        } else {
            AppRoute.navigationDestination(backStackEntry?.destination?.route)
        }

    PrimaryNavigationScaffold(
        selected = navigationDestination,
        onSelect = ::navigateTopLevel,
        modifier = modifier,
    ) { contentModifier ->
        NavHost(
            navController = navController,
            startDestination = initialDestination.route,
            modifier = contentModifier,
        ) {
            composable(TopLevelDestination.Home.route) {
                HomeRoute(
                    repository = container.repository,
                    onOpenPlanner = ::openPlanner,
                    onOpenInsights = { projectId ->
                        activeProjectId = projectId
                        navController.navigate(AppRoute.insights(projectId))
                    },
                    onEditProject = { navController.navigate(AppRoute.projectEditor(it)) },
                    onViewAllProjects = { navController.navigate(AppRoute.Projects) },
                    onNewProject = { navController.navigate(AppRoute.projectEditor()) },
                    onSampleCreate = { projectId ->
                        activeProjectId = projectId
                        scope.launch {
                            container.settingsRepository.setSampleProjectOffered(true)
                            container.settingsRepository.setSampleProjectCreated(true)
                        }
                        openPlanner(projectId)
                    },
                )
            }
            composable(TopLevelDestination.Planner.route) {
                if (currentProjectId == null) {
                    EmptyPlanner(onCreateProject = { navController.navigate(AppRoute.projectEditor()) })
                } else {
                    PlannerRoute(
                        projectId = currentProjectId,
                        repository = container.repository,
                        settings = settings,
                        onBack = { navigateTopLevel(TopLevelDestination.Home) },
                        onOpenInsights = { navController.navigate(AppRoute.insights(currentProjectId)) },
                        onExport = { navController.navigate(AppRoute.export(currentProjectId)) },
                        onEditProject = { navController.navigate(AppRoute.projectEditor(currentProjectId)) },
                    )
                }
            }
            composable(TopLevelDestination.Squares.route) {
                SquaresRoute(
                    repository = container.repository,
                    onCreateDesign = { navController.navigate(AppRoute.squareEditor()) },
                    onEditDesign = { id, duplicate ->
                        navController.navigate(AppRoute.squareEditor(id, duplicate))
                    },
                    onUseInProject = { designId ->
                        if (currentProjectId == null) {
                            pendingPlannerDesignId = designId
                            navController.navigate(AppRoute.projectEditor())
                        } else {
                            activeProjectId = currentProjectId
                            navController.navigate(
                                AppRoute.plannerWithDesign(currentProjectId, designId),
                            )
                        }
                    },
                )
            }
            composable(TopLevelDestination.Library.route) {
                LibraryRoute(
                    repository = container.repository,
                    projectId = currentProjectId,
                    onCreateColor = { navController.navigate(AppRoute.colorEditor()) },
                    onEditColor = { id, duplicate ->
                        navController.navigate(AppRoute.colorEditor(id, duplicate))
                    },
                    onCreatePalette = {
                        navController.navigate(AppRoute.paletteEditor(projectId = currentProjectId))
                    },
                    onEditPalette = { id, duplicate ->
                        navController.navigate(
                            AppRoute.paletteEditor(id, duplicate, currentProjectId),
                        )
                    },
                )
            }
            composable(TopLevelDestination.Settings.route) {
                SettingsRoute(
                    repository = container.settingsRepository,
                    onOpenBackup = { navController.navigate(AppRoute.Backup) },
                    onOpenAbout = { navController.navigate(AppRoute.About) },
                    onOpenAccessiblePlanner = {
                        if (currentProjectId == null) {
                            navController.navigate(AppRoute.projectEditor())
                        } else {
                            navController.navigate(AppRoute.accessiblePlanner(currentProjectId))
                        }
                    },
                    onDeleteAllData = container::deleteAllData,
                )
            }
            composable(AppRoute.Projects) {
                ProjectsRoute(
                    repository = container.repository,
                    onBack = { navController.popBackStack() },
                    onOpenProject = ::openPlanner,
                    onOpenInsights = { projectId ->
                        activeProjectId = projectId
                        navController.navigate(AppRoute.insights(projectId))
                    },
                    onEditProject = { navController.navigate(AppRoute.projectEditor(it)) },
                    onNewProject = { navController.navigate(AppRoute.projectEditor()) },
                )
            }
            composable(
                route = AppRoute.ProjectEditorPattern,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            ) { entry ->
                val projectId = entry.arguments?.getString("projectId").takeUnless { it == "new" }
                ProjectEditorRoute(
                    repository = container.repository,
                    settings = settings,
                    projectId = projectId,
                    onClose = { navController.popBackStack() },
                    onSave = { savedProjectId ->
                        navController.popBackStack()
                        val designId = pendingPlannerDesignId
                        pendingPlannerDesignId = null
                        activeProjectId = savedProjectId
                        if (designId == null) {
                            openPlanner(savedProjectId)
                        } else {
                            navController.navigate(
                                AppRoute.plannerWithDesign(savedProjectId, designId),
                            )
                        }
                    },
                )
            }
            composable(
                route = AppRoute.InsightsPattern,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            ) { entry ->
                val projectId = requireNotNull(entry.arguments?.getString("projectId"))
                InsightsRoute(
                    repository = container.repository,
                    projectId = projectId,
                    onBack = { navController.popBackStack() },
                    onExportPdf = { navController.navigate(AppRoute.export(projectId)) },
                    onSaveImage = { navController.navigate(AppRoute.export(projectId)) },
                    onSharePdf = { navController.navigate(AppRoute.export(projectId)) },
                    onShareImage = { navController.navigate(AppRoute.export(projectId)) },
                    onExportBackup = { navController.navigate(AppRoute.export(projectId)) },
                )
            }
            composable(
                route = AppRoute.ExportPattern,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            ) { entry ->
                ExportProjectRoute(
                    projectId = requireNotNull(entry.arguments?.getString("projectId")),
                    container = container,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = AppRoute.AccessiblePlannerPattern,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            ) { entry ->
                val projectId = requireNotNull(entry.arguments?.getString("projectId"))
                // CPD-OFF
                PlannerRoute(
                    projectId = projectId,
                    repository = container.repository,
                    settings = settings,
                    startAccessible = true,
                    onBack = { navController.popBackStack() },
                    onOpenInsights = { navController.navigate(AppRoute.insights(projectId)) },
                    onExport = { navController.navigate(AppRoute.export(projectId)) },
                    onEditProject = { navController.navigate(AppRoute.projectEditor(projectId)) },
                )
                // CPD-ON
            }
            composable(
                route = AppRoute.PlannerDesignPattern,
                arguments =
                    listOf(
                        navArgument("projectId") { type = NavType.StringType },
                        navArgument("designId") { type = NavType.StringType },
                    ),
            ) { entry ->
                val projectId = requireNotNull(entry.arguments?.getString("projectId"))
                val designId = requireNotNull(entry.arguments?.getString("designId"))
                PlannerRoute(
                    projectId = projectId,
                    repository = container.repository,
                    settings = settings,
                    initialDesignId = designId,
                    onBack = { navController.popBackStack() },
                    onOpenInsights = { navController.navigate(AppRoute.insights(projectId)) },
                    onExport = { navController.navigate(AppRoute.export(projectId)) },
                    onEditProject = { navController.navigate(AppRoute.projectEditor(projectId)) },
                )
            }
            composable(
                route = AppRoute.SquareEditorPattern,
                arguments =
                    listOf(
                        navArgument("designId") { type = NavType.StringType },
                        navArgument("duplicate") { type = NavType.BoolType },
                    ),
            ) { entry ->
                SquareEditorRoute(
                    repository = container.repository,
                    designId = entry.arguments?.getString("designId").takeUnless { it == "new" },
                    duplicate = entry.arguments?.getBoolean("duplicate") ?: false,
                    onClose = { navController.popBackStack() },
                )
            }
            composable(
                route = AppRoute.ColorEditorPattern,
                arguments =
                    listOf(
                        navArgument("colorId") { type = NavType.StringType },
                        navArgument("duplicate") { type = NavType.BoolType },
                    ),
            ) { entry ->
                ColorEditorRoute(
                    repository = container.repository,
                    colorId = entry.arguments?.getString("colorId").takeUnless { it == "new" },
                    duplicate = entry.arguments?.getBoolean("duplicate") ?: false,
                    onClose = { navController.popBackStack() },
                )
            }
            composable(
                route = AppRoute.PaletteEditorPattern,
                arguments =
                    listOf(
                        navArgument("paletteId") { type = NavType.StringType },
                        navArgument("duplicate") { type = NavType.BoolType },
                        navArgument("projectId") { type = NavType.StringType },
                    ),
            ) { entry ->
                PaletteEditorRoute(
                    repository = container.repository,
                    paletteId = entry.arguments?.getString("paletteId").takeUnless { it == "new" },
                    duplicate = entry.arguments?.getBoolean("duplicate") ?: false,
                    projectId = entry.arguments?.getString("projectId").takeUnless { it == "none" },
                    onClose = { navController.popBackStack() },
                )
            }
            composable(AppRoute.Backup) {
                BackupRestoreScreen(
                    service = container.backupService,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppRoute.About) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun PrimaryNavigationScaffold(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useRail && selected != null) {
                    SquareToolNavigationBar(selected = selected, onSelect = onSelect)
                }
            },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (useRail && selected != null) {
                    SquareToolNavigationRail(selected = selected, onSelect = onSelect)
                }
                content(Modifier.weight(1f).fillMaxSize())
            }
        }
    }
}

@Composable
private fun EmptyPlanner(onCreateProject: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.GridView,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.no_projects_yet),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = stringResource(R.string.create_project_to_open_planner),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Button(onClick = onCreateProject) { Text(stringResource(R.string.create_project)) }
    }
}
