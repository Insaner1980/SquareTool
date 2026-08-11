package com.finnvek.squaretool.domain

import com.finnvek.squaretool.domain.algorithm.GenerationRequest
import com.finnvek.squaretool.domain.algorithm.LayoutGenerator
import com.finnvek.squaretool.domain.algorithm.LayoutMode
import com.finnvek.squaretool.domain.algorithm.PlannerHistory
import com.finnvek.squaretool.domain.algorithm.WeightedDesign
import com.finnvek.squaretool.domain.model.CellCoordinate
import com.finnvek.squaretool.domain.model.GridSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerHistoryTest {
    @Test
    fun `single cell edit can be undone and redone`() {
        val initial = grid(2, 2)
        val history = PlannerHistory(initial)
        val painted =
            initial.updated(
                initial[CellCoordinate(0, 1)].copy(designId = "A"),
            )

        history.record("Paint cell", painted)
        assertEquals("A", history.current[CellCoordinate(0, 1)].designId)

        history.undo()
        assertEquals(null, history.current[CellCoordinate(0, 1)].designId)

        history.redo()
        assertEquals("A", history.current[CellCoordinate(0, 1)].designId)
    }

    @Test
    fun `drag paint group is one coherent history operation`() {
        val initial = grid(1, 4)
        val history = PlannerHistory(initial)
        val painted =
            initial.updated(
                (0..2).map { column ->
                    initial[CellCoordinate(0, column)].copy(designId = "A")
                },
            )

        history.record("Paint drag", painted)

        assertEquals(1, history.undoDepth)
        history.undo()
        assertEquals(listOf(null, null, null, null), history.current.designsInRowMajorOrder())
    }

    @Test
    fun `lock and completion changes remain independent and undoable`() {
        val initial = grid(1, 1)
        val history = PlannerHistory(initial)
        val coordinate = CellCoordinate(0, 0)

        history.record("Lock", history.current.updated(history.current[coordinate].copy(locked = true)))
        history.record("Complete", history.current.updated(history.current[coordinate].copy(completed = true)))

        assertTrue(history.current[coordinate].locked)
        assertTrue(history.current[coordinate].completed)
        history.undo()
        assertTrue(history.current[coordinate].locked)
        assertFalse(history.current[coordinate].completed)
        history.undo()
        assertFalse(history.current[coordinate].locked)
    }

    @Test
    fun `generator application is represented as one history operation`() {
        val initial = grid(8, 12)
        val generated =
            LayoutGenerator
                .generate(
                    GenerationRequest(
                        initial,
                        listOf(WeightedDesign("A"), WeightedDesign("B")),
                        LayoutMode.BalancedRandom,
                        seed = 10L,
                    ),
                ).snapshot
        val history = PlannerHistory(initial)

        history.record("Generate balanced", generated)

        assertEquals(1, history.undoDepth)
        assertEquals("Generate balanced", history.lastOperationLabel)
        history.undo()
        assertEquals(initial, history.current)
    }

    @Test
    fun `clear operation restores all cells with one undo`() {
        val initial =
            grid(2, 2) { row, column ->
                com.finnvek.squaretool.domain.model.CellState(
                    CellCoordinate(row, column),
                    designId = "A",
                )
            }
        val cleared = initial.updated(initial.cells.map { it.copy(designId = null) })
        val history = PlannerHistory(initial)

        history.record("Clear unlocked", cleared)
        assertEquals(listOf(null, null, null, null), history.current.designsInRowMajorOrder())
        history.undo()
        assertEquals(listOf("A", "A", "A", "A"), history.current.designsInRowMajorOrder())
    }

    @Test
    fun `new operation after undo clears redo branch`() {
        val initial = grid(1, 1)
        val coordinate = CellCoordinate(0, 0)
        val history = PlannerHistory(initial)

        history.record("A", initial.updated(initial[coordinate].copy(designId = "A")))
        history.undo()
        history.record("B", history.current.updated(history.current[coordinate].copy(designId = "B")))

        assertFalse(history.canRedo)
        assertEquals("B", history.current[coordinate].designId)
        history.redo()
        assertEquals("B", history.current[coordinate].designId)
    }

    @Test
    fun `planner resize can be undone and redone as one operation`() {
        val initial = grid(2, 2)
        val resized =
            com.finnvek.squaretool.domain.algorithm.GridOperations
                .resize(
                    initial,
                    GridSize(3, 4),
                ).snapshot
        val history = PlannerHistory(initial)

        history.record("Resize project", resized)

        assertEquals(GridSize(3, 4), history.current.size)
        assertEquals(1, history.undoDepth)
        history.undo()
        assertEquals(GridSize(2, 2), history.current.size)
        history.redo()
        assertEquals(GridSize(3, 4), history.current.size)
    }

    @Test
    fun `unchanged snapshot is not a meaningful history operation`() {
        val initial = grid(1, 1)
        val history = PlannerHistory(initial)

        history.record("No change", initial)

        assertEquals(0, history.undoDepth)
        assertFalse(history.canUndo)
    }

    @Test
    fun `history retains at least the latest fifty meaningful operations`() {
        val initial = grid(1, 1)
        val coordinate = CellCoordinate(0, 0)
        val history = PlannerHistory(initial)

        repeat(60) { index ->
            history.record(
                "Edit $index",
                history.current.updated(history.current[coordinate].copy(designId = "D$index")),
            )
        }

        assertEquals(50, history.undoDepth)
        repeat(50) { history.undo() }
        assertFalse(history.canUndo)
        assertEquals("D9", history.current[coordinate].designId)
    }
}
