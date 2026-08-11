package com.finnvek.squaretool.ui.planner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlannerViewportTest {
    @Test
    fun fit_centersGridAndUsesAvailableViewport() {
        val viewport = PlannerViewport.fit(viewportWidth = 400f, viewportHeight = 600f, rows = 12, columns = 8)

        assertEquals(50f, viewport.cellSize, 0.001f)
        assertEquals(0f, viewport.offsetX, 0.001f)
        assertEquals(0f, viewport.offsetY, 0.001f)
    }

    @Test
    fun hitTest_mapsScreenPositionToRowAndColumn() {
        val viewport = PlannerViewport(cellSize = 40f, scale = 2f, offsetX = 10f, offsetY = 20f)

        assertEquals(PlannerCell(1, 2), viewport.hitTest(x = 180f, y = 130f, rows = 4, columns = 5))
    }

    @Test
    fun hitTest_returnsNullOutsideGrid() {
        val viewport = PlannerViewport(cellSize = 40f, scale = 1f, offsetX = 10f, offsetY = 20f)

        assertNull(viewport.hitTest(x = 9f, y = 40f, rows = 4, columns = 5))
        assertNull(viewport.hitTest(x = 211f, y = 40f, rows = 4, columns = 5))
    }

    @Test
    fun zoomAround_keepsContentPointUnderAnchor() {
        val viewport = PlannerViewport(cellSize = 40f, scale = 1f, offsetX = 0f, offsetY = 0f)

        val zoomed = viewport.zoomAround(anchorX = 100f, anchorY = 120f, zoomFactor = 2f, minScale = 0.5f, maxScale = 6f)

        assertEquals(-100f, zoomed.offsetX, 0.001f)
        assertEquals(-120f, zoomed.offsetY, 0.001f)
        assertEquals(2f, zoomed.scale, 0.001f)
    }

    @Test
    fun zoomAround_clampsScale() {
        val viewport = PlannerViewport(cellSize = 40f, scale = 5f, offsetX = 0f, offsetY = 0f)

        assertEquals(6f, viewport.zoomAround(0f, 0f, 10f, 0.5f, 6f).scale, 0.001f)
    }
}
