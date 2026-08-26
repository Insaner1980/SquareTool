package com.finnvek.squaretool.ui.planner

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finnvek.squaretool.R
import com.finnvek.squaretool.data.repository.AppSettings
import com.finnvek.squaretool.data.repository.SquareToolRepository
import com.finnvek.squaretool.domain.model.CellCoordinate
import com.finnvek.squaretool.render.MotifRenderConfig
import com.finnvek.squaretool.render.MotifRenderDetail
import com.finnvek.squaretool.render.MotifRenderPlan
import com.finnvek.squaretool.render.MotifRenderer
import com.finnvek.squaretool.render.MotifSurface
import com.finnvek.squaretool.render.SquareDesignVisual
import com.finnvek.squaretool.ui.theme.SquareToolSpacing
import java.util.Locale
import kotlin.math.roundToInt

@Suppress("kotlin:S107") // Route callbacks expose planner operations as independently typed actions.
@Composable
fun PlannerRoute(
    projectId: String,
    repository: SquareToolRepository,
    settings: AppSettings,
    onBack: () -> Unit,
    onOpenInsights: () -> Unit,
    onExport: () -> Unit,
    onEditProject: () -> Unit,
    modifier: Modifier = Modifier,
    startAccessible: Boolean = false,
    initialDesignId: String? = null,
    plannerViewModel: PlannerViewModel =
        viewModel(
            key = "planner-$projectId",
            factory = PlannerViewModel.factory(repository, projectId),
        ),
) {
    val hapticFeedback = LocalHapticFeedback.current
    val signalEdit: () -> Unit = {
        if (settings.hapticsEnabled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val state by plannerViewModel.state.collectAsStateWithLifecycle()
    val settingsPolicy =
        remember(
            settings.showPlannerGridLines,
            settings.showLockMarkers,
            settings.preserveCompletedCells,
            settings.confirmDestructiveLayoutGeneration,
        ) { plannerSettingsPolicy(settings) }
    LaunchedEffect(projectId, startAccessible) {
        if (startAccessible) plannerViewModel.setAccessibleGridMode(true)
    }
    LaunchedEffect(settingsPolicy.defaultOverwriteCompleted) {
        plannerViewModel.setDefaultOverwriteCompleted(settingsPolicy.defaultOverwriteCompleted)
    }
    LaunchedEffect(projectId, initialDesignId) {
        if (initialDesignId != null) plannerViewModel.selectDesign(initialDesignId)
    }
    PlannerScreen(
        state = state,
        settingsPolicy = settingsPolicy,
        onBack = onBack,
        onOpenInsights = onOpenInsights,
        onExport = onExport,
        onEditProject = onEditProject,
        onSelectCell = plannerViewModel::selectCell,
        onAssignSelectedDesign = { designId ->
            plannerViewModel.assignDesignToSelected(designId)
            signalEdit()
        },
        onSetTool = plannerViewModel::setTool,
        onBeginToolDrag = plannerViewModel::beginToolDrag,
        onApplyToolDuringDrag = { coordinate ->
            plannerViewModel.applyToolDuringDrag(coordinate)
            signalEdit()
        },
        onEndToolDrag = plannerViewModel::endToolDrag,
        onCancelToolDrag = plannerViewModel::cancelToolDrag,
        onToggleLock = {
            plannerViewModel.toggleSelectedLock()
            signalEdit()
        },
        onToggleCompletion = {
            plannerViewModel.toggleSelectedCompletion()
            signalEdit()
        },
        onClearSelection = plannerViewModel::clearSelectedCell,
        onClearUnlockedCells = plannerViewModel::clearUnlockedCells,
        onUndo = plannerViewModel::undo,
        onRedo = plannerViewModel::redo,
        onSetAccessibleMode = plannerViewModel::setAccessibleGridMode,
        onOpenGenerator = plannerViewModel::openGenerator,
        onCloseGenerator = plannerViewModel::closeGenerator,
        onSetGeneratorMode = plannerViewModel::setGeneratorMode,
        onSetGeneratorSeed = plannerViewModel::setGeneratorSeed,
        onSetBandWidth = plannerViewModel::setGeneratorBandWidth,
        onSetAvoidNeighbors = plannerViewModel::setAvoidNeighbors,
        onSetOverwriteCompletedCells = plannerViewModel::setOverwriteCompleted,
        onToggleGeneratorDesign = plannerViewModel::toggleGeneratorDesign,
        onAdjustGeneratorWeight = plannerViewModel::adjustGeneratorWeight,
        onMoveGeneratorDesign = plannerViewModel::moveGeneratorDesign,
        onGenerate = plannerViewModel::generateLayout,
        onRegenerate = plannerViewModel::regenerateLayout,
        onApplyGeneratedLayout = plannerViewModel::applyGeneratedLayout,
        modifier = modifier,
    )
}

@Suppress("kotlin:S107", "kotlin:S3776") // Planner state and actions stay explicit across mutually exclusive UI branches.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    state: PlannerUiState,
    settingsPolicy: PlannerSettingsPolicy,
    onBack: () -> Unit,
    onOpenInsights: () -> Unit,
    onExport: () -> Unit,
    onEditProject: () -> Unit,
    onSelectCell: (CellCoordinate) -> Unit,
    onAssignSelectedDesign: (String?) -> Unit,
    onSetTool: (PlannerTool) -> Unit,
    onBeginToolDrag: (PlannerTool) -> Unit,
    onApplyToolDuringDrag: (CellCoordinate) -> Unit,
    onEndToolDrag: () -> Unit,
    onCancelToolDrag: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleCompletion: () -> Unit,
    onClearSelection: () -> Unit,
    onClearUnlockedCells: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSetAccessibleMode: (Boolean) -> Unit,
    onOpenGenerator: () -> Unit,
    onCloseGenerator: () -> Unit,
    onSetGeneratorMode: (PlannerGeneratorMode) -> Unit,
    onSetGeneratorSeed: (String) -> Unit,
    onSetBandWidth: (Int) -> Unit,
    onSetAvoidNeighbors: (Boolean) -> Unit,
    onSetOverwriteCompletedCells: (Boolean) -> Unit,
    onToggleGeneratorDesign: (String) -> Unit,
    onAdjustGeneratorWeight: (String, Double) -> Unit,
    onMoveGeneratorDesign: (String, Int) -> Unit,
    onGenerate: () -> Unit,
    onRegenerate: () -> Unit,
    onApplyGeneratedLayout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var overflowOpen by remember { mutableStateOf(false) }
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }
    var showGenerationConfirmation by rememberSaveable { mutableStateOf(false) }
    var canvasCommand by remember { mutableStateOf<PlannerViewportCommand?>(null) }
    var canvasCommandId by remember { mutableIntStateOf(0) }
    var canvasScale by remember { mutableFloatStateOf(1f) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("planner_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.planner_title),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        if (state.projectName.isNotBlank()) {
                            Text(
                                text = state.projectName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.planner_back),
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { overflowOpen = true },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.planner_more_actions),
                            )
                        }
                        PlannerOverflowMenu(
                            expanded = overflowOpen,
                            accessibleGridMode = state.accessibleGridMode,
                            onDismiss = { overflowOpen = false },
                            onEditProject = onEditProject,
                            onOpenInsights = onOpenInsights,
                            onExport = onExport,
                            onSetAccessibleMode = onSetAccessibleMode,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                LoadingPlanner(Modifier.padding(innerPadding))
            }

            state.projectMissing -> {
                MissingProject(Modifier.padding(innerPadding))
            }

            else -> {
                Column(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .padding(horizontal = SquareToolSpacing.Standard),
                    verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small),
                ) {
                    PlannerSummaryCard(state)

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .heightIn(min = 220.dp),
                    ) {
                        if (state.accessibleGridMode) {
                            AccessiblePlannerGrid(
                                rows = state.rows,
                                columns = state.columns,
                                cells = state.cells,
                                selectedCoordinate = state.selectedCoordinate,
                                showLockMarkers = settingsPolicy.showLockMarkers,
                                showCompletionMarkers = state.trackingEnabled,
                                onSelectCell = onSelectCell,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            PlannerCanvas(
                                rows = state.rows,
                                columns = state.columns,
                                cells = state.cells,
                                selectedCoordinate = state.selectedCoordinate,
                                tool = state.tool,
                                command = canvasCommand,
                                showGridLines = settingsPolicy.showGridLines,
                                showLockMarkers = settingsPolicy.showLockMarkers,
                                showCompletionMarkers = state.trackingEnabled,
                                onScaleChange = { canvasScale = it },
                                onSelectCell = onSelectCell,
                                onBeginToolDrag = onBeginToolDrag,
                                onApplyToolDuringDrag = onApplyToolDuringDrag,
                                onEndToolDrag = onEndToolDrag,
                                onCancelToolDrag = onCancelToolDrag,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    PlannerToolbars(
                        state = state,
                        scale = canvasScale,
                        canvasEnabled = !state.accessibleGridMode,
                        onSetTool = onSetTool,
                        onOpenGenerator = onOpenGenerator,
                        onToggleLock = onToggleLock,
                        onToggleCompletion = onToggleCompletion,
                        onUndo = onUndo,
                        onRedo = onRedo,
                        onZoomOut = {
                            canvasCommand = PlannerViewportCommand.Zoom(++canvasCommandId, 0.8f)
                        },
                        onZoomIn = {
                            canvasCommand = PlannerViewportCommand.Zoom(++canvasCommandId, 1.25f)
                        },
                        onFit = {
                            canvasCommand = PlannerViewportCommand.Fit(++canvasCommandId)
                        },
                        onClearUnlockedCells = { showClearConfirmation = true },
                    )

                    PlannerInspector(
                        state = state,
                        onAssignDesign = onAssignSelectedDesign,
                        onToggleLock = onToggleLock,
                        onToggleCompletion = onToggleCompletion,
                        onClearSelection = onClearSelection,
                    )

                    SaveStatus(state)
                    Spacer(Modifier.height(SquareToolSpacing.ExtraSmall))
                }
            }
        }
    }

    if (state.generator.isOpen) {
        PlannerGeneratorSheet(
            state = state,
            showGridLines = settingsPolicy.showGridLines,
            showLockMarkers = settingsPolicy.showLockMarkers,
            onDismiss = onCloseGenerator,
            onSetMode = onSetGeneratorMode,
            onSetSeed = onSetGeneratorSeed,
            onSetBandWidth = onSetBandWidth,
            onSetAvoidNeighbors = onSetAvoidNeighbors,
            onSetOverwriteCompletedCells = onSetOverwriteCompletedCells,
            onToggleDesign = onToggleGeneratorDesign,
            onAdjustWeight = onAdjustGeneratorWeight,
            onMoveDesign = onMoveGeneratorDesign,
            onGenerate = onGenerate,
            onRegenerate = onRegenerate,
            onApply = {
                val before = state.snapshot
                val after = state.layoutGenerationResult?.snapshot
                if (
                    before != null && after != null &&
                    requiresGenerationConfirmation(
                        confirmationEnabled = settingsPolicy.confirmDestructiveGeneration,
                        before = before,
                        after = after,
                    )
                ) {
                    showGenerationConfirmation = true
                } else {
                    onApplyGeneratedLayout()
                }
            },
        )
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(stringResource(R.string.planner_confirm_clear_title)) },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.planner_confirm_clear_message,
                        state.clearableCount,
                        state.clearableCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = state.clearableCount > 0,
                    onClick = {
                        showClearConfirmation = false
                        onClearUnlockedCells()
                    },
                ) {
                    Text(stringResource(R.string.planner_confirm_clear_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text(stringResource(R.string.planner_cancel))
                }
            },
        )
    }

    if (showGenerationConfirmation) {
        AlertDialog(
            onDismissRequest = { showGenerationConfirmation = false },
            title = { Text(stringResource(R.string.planner_confirm_generation_title)) },
            text = { Text(stringResource(R.string.planner_confirm_generation_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGenerationConfirmation = false
                        onApplyGeneratedLayout()
                    },
                ) {
                    Text(stringResource(R.string.planner_confirm_generation_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerationConfirmation = false }) {
                    Text(stringResource(R.string.planner_cancel))
                }
            },
        )
    }
}

