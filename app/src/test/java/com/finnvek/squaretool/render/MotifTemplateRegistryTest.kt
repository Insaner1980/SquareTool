package com.finnvek.squaretool.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MotifTemplateRegistryTest {
    private val expectedRanges =
        linkedMapOf(
            "classic_granny" to (3..6),
            "sunburst" to (4..6),
            "daisy" to (4..5),
            "flower_medallion" to (4..6),
            "solid_center" to (3..5),
            "star_bloom" to (4..6),
            "diamond_layers" to (3..6),
            "pinwheel" to (3..5),
            "corner_accent" to (3..6),
            "simple_rounds" to (3..6),
        )

    @Test
    fun registryExposesExactlyTheStableTemplateIds() {
        assertEquals(expectedRanges.keys.toList(), MotifTemplateRegistry.templates.map { it.id })
        assertEquals(
            expectedRanges.size,
            MotifTemplateRegistry.templates
                .map { it.id }
                .toSet()
                .size,
        )
    }

    @Test
    fun templatesEnforceTheirDocumentedRoundBoundaries() {
        expectedRanges.forEach { (id, range) ->
            val template = MotifTemplateRegistry.require(id)

            assertEquals(range.first, template.minRounds)
            assertEquals(range.last, template.maxRounds)
            assertFalse(template.supportsRoundCount(range.first - 1))
            assertTrue(template.supportsRoundCount(range.first))
            assertTrue(template.supportsRoundCount(range.last))
            assertFalse(template.supportsRoundCount(range.last + 1))
            assertThrows(IllegalArgumentException::class.java) {
                template.requireSupportedRoundCount(range.first - 1)
            }
            assertThrows(IllegalArgumentException::class.java) {
                template.requireSupportedRoundCount(range.last + 1)
            }
        }
    }

    @Test
    fun everySupportedRoundCountHasPositiveNormalizedAreaWeights() {
        MotifTemplateRegistry.templates.forEach { template ->
            for (roundCount in template.minRounds..template.maxRounds) {
                val weights = template.areaWeights(roundCount)

                assertEquals("${template.id} round count", roundCount, weights.size)
                assertTrue("${template.id} positive weights", weights.all { it > 0f })
                assertEquals("${template.id} normalized weights", 1f, weights.sum(), 0.000_001f)
            }
        }
    }

    @Test
    fun omittedClassicRoundsAreCombinedIntoTheOutermostActiveRound() {
        assertEquals(
            listOf(0.10f, 0.14f, 0.76f),
            MotifTemplateRegistry.require("classic_granny").areaWeights(roundCount = 3),
        )
    }
}
