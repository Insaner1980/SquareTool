package com.finnvek.squaretool.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MotifGeometryPlannerTest {
    private val colors =
        listOf(
            0xFF6B8A2E.toInt(),
            0xFFF3E6C9.toInt(),
            0xFFD75A1F.toInt(),
            0xFFD99A1E.toInt(),
            0xFF9BA77A.toInt(),
            0xFF4B2D18.toInt(),
        )

    @Test
    fun simpleRoundsProducesTheExactNormalizedThreeRoundGeometry() {
        val plan =
            MotifGeometryPlanner.createPlan(
                visual = SquareDesignVisual("simple_rounds", colors.take(3)),
                config =
                    MotifRenderConfig(
                        detail = MotifRenderDetail.FULL,
                        surface = MotifSurface.LIGHT,
                    ),
            )

        assertEquals(
            listOf(
                MotifPrimitive.RoundedSquare(2, inset = 0.04f, cornerRadius = 0.12f, outlineWidth = 0.012f),
                MotifPrimitive.RoundedSquare(1, inset = 0.20f, cornerRadius = 0.10f, outlineWidth = 0.010f),
                MotifPrimitive.RoundedSquare(0, inset = 0.36f, cornerRadius = 0.08f, outlineWidth = 0.008f),
            ),
            plan.primitives,
        )
    }

    @Test
    fun everyTemplateProducesDeterministicGeometryUsingEveryRound() {
        MotifTemplateRegistry.templates.forEach { template ->
            val visual = SquareDesignVisual(template.id, colors.take(template.maxRounds))
            val config =
                MotifRenderConfig(
                    detail = MotifRenderDetail.FULL,
                    surface = MotifSurface.DARK,
                )

            val first = MotifGeometryPlanner.createPlan(visual, config)
            val second = MotifGeometryPlanner.createPlan(visual, config)

            assertEquals("${template.id} must be deterministic", first, second)
            assertEquals(
                "${template.id} must use every configured round",
                (0 until template.maxRounds).toSet(),
                first.primitives.map { it.roundIndex }.toSet(),
            )
        }
    }

    @Test
    fun smallDetailUsesFewerPrimitivesButKeepsEveryRoundVisible() {
        val visual = SquareDesignVisual("flower_medallion", colors)

        val small =
            MotifGeometryPlanner.createPlan(
                visual,
                MotifRenderConfig(MotifRenderDetail.SMALL, MotifSurface.LIGHT),
            )
        val full =
            MotifGeometryPlanner.createPlan(
                visual,
                MotifRenderConfig(MotifRenderDetail.FULL, MotifSurface.LIGHT),
            )

        assertTrue(full.primitives.size > small.primitives.size)
        assertEquals((0..5).toSet(), small.primitives.map { it.roundIndex }.toSet())
    }

    @Test
    fun lightAndDarkSurfacesSelectContrastingStableOutlineColors() {
        val visual = SquareDesignVisual("simple_rounds", colors.take(3))

        val light =
            MotifGeometryPlanner.createPlan(
                visual,
                MotifRenderConfig(MotifRenderDetail.SMALL, MotifSurface.LIGHT),
            )
        val dark =
            MotifGeometryPlanner.createPlan(
                visual,
                MotifRenderConfig(MotifRenderDetail.SMALL, MotifSurface.DARK),
            )

        assertEquals(0x663A4020, light.outlineArgb)
        assertEquals(0x99FFF8E8.toInt(), dark.outlineArgb)
    }

    @Test
    fun selectedLockedAndCompletedStatesProduceClearStableOverlays() {
        val plan =
            MotifGeometryPlanner.createPlan(
                visual = SquareDesignVisual("classic_granny", colors.take(3)),
                config =
                    MotifRenderConfig(
                        detail = MotifRenderDetail.SMALL,
                        surface = MotifSurface.LIGHT,
                        selected = true,
                        locked = true,
                        completed = true,
                    ),
            )

        assertEquals(
            listOf(
                MotifOverlay.CompletionWash(alpha = 0.14f),
                MotifOverlay.CompletedCheck,
                MotifOverlay.LockedBadge,
                MotifOverlay.SelectionBorder(width = 0.035f),
            ),
            plan.overlays,
        )
    }

    @Test
    fun plannerRejectsUnknownTemplatesAndUnsupportedColorCounts() {
        assertThrows(IllegalArgumentException::class.java) {
            MotifGeometryPlanner.createPlan(
                SquareDesignVisual("missing", colors.take(3)),
                MotifRenderConfig(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            MotifGeometryPlanner.createPlan(
                SquareDesignVisual("daisy", colors.take(3)),
                MotifRenderConfig(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            MotifGeometryPlanner.createPlan(
                SquareDesignVisual("solid_center", colors),
                MotifRenderConfig(),
            )
        }
    }
}
