package com.finnvek.squaretool.ui.planner

import kotlin.math.floor

data class PlannerCell(
    val row: Int,
    val column: Int,
)

data class PlannerViewport(
    val cellSize: Float,
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
) {
    fun hitTest(
        x: Float,
        y: Float,
        rows: Int,
        columns: Int,
    ): PlannerCell? {
        if (rows <= 0 || columns <= 0) return null
        val renderedCellSize = cellSize * scale
        val localX = x - offsetX
        val localY = y - offsetY
        if (localX < 0f || localY < 0f) return null
        val column = floor(localX / renderedCellSize).toInt()
        val row = floor(localY / renderedCellSize).toInt()
        return if (row in 0 until rows && column in 0 until columns) PlannerCell(row, column) else null
    }

    fun zoomAround(
        anchorX: Float,
        anchorY: Float,
        zoomFactor: Float,
        minScale: Float,
        maxScale: Float,
    ): PlannerViewport {
        val newScale = (scale * zoomFactor).coerceIn(minScale, maxScale)
        val appliedFactor = newScale / scale
        return copy(
            scale = newScale,
            offsetX = anchorX - (anchorX - offsetX) * appliedFactor,
            offsetY = anchorY - (anchorY - offsetY) * appliedFactor,
        )
    }

    fun panBy(
        deltaX: Float,
        deltaY: Float,
    ): PlannerViewport = copy(offsetX = offsetX + deltaX, offsetY = offsetY + deltaY)

    companion object {
        fun fit(
            viewportWidth: Float,
            viewportHeight: Float,
            rows: Int,
            columns: Int,
        ): PlannerViewport {
            require(viewportWidth > 0f && viewportHeight > 0f)
            require(rows > 0 && columns > 0)
            val cellSize = minOf(viewportWidth / columns, viewportHeight / rows)
            val gridWidth = cellSize * columns
            val gridHeight = cellSize * rows
            return PlannerViewport(
                cellSize = cellSize,
                scale = 1f,
                offsetX = (viewportWidth - gridWidth) / 2f,
                offsetY = (viewportHeight - gridHeight) / 2f,
            )
        }
    }
}
