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

    private fun snapshot(designIds: List<String?>): GridSnapshot =
        GridSnapshot.of(
            GridSize(rows = 1, columns = designIds.size),
            designIds.mapIndexed { column, designId ->
                CellState(CellCoordinate(0, column), designId = designId)
            },
        )
}
