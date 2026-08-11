package com.finnvek.squaretool.domain

import com.finnvek.squaretool.domain.algorithm.GridOperations
import com.finnvek.squaretool.domain.algorithm.MeasurementCalculator
import com.finnvek.squaretool.domain.model.CellCoordinate
import com.finnvek.squaretool.domain.model.CellState
import com.finnvek.squaretool.domain.model.GridSize
import com.finnvek.squaretool.domain.model.GridSnapshot
import com.finnvek.squaretool.domain.model.MeasurementUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GridCalculationsTest {
    @Test
    fun `eight by twelve contains ninety six cells`() {
        assertEquals(96, GridSize(rows = 12, columns = 8).cellCount)
    }

    @Test
    fun `grid accepts documented minimum and maximum boundaries`() {
        assertEquals(1, GridSize(1, 1).cellCount)
        assertEquals(2_500, GridSize(50, 50).cellCount)
    }

    @Test
    fun `grid rejects dimensions outside supported boundaries`() {
        assertThrows(IllegalArgumentException::class.java) { GridSize(0, 1) }
        assertThrows(IllegalArgumentException::class.java) { GridSize(1, 0) }
        assertThrows(IllegalArgumentException::class.java) { GridSize(51, 1) }
        assertThrows(IllegalArgumentException::class.java) { GridSize(1, 51) }
    }

    @Test
    fun `snapshot rejects coordinates outside its grid`() {
        assertThrows(IllegalArgumentException::class.java) {
            GridSnapshot.of(
                GridSize(1, 1),
                listOf(CellState(CellCoordinate(1, 0))),
            )
        }
    }

    @Test
    fun `enlarging preserves top left cells and adds blank cells`() {
        val original =
            grid(2, 2) { row, column ->
                CellState(CellCoordinate(row, column), designId = "${row}$column")
            }

        val result = GridOperations.resize(original, GridSize(3, 4))

        assertEquals("00", result.snapshot[CellCoordinate(0, 0)].designId)
        assertEquals("11", result.snapshot[CellCoordinate(1, 1)].designId)
        assertNull(result.snapshot[CellCoordinate(2, 3)].designId)
        assertEquals(0, result.lostCellCount)
        assertEquals(0, result.lostAssignedCellCount)
    }

    @Test
    fun `shrinking reports all removed and assigned removed cells`() {
        val original =
            grid(3, 3) { row, column ->
                val assigned = (row == 0 && column == 2) || (row == 2 && column == 2)
                CellState(CellCoordinate(row, column), designId = if (assigned) "A" else null)
            }

        val result = GridOperations.resize(original, GridSize(2, 2))

        assertEquals(5, result.lostCellCount)
        assertEquals(2, result.lostAssignedCellCount)
        assertEquals(4, result.snapshot.size.cellCount)
        assertFalse(result.snapshot.cells.any { it.designId != null })
    }

    @Test
    fun `inch converts exactly to two point five four centimeters`() {
        assertEquals(
            2.54,
            MeasurementCalculator.convert(1.0, MeasurementUnit.INCHES, MeasurementUnit.CENTIMETERS),
            0.0,
        )
        assertEquals(
            1.0,
            MeasurementCalculator.convert(2.54, MeasurementUnit.CENTIMETERS, MeasurementUnit.INCHES),
            1e-12,
        )
    }

    @Test
    fun `blanket dimensions include joining gaps between squares only`() {
        val dimensions =
            MeasurementCalculator.blanketDimensions(
                size = GridSize(rows = 3, columns = 4),
                squareWidth = 10.0,
                squareHeight = 12.0,
                joiningGap = 0.5,
                unit = MeasurementUnit.CENTIMETERS,
            )

        requireNotNull(dimensions)
        assertEquals(41.5, dimensions.width, 0.0)
        assertEquals(37.0, dimensions.height, 0.0)
        assertEquals(MeasurementUnit.CENTIMETERS, dimensions.unit)
    }

    @Test
    fun `blanket height defaults to square width`() {
        val dimensions =
            MeasurementCalculator.blanketDimensions(
                size = GridSize(rows = 12, columns = 8),
                squareWidth = 6.0,
                unit = MeasurementUnit.INCHES,
            )

        requireNotNull(dimensions)
        assertEquals(48.0, dimensions.width, 0.0)
        assertEquals(72.0, dimensions.height, 0.0)
    }

    @Test
    fun `missing zero or negative measurements do not produce dimensions`() {
        assertNull(MeasurementCalculator.blanketDimensions(GridSize(2, 2), null))
        assertNull(MeasurementCalculator.blanketDimensions(GridSize(2, 2), 0.0))
        assertNull(MeasurementCalculator.blanketDimensions(GridSize(2, 2), -1.0))
        assertNull(
            MeasurementCalculator.blanketDimensions(
                GridSize(2, 2),
                squareWidth = 5.0,
                joiningGap = -0.1,
            ),
        )
    }

    @Test
    fun `grid contains only coordinates inside its bounds`() {
        val size = GridSize(2, 3)

        assertTrue(size.contains(CellCoordinate(0, 0)))
        assertTrue(size.contains(CellCoordinate(1, 2)))
        assertFalse(size.contains(CellCoordinate(2, 2)))
        assertFalse(size.contains(CellCoordinate(1, 3)))
    }
}
