package com.finnvek.squaretool.ui.planner

import com.finnvek.squaretool.data.repository.AppSettings
import com.finnvek.squaretool.domain.model.CellCoordinate
import com.finnvek.squaretool.domain.model.CellState
import com.finnvek.squaretool.domain.model.GridSize
import com.finnvek.squaretool.domain.model.GridSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerDecisionLogicTest {
    @Test
    fun settingsPolicyMapsPlannerFlagsAndCompletedDefault() {
        val policy =
            plannerSettingsPolicy(
                AppSettings(
                    showPlannerGridLines = false,
                    showLockMarkers = false,
                    preserveCompletedCells = false,
                    confirmDestructiveLayoutGeneration = true,
                ),
            )

        assertFalse(policy.showGridLines)
        assertFalse(policy.showLockMarkers)
        assertTrue(policy.defaultOverwriteCompleted)
        assertTrue(policy.confirmDestructiveGeneration)
    }

    @Test
    fun confirmationIsRequiredOnlyWhenGenerationReplacesAssignedContent() {
        val before = snapshot(designIds = listOf("a", null))
        val replacement = snapshot(designIds = listOf("b", "a"))
        val blankFillOnly = snapshot(designIds = listOf("a", "b"))

        assertTrue(
            requiresGenerationConfirmation(
                confirmationEnabled = true,
                before = before,
                after = replacement,
            ),
        )
        assertFalse(
            requiresGenerationConfirmation(
                confirmationEnabled = true,
                before = before,
                after = blankFillOnly,
            ),
        )
        assertFalse(
            requiresGenerationConfirmation(
                confirmationEnabled = false,
                before = before,
                after = replacement,
            ),
        )
    }

    @Test
    fun lockAndProgressToolsToggleOnlyTheirOwnState() {
        val original =
            CellState(
                coordinate = CellCoordinate(0, 0),
                designId = "a",
                locked = false,
                completed = false,
            )

        assertEquals(
            original.copy(locked = true),
            applyPlannerTool(original, PlannerTool.LOCK, selectedDesignId = "b", trackingEnabled = true),
        )
        assertEquals(
            original.copy(completed = true),
            applyPlannerTool(original, PlannerTool.PROGRESS, selectedDesignId = "b", trackingEnabled = true),
        )
        assertEquals(
            original,
            applyPlannerTool(original, PlannerTool.PROGRESS, selectedDesignId = "b", trackingEnabled = false),
        )
    }

    @Test
    fun paintToolPreservesLockedCells() {
        val locked =
            CellState(
                coordinate = CellCoordinate(0, 0),
                designId = "a",
                locked = true,
            )

        assertEquals(
            locked,
            applyPlannerTool(locked, PlannerTool.PAINT, selectedDesignId = "b", trackingEnabled = true),
        )
    }

    @Test
    fun selectAndPaintToolsHandleEditableCells() {
        val original = CellState(coordinate = CellCoordinate(0, 0), designId = "a")

        assertEquals(
            original,
            applyPlannerTool(original, PlannerTool.SELECT, selectedDesignId = "b", trackingEnabled = true),
        )
        assertEquals(
            original.copy(designId = "b"),
            applyPlannerTool(original, PlannerTool.PAINT, selectedDesignId = "b", trackingEnabled = true),
        )
        assertEquals(
            original.copy(designId = null),
            applyPlannerTool(original, PlannerTool.PAINT, selectedDesignId = null, trackingEnabled = true),
        )
    }

    @Test
    fun uiStateSummariesReflectCurrentCellsAndSelection() {
        val selectedCoordinate = CellCoordinate(0, 1)
        val cells =
            listOf(
                PlannerUiCell(CellCoordinate(0, 0), "a", "A", null, locked = false, completed = true),
                PlannerUiCell(selectedCoordinate, "b", "B", null, locked = true, completed = false),
                PlannerUiCell(CellCoordinate(1, 0), null, null, null, locked = false, completed = true),
                PlannerUiCell(CellCoordinate(1, 1), null, null, null, locked = true, completed = false),
            )
        val state =
            PlannerUiState(
                isLoading = false,
                projectId = "project-1",
                projectName = "Blanket",
                rows = 2,
                columns = 2,
                trackingEnabled = true,
                cells = cells,
                selectedCoordinate = selectedCoordinate,
            )

        assertEquals(4, state.totalCount)
        assertEquals(2, state.assignedCount)
        assertEquals(2, state.lockedCount)
        assertEquals(2, state.completedCount)
        assertEquals(50, state.completionPercent)
        assertEquals(2, state.clearableCount)
        assertEquals(cells[1], state.selectedCell)
    }

    @Test
    fun uiStateReturnsEmptySummariesWhenTrackingOrGridIsUnavailable() {
        val trackingDisabled =
            PlannerUiState(
                isLoading = false,
                rows = 1,
                columns = 1,
                trackingEnabled = false,
                cells =
                    listOf(
                        PlannerUiCell(CellCoordinate(0, 0), null, null, null, locked = false, completed = true),
                    ),
                selectedCoordinate = CellCoordinate(0, 1),
            )
        val emptyGrid = PlannerUiState(isLoading = false, trackingEnabled = true)

        assertEquals(0, trackingDisabled.completionPercent)
        assertEquals(null, trackingDisabled.selectedCell)
        assertEquals(0, emptyGrid.completionPercent)
        assertEquals(0, emptyGrid.totalCount)
    }

    @Test
    fun generatorModelsExposeDefaultsAndCalculatedResultData() {
        val snapshot = snapshot(designIds = listOf("a", "b"))
        val cells =
            listOf(
                PlannerUiCell(CellCoordinate(0, 0), "a", "A", null, locked = false, completed = false),
                PlannerUiCell(CellCoordinate(0, 1), "b", "B", null, locked = false, completed = false),
            )
        val generator = PlannerGeneratorState()
        val result =
            PlannerLayoutGenerationResult(
                snapshot = snapshot,
                cells = cells,
                designCounts = mapOf("a" to 1, "b" to 1),
                orthogonalConflictCount = 0,
                changedCellCount = 2,
            )
        val option = PlannerDesignOption("a", "A", visual = null)

        assertFalse(generator.isOpen)
        assertEquals(PlannerGeneratorMode.BALANCED_RANDOM, generator.mode)
        assertEquals(1, generator.bandWidth)
        assertTrue(option.includedInGeneration)
        assertEquals(1.0, option.weight, 0.0)
        assertEquals(2, result.changedCellCount)
        assertFalse(result.applied)
        assertEquals(18, PlannerGeneratorMode.entries.size)
        assertEquals(4, PlannerGeneratorIssue.entries.size)
    }

    private fun snapshot(designIds: List<String?>): GridSnapshot =
        GridSnapshot.of(
            GridSize(rows = 1, columns = designIds.size),
            designIds.mapIndexed { column, designId ->
                CellState(CellCoordinate(0, column), designId = designId)
            },
        )
}
