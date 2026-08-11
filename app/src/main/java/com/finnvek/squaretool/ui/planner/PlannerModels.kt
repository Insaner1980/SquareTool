package com.finnvek.squaretool.ui.planner

import androidx.annotation.StringRes
import com.finnvek.squaretool.R
import com.finnvek.squaretool.data.repository.AppSettings
import com.finnvek.squaretool.domain.model.CellCoordinate
import com.finnvek.squaretool.domain.model.CellState
import com.finnvek.squaretool.domain.model.GridSnapshot
import com.finnvek.squaretool.render.SquareDesignVisual
import kotlin.math.roundToInt

enum class PlannerTool {
    SELECT,
    PAINT,
    LOCK,
    PROGRESS,
}

data class PlannerSettingsPolicy(
    val showGridLines: Boolean,
    val showLockMarkers: Boolean,
    val defaultOverwriteCompleted: Boolean,
    val confirmDestructiveGeneration: Boolean,
)

internal fun plannerSettingsPolicy(settings: AppSettings): PlannerSettingsPolicy =
    PlannerSettingsPolicy(
        showGridLines = settings.showPlannerGridLines,
        showLockMarkers = settings.showLockMarkers,
        defaultOverwriteCompleted = !settings.preserveCompletedCells,
        confirmDestructiveGeneration = settings.confirmDestructiveLayoutGeneration,
    )

internal fun requiresGenerationConfirmation(
    confirmationEnabled: Boolean,
    before: GridSnapshot,
    after: GridSnapshot,
): Boolean =
    confirmationEnabled &&
        before.cells.any { current ->
            current.designId != null && after[current.coordinate].designId != current.designId
        }

internal fun applyPlannerTool(
    cell: CellState,
    tool: PlannerTool,
    selectedDesignId: String?,
    trackingEnabled: Boolean,
): CellState =
    when (tool) {
        PlannerTool.SELECT -> cell
        PlannerTool.PAINT -> if (cell.locked) cell else cell.copy(designId = selectedDesignId)
        PlannerTool.LOCK -> cell.copy(locked = !cell.locked)
        PlannerTool.PROGRESS -> if (trackingEnabled) cell.copy(completed = !cell.completed) else cell
    }

enum class PlannerGeneratorMode(
    @StringRes val labelRes: Int,
) {
    RANDOM(R.string.planner_generator_random),
    BALANCED_RANDOM(R.string.planner_generator_balanced),
    CHECKER(R.string.planner_generator_checker),
    ALTERNATING_ROWS(R.string.planner_generator_alternating_rows),
    ALTERNATING_COLUMNS(R.string.planner_generator_alternating_columns),
    DIAGONAL(R.string.planner_generator_diagonal),
    HORIZONTAL_STRIPES(R.string.planner_generator_horizontal_stripes),
    VERTICAL_STRIPES(R.string.planner_generator_vertical_stripes),
    MIRROR_LEFT_TO_RIGHT(R.string.planner_generator_mirror_left_to_right),
    MIRROR_RIGHT_TO_LEFT(R.string.planner_generator_mirror_right_to_left),
    MIRROR_TOP_TO_BOTTOM(R.string.planner_generator_mirror_top_to_bottom),
    MIRROR_BOTTOM_TO_TOP(R.string.planner_generator_mirror_bottom_to_top),
    GRADIENT_LEFT_TO_RIGHT(R.string.planner_generator_gradient_left_to_right),
    GRADIENT_RIGHT_TO_LEFT(R.string.planner_generator_gradient_right_to_left),
    GRADIENT_TOP_TO_BOTTOM(R.string.planner_generator_gradient_top_to_bottom),
    GRADIENT_BOTTOM_TO_TOP(R.string.planner_generator_gradient_bottom_to_top),
    GRADIENT_DIAGONAL(R.string.planner_generator_gradient_diagonal),
    RADIAL(R.string.planner_generator_radial),
}

enum class PlannerGeneratorIssue {
    INVALID_SEED,
    SELECT_A_DESIGN,
    CHECKER_REQUIRES_TWO_DESIGNS,
    NO_CHANGES,
}

data class PlannerDesignOption(
    val id: String,
    val name: String,
    val visual: SquareDesignVisual?,
    val includedInGeneration: Boolean = true,
    val weight: Double = 1.0,
)

data class PlannerUiCell(
    val coordinate: CellCoordinate,
    val designId: String?,
    val designName: String?,
    val visual: SquareDesignVisual?,
    val locked: Boolean,
    val completed: Boolean,
)

data class PlannerGeneratorState(
    val isOpen: Boolean = false,
    val mode: PlannerGeneratorMode = PlannerGeneratorMode.BALANCED_RANDOM,
    val seed: String = "",
    val bandWidth: Int = 1,
    val avoidOrthogonalNeighbors: Boolean = false,
    val overwriteCompleted: Boolean = false,
    val issue: PlannerGeneratorIssue? = null,
)

data class PlannerLayoutGenerationResult(
    val snapshot: GridSnapshot,
    val cells: List<PlannerUiCell>,
    val designCounts: Map<String, Int>,
    val orthogonalConflictCount: Int,
    val changedCellCount: Int,
    val applied: Boolean = false,
)

data class PlannerUiState(
    val isLoading: Boolean = true,
    val projectMissing: Boolean = false,
    val projectId: String = "",
    val projectName: String = "",
    val rows: Int = 0,
    val columns: Int = 0,
    val trackingEnabled: Boolean = false,
    val snapshot: GridSnapshot? = null,
    val cells: List<PlannerUiCell> = emptyList(),
    val designs: List<PlannerDesignOption> = emptyList(),
    val selectedCoordinate: CellCoordinate? = null,
    val selectedDesignId: String? = null,
    val tool: PlannerTool = PlannerTool.SELECT,
    val accessibleGridMode: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val isGenerating: Boolean = false,
    val generator: PlannerGeneratorState = PlannerGeneratorState(),
    val layoutGenerationResult: PlannerLayoutGenerationResult? = null,
) {
    val totalCount: Int get() = rows * columns
    val assignedCount: Int get() = cells.count { it.designId != null }
    val lockedCount: Int get() = cells.count(PlannerUiCell::locked)
    val completedCount: Int get() = cells.count(PlannerUiCell::completed)
    val completionPercent: Int
        get() =
            if (!trackingEnabled || totalCount == 0) {
                0
            } else {
                ((completedCount * 100.0) / totalCount).roundToInt().coerceIn(0, 100)
            }
    val clearableCount: Int get() = cells.count { !it.locked && (it.designId != null || it.completed) }
    val selectedCell: PlannerUiCell?
        get() = selectedCoordinate?.let { coordinate -> cells.firstOrNull { it.coordinate == coordinate } }
}
