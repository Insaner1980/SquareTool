package com.finnvek.squaretool.domain.algorithm

import com.finnvek.squaretool.domain.model.BlanketDimensions
import com.finnvek.squaretool.domain.model.CellState
import com.finnvek.squaretool.domain.model.GridSize
import com.finnvek.squaretool.domain.model.GridSnapshot
import com.finnvek.squaretool.domain.model.MeasurementUnit

data class GridResizeResult(
    val snapshot: GridSnapshot,
    val lostCellCount: Int,
    val lostAssignedCellCount: Int,
)

object GridOperations {
    fun resize(
        snapshot: GridSnapshot,
        newSize: GridSize,
    ): GridResizeResult {
        val keptCells = snapshot.cells.filter { newSize.contains(it.coordinate) }
        val lostCells = snapshot.cells.filterNot { newSize.contains(it.coordinate) }
        return GridResizeResult(
            snapshot = GridSnapshot.of(newSize, keptCells),
            lostCellCount = lostCells.size,
            lostAssignedCellCount = lostCells.count { it.designId != null },
        )
    }
}

object MeasurementCalculator {
    private const val CENTIMETERS_PER_INCH = 2.54

    fun convert(
        value: Double,
        from: MeasurementUnit,
        to: MeasurementUnit,
    ): Double =
        when {
            from == to -> value
            from == MeasurementUnit.INCHES -> value * CENTIMETERS_PER_INCH
            else -> value / CENTIMETERS_PER_INCH
        }

    fun blanketDimensions(
        size: GridSize,
        squareWidth: Double?,
        squareHeight: Double? = null,
        joiningGap: Double? = 0.0,
        unit: MeasurementUnit = MeasurementUnit.CENTIMETERS,
    ): BlanketDimensions? {
        val width = squareWidth ?: return null
        val height = squareHeight ?: width
        val gap = joiningGap ?: 0.0
        if (!width.isFinite() || width <= 0.0) return null
        if (!height.isFinite() || height <= 0.0) return null
        if (!gap.isFinite() || gap < 0.0) return null
        return BlanketDimensions(
            width = size.columns * width + (size.columns - 1) * gap,
            height = size.rows * height + (size.rows - 1) * gap,
            unit = unit,
        )
    }
}
