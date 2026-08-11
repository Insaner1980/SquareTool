package com.finnvek.squaretool.ui.planner

import com.finnvek.squaretool.render.MotifRenderDetail
import org.junit.Assert.assertEquals
import org.junit.Test

class PlannerMotifDetailTest {
    @Test
    fun renderedCellsBelowThresholdUseSmallDetail() {
        assertEquals(
            MotifRenderDetail.SMALL,
            resolvePlannerMotifDetail(
                renderedCellSizePx = 23.9f,
                smallPreviewThresholdPx = 24f,
            ),
        )
    }

    @Test
    fun renderedCellsAtThresholdUseFullDetail() {
        assertEquals(
            MotifRenderDetail.FULL,
            resolvePlannerMotifDetail(
                renderedCellSizePx = 24f,
                smallPreviewThresholdPx = 24f,
            ),
        )
    }
}