@Composable
private fun LoadingPlanner(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(SquareToolSpacing.Standard))
            Text(stringResource(R.string.planner_loading))
        }
    }
}

@Composable
private fun MissingProject(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.planner_project_not_found),
            modifier = Modifier.padding(SquareToolSpacing.Section),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun PlannerSummaryCard(state: PlannerUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(SquareToolSpacing.Standard),
            verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GridOn, contentDescription = null)
                    Spacer(Modifier.width(SquareToolSpacing.Small))
                    Text(
                        stringResource(R.string.planner_grid_dimensions, state.rows, state.columns),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    if (state.trackingEnabled) {
                        stringResource(R.string.planner_progress_percent, state.completionPercent)
                    } else {
                        stringResource(R.string.planner_progress_disabled)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (state.trackingEnabled) {
                LinearProgressIndicator(
                    progress = { state.completionPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Standard),
            ) {
                Text(
                    stringResource(R.string.planner_assigned_summary, state.assignedCount, state.totalCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    pluralStringResource(
                        R.plurals.planner_locked_summary,
                        state.lockedCount,
                        state.lockedCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (state.trackingEnabled) {
                    Text(
                        pluralStringResource(
                            R.plurals.planner_completed_summary,
                            state.completedCount,
                            state.completedCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Suppress("kotlin:S107", "kotlin:S3776") // Toolbars expose independent planner tools and contextual controls.
@Composable
private fun ColumnScope.PlannerToolbars(
    state: PlannerUiState,
    scale: Float,
    canvasEnabled: Boolean,
    onSetTool: (PlannerTool) -> Unit,
    onOpenGenerator: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleCompletion: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onFit: () -> Unit,
    onClearUnlockedCells: () -> Unit,
) {
    val selected = state.selectedCell
    LazyRow(
        modifier = Modifier.fillMaxWidth().testTag("planner_primary_tools"),
        horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small),
        contentPadding = PaddingValues(horizontal = SquareToolSpacing.ExtraSmall),
    ) {
        item {
            PlannerToolChip(
                selected = false,
                label = stringResource(R.string.planner_generate),
                icon = { Icon(Icons.Default.AutoAwesome, null) },
                onClick = onOpenGenerator,
                testTag = "planner_generate",
            )
        }
        if (canvasEnabled) {
            item {
                PlannerToolChip(
                    selected = state.tool == PlannerTool.SELECT,
                    label = stringResource(R.string.planner_select_tool),
                    icon = { Icon(Icons.Default.SelectAll, null) },
                    onClick = { onSetTool(PlannerTool.SELECT) },
                    testTag = "planner_select_tool",
                )
            }
            item {
                PlannerToolChip(
                    selected = state.tool == PlannerTool.PAINT,
                    label = stringResource(R.string.planner_paint_tool),
                    icon = { Icon(Icons.Default.Edit, null) },
                    onClick = { onSetTool(PlannerTool.PAINT) },
                    testTag = "planner_paint_tool",
                )
            }
            item {
                PlannerToolChip(
                    selected = state.tool == PlannerTool.LOCK,
                    label = stringResource(R.string.planner_lock_tool),
                    icon = { Icon(Icons.Default.Lock, null) },
                    onClick = { onSetTool(PlannerTool.LOCK) },
                    testTag = "planner_lock_tool",
                )
            }
            if (state.trackingEnabled) {
                item {
                    PlannerToolChip(
                        selected = state.tool == PlannerTool.PROGRESS,
                        label = stringResource(R.string.planner_progress_tool),
                        icon = { Icon(Icons.Default.CheckCircle, null) },
                        onClick = { onSetTool(PlannerTool.PROGRESS) },
                        testTag = "planner_progress_tool",
                    )
                }
            }
        }
        item {
            PlannerToolChip(
                selected = selected?.locked == true,
                enabled = selected != null,
                label =
                    stringResource(
                        if (selected?.locked == true) R.string.planner_unlock else R.string.planner_lock,
                    ),
                icon = {
                    Icon(
                        if (selected?.locked == true) Icons.Default.LockOpen else Icons.Default.Lock,
                        null,
                    )
                },
                onClick = onToggleLock,
                testTag = "planner_lock",
            )
        }
        if (state.trackingEnabled) {
            item {
                PlannerToolChip(
                    selected = selected?.completed == true,
                    enabled = selected != null,
                    label =
                        stringResource(
                            if (selected?.completed == true) {
                                R.string.planner_mark_not_completed
                            } else {
                                R.string.planner_mark_completed
                            },
                        ),
                    icon = { Icon(Icons.Default.CheckCircle, null) },
                    onClick = onToggleCompletion,
                    testTag = "planner_completion",
                )
            }
        }
        item {
            PlannerToolChip(
                selected = false,
                enabled = state.clearableCount > 0,
                label = stringResource(R.string.planner_clear_unlocked),
                icon = { Icon(Icons.Default.ClearAll, null) },
                onClick = onClearUnlockedCells,
                testTag = "planner_clear_unlocked",
            )
        }
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth().testTag("planner_history_zoom_tools"),
        horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small),
        contentPadding = PaddingValues(horizontal = SquareToolSpacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            OutlinedButton(
                enabled = state.canUndo,
                onClick = onUndo,
                modifier = Modifier.heightIn(min = 48.dp).testTag("planner_undo"),
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, null)
                Spacer(Modifier.width(SquareToolSpacing.Small))
                Text(stringResource(R.string.planner_undo))
            }
        }
        item {
            IconButton(
                enabled = state.canRedo,
                onClick = onRedo,
                modifier = Modifier.size(48.dp).testTag("planner_redo"),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = stringResource(R.string.planner_redo),
                )
            }
        }
        if (canvasEnabled) {
            item {
                IconButton(onClick = onZoomOut, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.ZoomOut, stringResource(R.string.planner_zoom_out))
                }
            }
            item {
                Text(
                    stringResource(R.string.planner_zoom_percent, (scale * 100).roundToInt()),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            item {
                IconButton(onClick = onZoomIn, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.ZoomIn, stringResource(R.string.planner_zoom_in))
                }
            }
            item {
                OutlinedButton(onClick = onFit, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Default.FitScreen, null)
                    Spacer(Modifier.width(SquareToolSpacing.Small))
                    Text(stringResource(R.string.planner_fit_to_screen))
                }
            }
        }
    }
}

@Composable
private fun PlannerToolChip(
    selected: Boolean,
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    testTag: String,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = icon,
        modifier = Modifier.heightIn(min = 48.dp).testTag(testTag),
    )
}

@Suppress("kotlin:S3776") // Inspector branches mirror the selected cell and project configuration.
@Composable
private fun PlannerInspector(
    state: PlannerUiState,
    onAssignDesign: (String?) -> Unit,
    onToggleLock: () -> Unit,
    onToggleCompletion: () -> Unit,
    onClearSelection: () -> Unit,
) {
    val cell = state.selectedCell ?: return
    Card(
        modifier = Modifier.fillMaxWidth().testTag("planner_inspector"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(SquareToolSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.planner_cell_inspector),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(
                            R.string.planner_cell_location,
                            cell.coordinate.row + 1,
                            cell.coordinate.column + 1,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                AssistChip(
                    onClick = onToggleLock,
                    label = {
                        Text(stringResource(if (cell.locked) R.string.planner_unlock else R.string.planner_lock))
                    },
                    leadingIcon = {
                        Icon(if (cell.locked) Icons.Default.LockOpen else Icons.Default.Lock, null)
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
                if (state.trackingEnabled) {
                    Spacer(Modifier.width(SquareToolSpacing.Small))
                    IconButton(onClick = onToggleCompletion, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription =
                                stringResource(
                                    if (cell.completed) {
                                        R.string.planner_mark_not_completed
                                    } else {
                                        R.string.planner_mark_completed
                                    },
                                ),
                            tint =
                                if (cell.completed) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.planner_choose_design),
                style = MaterialTheme.typography.labelLarge,
            )
            if (state.designs.isEmpty()) {
                Text(
                    stringResource(R.string.planner_no_designs),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small)) {
                    item {
                        FilterChip(
                            selected = cell.designId == null,
                            enabled = !cell.locked,
                            onClick = { onAssignDesign(null) },
                            label = { Text(stringResource(R.string.planner_cell_blank)) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                    items(state.designs, key = PlannerDesignOption::id) { design ->
                        FilterChip(
                            selected = cell.designId == design.id,
                            enabled = !cell.locked,
                            onClick = { onAssignDesign(design.id) },
                            label = { Text(design.name, maxLines = 1) },
                            leadingIcon = {
                                PlannerMotifPreview(
                                    visual = design.visual,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }
            TextButton(
                enabled = !cell.locked && (cell.designId != null || cell.completed),
                onClick = onClearSelection,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.planner_clear_cell))
            }
        }
    }
}

@Composable
private fun SaveStatus(state: PlannerUiState) {
    val text =
        when {
            state.saveFailed -> stringResource(R.string.planner_save_failed)
            state.isSaving -> stringResource(R.string.planner_saving)
            else -> null
        }
    if (text != null) {
        Text(
            text = text,
            color = if (state.saveFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { contentDescription = text },
        )
    }
}

@Composable
private fun PlannerOverflowMenu(
    expanded: Boolean,
    accessibleGridMode: Boolean,
    onDismiss: () -> Unit,
    onEditProject: () -> Unit,
    onOpenInsights: () -> Unit,
    onExport: () -> Unit,
    onSetAccessibleMode: (Boolean) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.planner_edit_project)) },
            leadingIcon = { Icon(Icons.Default.Edit, null) },
            onClick = {
                onDismiss()
                onEditProject()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.planner_open_insights)) },
            leadingIcon = { Icon(Icons.Default.Insights, null) },
            onClick = {
                onDismiss()
                onOpenInsights()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.planner_export)) },
            leadingIcon = { Icon(Icons.Default.FileDownload, null) },
            onClick = {
                onDismiss()
                onExport()
            },
        )
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(
                        if (accessibleGridMode) {
                            R.string.planner_visual_canvas_mode
                        } else {
                            R.string.planner_accessible_grid_mode
                        },
                    ),
                )
            },
            leadingIcon = { Icon(Icons.Default.AccessibilityNew, null) },
            onClick = {
                onDismiss()
                onSetAccessibleMode(!accessibleGridMode)
            },
        )
    }
}

@Suppress("kotlin:S107") // Grid semantics require explicit dimensions, selection, and action callbacks.
@Composable
fun AccessiblePlannerGrid(
    rows: Int,
    columns: Int,
    cells: List<PlannerUiCell>,
    selectedCoordinate: CellCoordinate?,
    onSelectCell: (CellCoordinate) -> Unit,
    modifier: Modifier = Modifier,
    showLockMarkers: Boolean = true,
    showCompletionMarkers: Boolean = true,
) {
    val blank = stringResource(R.string.planner_cell_blank)
    val locked = stringResource(R.string.planner_cell_locked)
    val unlocked = stringResource(R.string.planner_cell_unlocked)
    val completed = stringResource(R.string.planner_cell_completed)
    val notCompleted = stringResource(R.string.planner_cell_not_completed)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(72.dp),
        modifier = modifier.testTag("planner_accessible_grid"),
        contentPadding = PaddingValues(SquareToolSpacing.Small),
        horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small),
    ) {
        items(
            items = cells.take((rows * columns).coerceAtLeast(0)),
            key = { cell -> cell.coordinate.row * columns.coerceAtLeast(1) + cell.coordinate.column },
        ) { cell ->
            val description =
                stringResource(
                    R.string.planner_cell_description,
                    cell.coordinate.row + 1,
                    cell.coordinate.column + 1,
                    cell.designName ?: blank,
                    if (cell.locked) locked else unlocked,
                    if (cell.completed && showCompletionMarkers) completed else notCompleted,
                )
            Surface(
                selected = selectedCoordinate == cell.coordinate,
                onClick = { onSelectCell(cell.coordinate) },
                shape = RoundedCornerShape(12.dp),
                tonalElevation = if (selectedCoordinate == cell.coordinate) 6.dp else 1.dp,
                modifier =
                    Modifier
                        .sizeIn(minWidth = 64.dp, minHeight = 64.dp)
                        .aspectRatio(1f)
                        .semantics(mergeDescendants = true) {
                            contentDescription = description
                            selected = selectedCoordinate == cell.coordinate
                            role = Role.Button
                        }.testTag("planner_cell_${cell.coordinate.row}_${cell.coordinate.column}"),
            ) {
                Box(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
                    PlannerMotifPreview(
                        visual = cell.visual,
                        selected = selectedCoordinate == cell.coordinate,
                        locked = cell.locked && showLockMarkers,
                        completed = cell.completed && showCompletionMarkers,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

private sealed interface PlannerViewportCommand {
    val id: Int

    data class Zoom(
        override val id: Int,
        val factor: Float,
    ) : PlannerViewportCommand

    data class Fit(
        override val id: Int,
    ) : PlannerViewportCommand
}

private data class PlannerRenderKey(
    val visual: SquareDesignVisual,
    val selected: Boolean,
    val locked: Boolean,
    val completed: Boolean,
    val surface: MotifSurface,
)

private data class PlannerRenderPlans(
    val small: MotifRenderPlan,
    val full: MotifRenderPlan,
) {
    fun forDetail(detail: MotifRenderDetail): MotifRenderPlan = if (detail == MotifRenderDetail.SMALL) small else full
}

@Suppress("kotlin:S107", "kotlin:S3776") // Canvas input and rendering parameters form one typed interaction contract.
@Composable
private fun PlannerCanvas(
    rows: Int,
    columns: Int,
    cells: List<PlannerUiCell>,
    selectedCoordinate: CellCoordinate?,
    tool: PlannerTool,
    command: PlannerViewportCommand?,
    showGridLines: Boolean,
    showLockMarkers: Boolean,
    showCompletionMarkers: Boolean,
    onScaleChange: (Float) -> Unit,
    onSelectCell: (CellCoordinate) -> Unit,
    onBeginToolDrag: (PlannerTool) -> Unit,
    onApplyToolDuringDrag: (CellCoordinate) -> Unit,
    onEndToolDrag: () -> Unit,
    onCancelToolDrag: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    semanticDescription: String? = null,
    testTag: String = "planner_canvas",
) {
    val currentOnScaleChange by rememberUpdatedState(onScaleChange)
    var viewport by remember(rows, columns) { mutableStateOf<PlannerViewport?>(null) }
    var canvasSize by remember(rows, columns) { mutableStateOf(IntSize.Zero) }
    val background = MaterialTheme.colorScheme.surfaceVariant
    val cellSurface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline
    val selection = MaterialTheme.colorScheme.secondary
    val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.35f
    val motifSurface = if (darkSurface) MotifSurface.DARK else MotifSurface.LIGHT
    val canvasDescription = semanticDescription ?: stringResource(R.string.planner_canvas_description)
    val renderPlans =
        remember(
            cells,
            selectedCoordinate,
            motifSurface,
            showLockMarkers,
            showCompletionMarkers,
        ) {
            val cache = mutableMapOf<PlannerRenderKey, PlannerRenderPlans>()
            cells.map { cell ->
                val visual = cell.visual ?: return@map null
                val key =
                    PlannerRenderKey(
                        visual = visual,
                        selected = cell.coordinate == selectedCoordinate,
                        locked = cell.locked && showLockMarkers,
                        completed = cell.completed && showCompletionMarkers,
                        surface = motifSurface,
                    )
                cache.getOrPut(key) {
                    val config =
                        MotifRenderConfig(
                            surface = motifSurface,
                            selected = key.selected,
                            locked = key.locked,
                            completed = key.completed,
                        )
                    PlannerRenderPlans(
                        small =
                            MotifRenderer.createPlan(
                                visual,
                                config.copy(detail = MotifRenderDetail.SMALL),
                            ),
                        full =
                            MotifRenderer.createPlan(
                                visual,
                                config.copy(detail = MotifRenderDetail.FULL),
                            ),
                    )
                }
            }
        }

    LaunchedEffect(command?.id, canvasSize) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0 || rows <= 0 || columns <= 0) return@LaunchedEffect
        viewport =
            when (val requested = command) {
                is PlannerViewportCommand.Zoom -> {
                    val current =
                        viewport ?: PlannerViewport.fit(
                            canvasSize.width.toFloat(),
                            canvasSize.height.toFloat(),
                            rows,
                            columns,
                        )
                    current.zoomAround(
                        anchorX = canvasSize.width / 2f,
                        anchorY = canvasSize.height / 2f,
                        zoomFactor = requested.factor,
                        minScale = MIN_CANVAS_SCALE,
                        maxScale = MAX_CANVAS_SCALE,
                    )
                }

                is PlannerViewportCommand.Fit, null -> {
                    PlannerViewport.fit(
                        canvasSize.width.toFloat(),
                        canvasSize.height.toFloat(),
                        rows,
                        columns,
                    )
                }
            }
        currentOnScaleChange(viewport?.scale ?: 1f)
    }

    Box(modifier = modifier) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(background, RoundedCornerShape(16.dp))
                    .onSizeChanged { size ->
                        if (size != canvasSize && size.width > 0 && size.height > 0 && rows > 0 && columns > 0) {
                            canvasSize = size
                            viewport = PlannerViewport.fit(size.width.toFloat(), size.height.toFloat(), rows, columns)
                            currentOnScaleChange(1f)
                        }
                    }.semantics { contentDescription = canvasDescription }
                    .testTag(testTag)
                    .pointerInput(interactive, tool, rows, columns) {
                        if (!interactive) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var editing = tool != PlannerTool.SELECT
                            var transformed = false
                            var panned = false
                            val startPosition = down.position
                            if (editing) {
                                onBeginToolDrag(tool)
                                viewport
                                    ?.hitTest(startPosition.x, startPosition.y, rows, columns)
                                    ?.let { onApplyToolDuringDrag(CellCoordinate(it.row, it.column)) }
                            }
                            var event = awaitPointerEvent()
                            while (event.changes.any { it.pressed }) {
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.size >= 2) {
                                    if (editing) {
                                        onCancelToolDrag()
                                        editing = false
                                    }
                                    transformed = true
                                    val current = viewport
                                    if (current != null) {
                                        val centroid = event.calculateCentroid(useCurrent = true)
                                        val zoom = event.calculateZoom()
                                        val pan = event.calculatePan()
                                        viewport =
                                            current
                                                .zoomAround(
                                                    centroid.x,
                                                    centroid.y,
                                                    zoom,
                                                    MIN_CANVAS_SCALE,
                                                    MAX_CANVAS_SCALE,
                                                ).panBy(pan.x, pan.y)
                                        currentOnScaleChange(viewport?.scale ?: 1f)
                                    }
                                    event.changes.forEach { it.consume() }
                                } else if (pressed.size == 1) {
                                    val change = pressed.first()
                                    if (editing) {
                                        viewport
                                            ?.hitTest(change.position.x, change.position.y, rows, columns)
                                            ?.let { onApplyToolDuringDrag(CellCoordinate(it.row, it.column)) }
                                        change.consume()
                                    } else if (!transformed) {
                                        val delta = change.position - change.previousPosition
                                        if (delta.getDistance() > 0f) {
                                            panned = panned || (change.position - startPosition).getDistance() > 8.dp.toPx()
                                            viewport = viewport?.panBy(delta.x, delta.y)
                                            change.consume()
                                        }
                                    }
                                }
                                event = awaitPointerEvent()
                            }
                            if (editing) {
                                onEndToolDrag()
                            } else if (!transformed && !panned) {
                                viewport
                                    ?.hitTest(startPosition.x, startPosition.y, rows, columns)
                                    ?.let { onSelectCell(CellCoordinate(it.row, it.column)) }
                            }
                        }
                    },
        ) {
            val current = viewport ?: return@Canvas
            val renderedCellSize = current.cellSize * current.scale
            val motifDetail =
                resolvePlannerMotifDetail(
                    renderedCellSizePx = renderedCellSize,
                    smallPreviewThresholdPx = 24.dp.toPx(),
                )
            val reusableRect = RectF()
            cells.forEachIndexed { index, cell ->
                val left = current.offsetX + cell.coordinate.column * renderedCellSize
                val top = current.offsetY + cell.coordinate.row * renderedCellSize
                val right = left + renderedCellSize
                val bottom = top + renderedCellSize
                if (right < 0f || bottom < 0f || left > size.width || top > size.height) return@forEachIndexed
                val inset = (renderedCellSize * 0.025f).coerceAtMost(2.dp.toPx())
                reusableRect.set(left + inset, top + inset, right - inset, bottom - inset)
                val plan = renderPlans.getOrNull(index)?.forDetail(motifDetail)
                if (plan != null && reusableRect.width() > 0f && reusableRect.height() > 0f) {
                    MotifRenderer.drawPlan(drawContext.canvas.nativeCanvas, reusableRect, plan)
                } else {
                    drawRect(
                        color = cellSurface,
                        topLeft = Offset(reusableRect.left, reusableRect.top),
                        size = Size(reusableRect.width(), reusableRect.height()),
                    )
                    drawBlankCellOverlays(
                        left = reusableRect.left,
                        top = reusableRect.top,
                        side = reusableRect.width(),
                        selected = cell.coordinate == selectedCoordinate,
                        locked = cell.locked && showLockMarkers,
                        completed = cell.completed && showCompletionMarkers,
                        selectionColor = selection,
                    )
                }
                if (showGridLines) {
                    drawRect(
                        color = outline.copy(alpha = 0.45f),
                        topLeft = Offset(left, top),
                        size = Size(renderedCellSize, renderedCellSize),
                        style = Stroke(width = (1.dp.toPx() / current.scale).coerceAtLeast(0.5f)),
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlankCellOverlays(
    left: Float,
    top: Float,
    side: Float,
    selected: Boolean,
    locked: Boolean,
    completed: Boolean,
    selectionColor: Color,
) {
    if (selected) {
        drawRect(
            color = selectionColor,
            topLeft = Offset(left, top),
            size = Size(side, side),
            style = Stroke(width = (side * 0.045f).coerceAtLeast(2f)),
        )
    }
    if (locked) {
        drawCircle(
            color = Color(0xE63A4020),
            radius = side * 0.12f,
            center = Offset(left + side * 0.8f, top + side * 0.2f),
        )
    }
    if (completed) {
        val center = Offset(left + side * 0.8f, top + side * 0.8f)
        drawCircle(color = Color(0xFF6B8A2E), radius = side * 0.12f, center = center)
        drawLine(
            color = Color.White,
            start = Offset(center.x - side * 0.055f, center.y),
            end = Offset(center.x - side * 0.01f, center.y + side * 0.04f),
            strokeWidth = (side * 0.025f).coerceAtLeast(1f),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.White,
            start = Offset(center.x - side * 0.01f, center.y + side * 0.04f),
            end = Offset(center.x + side * 0.065f, center.y - side * 0.045f),
            strokeWidth = (side * 0.025f).coerceAtLeast(1f),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun PlannerMotifPreview(
    visual: SquareDesignVisual?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    locked: Boolean = false,
    completed: Boolean = false,
) {
    val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.35f
    val surface = if (darkSurface) MotifSurface.DARK else MotifSurface.LIGHT
    Canvas(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .then(
                    if (contentDescription == null) {
                        Modifier
                    } else {
                        Modifier.semantics {
                            this.contentDescription = contentDescription
                        }
                    },
                ),
    ) {
        if (visual != null) {
            MotifRenderer.draw(
                canvas = drawContext.canvas.nativeCanvas,
                bounds = RectF(0f, 0f, size.width, size.height),
                visual = visual,
                config =
                    MotifRenderConfig(
                        surface = surface,
                        selected = selected,
                        locked = locked,
                        completed = completed,
                    ),
                smallPreviewThresholdPx = 24.dp.toPx(),
            )
        }
    }
}

@Suppress("kotlin:S107", "kotlin:S3776") // Generator controls intentionally expose each option and operation explicitly.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannerGeneratorSheet(
    state: PlannerUiState,
    showGridLines: Boolean,
    showLockMarkers: Boolean,
    onDismiss: () -> Unit,
    onSetMode: (PlannerGeneratorMode) -> Unit,
    onSetSeed: (String) -> Unit,
    onSetBandWidth: (Int) -> Unit,
    onSetAvoidNeighbors: (Boolean) -> Unit,
    onSetOverwriteCompletedCells: (Boolean) -> Unit,
    onToggleDesign: (String) -> Unit,
    onAdjustWeight: (String, Double) -> Unit,
    onMoveDesign: (String, Int) -> Unit,
    onGenerate: () -> Unit,
    onRegenerate: () -> Unit,
    onApply: () -> Unit,
) {
    val generator = state.generator
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().testTag("planner_generator_sheet"),
            contentPadding =
                PaddingValues(
                    start = SquareToolSpacing.Standard,
                    end = SquareToolSpacing.Standard,
                    bottom = SquareToolSpacing.ExtraLarge,
                ),
            verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small),
        ) {
            item {
                Text(
                    stringResource(R.string.planner_generator_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    stringResource(R.string.planner_generator_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                Text(
                    stringResource(R.string.planner_generator_mode),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(PlannerGeneratorMode.entries, key = PlannerGeneratorMode::name) { mode ->
                Surface(
                    selected = generator.mode == mode,
                    onClick = { onSetMode(mode) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = SquareToolSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = generator.mode == mode, onClick = null)
                        Text(stringResource(mode.labelRes), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = generator.seed,
                    onValueChange = onSetSeed,
                    label = { Text(stringResource(R.string.planner_generator_seed)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = generator.issue == PlannerGeneratorIssue.INVALID_SEED,
                    modifier = Modifier.fillMaxWidth().testTag("planner_generator_seed"),
                )
            }
            if (
                generator.mode == PlannerGeneratorMode.HORIZONTAL_STRIPES ||
                generator.mode == PlannerGeneratorMode.VERTICAL_STRIPES
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.planner_generator_band_width),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onSetBandWidth(generator.bandWidth - 1) },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(Icons.Default.Remove, stringResource(R.string.planner_decrease_stripe_width))
                            }
                            Text(generator.bandWidth.toString(), style = MaterialTheme.typography.titleMedium)
                            IconButton(
                                onClick = { onSetBandWidth(generator.bandWidth + 1) },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(Icons.Default.ZoomIn, stringResource(R.string.planner_increase_stripe_width))
                            }
                        }
                    }
                }
            }
            item {
                HorizontalDivider()
                Text(
                    stringResource(R.string.planner_generator_designs),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = SquareToolSpacing.Small),
                )
            }
            if (state.designs.isEmpty()) {
                item { Text(stringResource(R.string.planner_no_designs)) }
            } else {
                items(state.designs, key = PlannerDesignOption::id) { design ->
                    GeneratorDesignRow(
                        design = design,
                        canMoveUp = state.designs.firstOrNull()?.id != design.id,
                        canMoveDown = state.designs.lastOrNull()?.id != design.id,
                        onToggle = { onToggleDesign(design.id) },
                        onAdjustWeight = { delta -> onAdjustWeight(design.id, delta) },
                        onMove = { delta -> onMoveDesign(design.id, delta) },
                    )
                }
            }
            item {
                GeneratorSwitchRow(
                    label = stringResource(R.string.planner_generator_avoid_neighbors),
                    checked = generator.avoidOrthogonalNeighbors,
                    enabled =
                        generator.mode == PlannerGeneratorMode.RANDOM ||
                            generator.mode == PlannerGeneratorMode.BALANCED_RANDOM,
                    onCheckedChange = onSetAvoidNeighbors,
                )
                GeneratorSwitchRow(
                    label = stringResource(R.string.planner_generator_overwrite_completed),
                    checked = generator.overwriteCompleted,
                    enabled = state.trackingEnabled,
                    onCheckedChange = onSetOverwriteCompletedCells,
                )
                Text(
                    stringResource(
                        if (generator.overwriteCompleted) {
                            R.string.planner_generator_completed_overwritten
                        } else {
                            R.string.planner_generator_completed_protected
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.planner_generator_locks_preserved),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.layoutGenerationResult?.let { result ->
                item {
                    PlannerGenerationPreview(
                        state = state,
                        result = result,
                        showGridLines = showGridLines,
                        showLockMarkers = showLockMarkers,
                    )
                }
            }
            generator.issue?.let { issue ->
                item {
                    Text(
                        text = stringResource(issue.messageRes()),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small, Alignment.End),
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.planner_cancel))
                    }
                    if (state.layoutGenerationResult == null) {
                        Button(
                            enabled = !state.isGenerating,
                            onClick = onGenerate,
                            modifier = Modifier.heightIn(min = 56.dp).testTag("planner_generator_preview"),
                        ) {
                            if (state.isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(SquareToolSpacing.Small))
                            }
                            Text(
                                stringResource(
                                    if (state.isGenerating) {
                                        R.string.planner_generator_working
                                    } else {
                                        R.string.planner_generator_preview_action
                                    },
                                ),
                            )
                        }
                    } else {
                        OutlinedButton(
                            enabled = !state.isGenerating,
                            onClick = onRegenerate,
                            modifier = Modifier.heightIn(min = 56.dp).testTag("planner_generator_regenerate"),
                        ) {
                            Text(stringResource(R.string.planner_generator_regenerate))
                        }
                        Button(
                            enabled =
                                !state.isGenerating &&
                                    state.layoutGenerationResult.changedCellCount > 0 &&
                                    !state.layoutGenerationResult.applied,
                            onClick = onApply,
                            modifier = Modifier.heightIn(min = 56.dp).testTag("planner_generator_apply"),
                        ) {
                            Text(stringResource(R.string.planner_generator_apply))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannerGenerationPreview(
    state: PlannerUiState,
    result: PlannerLayoutGenerationResult,
    showGridLines: Boolean,
    showLockMarkers: Boolean,
) {
    val weightSummary =
        if (state.generator.mode.name
                .startsWith("MIRROR_")
        ) {
            ""
        } else {
            state.designs
                .filter(PlannerDesignOption::includedInGeneration)
                .joinToString(separator = ", ") { design ->
                    "${design.name} ${String.format(Locale.getDefault(), "%.2f", design.weight)}"
                }
        }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("planner_generator_result"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(SquareToolSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small),
        ) {
            Text(
                stringResource(R.string.planner_generator_preview_title),
                style = MaterialTheme.typography.titleLarge,
            )
            PlannerCanvas(
                rows = state.rows,
                columns = state.columns,
                cells = result.cells,
                selectedCoordinate = null,
                tool = PlannerTool.SELECT,
                command = null,
                showGridLines = showGridLines,
                showLockMarkers = showLockMarkers,
                showCompletionMarkers = state.trackingEnabled,
                onScaleChange = {},
                onSelectCell = {},
                onBeginToolDrag = { _ -> },
                onApplyToolDuringDrag = {},
                onEndToolDrag = {},
                onCancelToolDrag = {},
                modifier = Modifier.fillMaxWidth().height(220.dp),
                interactive = false,
                semanticDescription = stringResource(R.string.planner_generator_preview_description),
                testTag = "planner_generator_preview_canvas",
            )
            Text(
                pluralStringResource(
                    R.plurals.planner_generator_changed_cells,
                    result.changedCellCount,
                    result.changedCellCount,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            state.designs.forEach { design ->
                val count = result.designCounts[design.id] ?: 0
                if (count > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(design.name, style = MaterialTheme.typography.bodyMedium)
                        Text(count.toString(), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Text(
                if (weightSummary.isBlank()) {
                    stringResource(R.string.planner_generator_weight_summary_not_used)
                } else {
                    stringResource(R.string.planner_generator_weight_summary, weightSummary)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                pluralStringResource(
                    if (state.generator.avoidOrthogonalNeighbors) {
                        R.plurals.planner_generator_neighbor_summary_enabled
                    } else {
                        R.plurals.planner_generator_neighbor_summary_disabled
                    },
                    result.orthogonalConflictCount,
                    result.orthogonalConflictCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun GeneratorDesignRow(
    design: PlannerDesignOption,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onAdjustWeight: (Double) -> Unit,
    onMove: (Int) -> Unit,
) {
    val includeDescription = stringResource(R.string.planner_generator_include_design, design.name)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(SquareToolSpacing.Small)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = design.includedInGeneration,
                    onCheckedChange = { onToggle() },
                    modifier =
                        Modifier.semantics {
                            contentDescription = includeDescription
                        },
                )
                PlannerMotifPreview(
                    visual = design.visual,
                    contentDescription = stringResource(R.string.planner_design_preview, design.name),
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.width(SquareToolSpacing.Small))
                Text(
                    design.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(
                    enabled = canMoveUp,
                    onClick = { onMove(-1) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.planner_generator_move_up, design.name))
                }
                IconButton(
                    enabled = canMoveDown,
                    onClick = { onMove(1) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Redo, stringResource(R.string.planner_generator_move_down, design.name))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onAdjustWeight(-0.25) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Remove, stringResource(R.string.planner_generator_decrease_weight, design.name))
                }
                Text(stringResource(R.string.planner_generator_weight, design.weight))
                IconButton(onClick = { onAdjustWeight(0.25) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.ZoomIn, stringResource(R.string.planner_generator_increase_weight, design.name))
                }
            }
        }
    }
}

@Composable
private fun GeneratorSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

private fun PlannerGeneratorIssue.messageRes(): Int =
    when (this) {
        PlannerGeneratorIssue.INVALID_SEED -> R.string.planner_generator_invalid_seed
        PlannerGeneratorIssue.SELECT_A_DESIGN -> R.string.planner_generator_select_design
        PlannerGeneratorIssue.CHECKER_REQUIRES_TWO_DESIGNS -> R.string.planner_generator_checker_two
        PlannerGeneratorIssue.NO_CHANGES -> R.string.planner_generator_no_changes
    }

private const val MIN_CANVAS_SCALE = 0.35f
private const val MAX_CANVAS_SCALE = 6f

internal fun resolvePlannerMotifDetail(
    renderedCellSizePx: Float,
    smallPreviewThresholdPx: Float,
): MotifRenderDetail =
    if (renderedCellSizePx < smallPreviewThresholdPx) {
        MotifRenderDetail.SMALL
    } else {
        MotifRenderDetail.FULL
    }
