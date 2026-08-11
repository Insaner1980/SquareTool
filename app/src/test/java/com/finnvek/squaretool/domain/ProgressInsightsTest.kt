package com.finnvek.squaretool.domain

import com.finnvek.squaretool.domain.algorithm.ColorUsageCalculator
import com.finnvek.squaretool.domain.algorithm.DesignDistributionCalculator
import com.finnvek.squaretool.domain.algorithm.ProgressCalculator
import com.finnvek.squaretool.domain.algorithm.YarnCalculator
import com.finnvek.squaretool.domain.model.CellCoordinate
import com.finnvek.squaretool.domain.model.CellState
import com.finnvek.squaretool.domain.model.DesignColorProfile
import com.finnvek.squaretool.domain.model.YarnSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressInsightsTest {
    @Test
    fun `sixty nine of ninety six rounds to seventy two percent`() {
        val progress =
            ProgressCalculator.calculate(
                completedCount = 69,
                totalCount = 96,
                trackingEnabled = true,
            )

        requireNotNull(progress)
        assertEquals(72, progress.percentage)
        assertEquals(27, progress.remainingCount)
    }

    @Test
    fun `tracking disabled has no progress result`() {
        assertNull(ProgressCalculator.calculate(69, 96, trackingEnabled = false))
    }

    @Test
    fun `empty total is safe and reports zero percent`() {
        val progress = ProgressCalculator.calculate(0, 0, trackingEnabled = true)

        requireNotNull(progress)
        assertEquals(0, progress.percentage)
        assertEquals(0, progress.remainingCount)
    }

    @Test
    fun `completed count is clamped to the valid total`() {
        val progress = ProgressCalculator.calculate(7, 5, trackingEnabled = true)

        requireNotNull(progress)
        assertEquals(5, progress.completedCount)
        assertEquals(100, progress.percentage)
    }

    @Test
    fun `design distribution counts assigned cells and blanks separately`() {
        val snapshot =
            grid(2, 3) { row, column ->
                val design = listOf("A", "A", null, "B", null, "A")[row * 3 + column]
                CellState(CellCoordinate(row, column), designId = design)
            }

        val result = DesignDistributionCalculator.calculate(snapshot)

        assertEquals(mapOf("A" to 3, "B" to 1), result.designCounts)
        assertEquals(4, result.assignedCount)
        assertEquals(2, result.blankCount)
    }

    @Test
    fun `round weights are normalized to one`() {
        val normalized = ColorUsageCalculator.normalizeWeights(listOf(1.0, 2.0, 1.0))

        assertEquals(listOf(0.25, 0.5, 0.25), normalized)
        assertEquals(1.0, normalized.sum(), 1e-12)
    }

    @Test
    fun `repeated round colors combine into one color percentage`() {
        val snapshot =
            grid(1, 2) { _, column ->
                CellState(CellCoordinate(0, column), designId = "flower")
            }
        val profile =
            DesignColorProfile(
                designId = "flower",
                roundColorIds = listOf("red", "blue", "red"),
                roundWeights = listOf(0.25, 0.5, 0.25),
            )

        val usage = ColorUsageCalculator.calculate(snapshot, mapOf("flower" to profile))

        assertEquals(50.0, usage.percentages.getValue("red"), 1e-10)
        assertEquals(50.0, usage.percentages.getValue("blue"), 1e-10)
    }

    @Test
    fun `color percentages sum to one hundred and ignore blank cells`() {
        val snapshot =
            grid(1, 3) { _, column ->
                CellState(
                    CellCoordinate(0, column),
                    designId =
                        if (column == 2) {
                            null
                        } else if (column == 0) {
                            "A"
                        } else {
                            "B"
                        },
                )
            }
        val profiles =
            mapOf(
                "A" to DesignColorProfile("A", listOf("red"), listOf(1.0)),
                "B" to DesignColorProfile("B", listOf("blue"), listOf(1.0)),
            )

        val usage = ColorUsageCalculator.calculate(snapshot, profiles)

        assertEquals(100.0, usage.percentages.values.sum(), 1e-10)
        assertEquals(2, usage.contributingCellCount)
        assertEquals(50.0, usage.percentages.getValue("red"), 1e-10)
    }

    @Test
    fun `global grams buffer and skein rounding are calculated from assigned cells`() {
        val snapshot =
            grid(1, 3) { _, column ->
                CellState(CellCoordinate(0, column), designId = if (column < 2) "A" else null)
            }

        val estimate =
            YarnCalculator.estimate(
                snapshot = snapshot,
                profiles = emptyMap(),
                settings =
                    YarnSettings(
                        globalGramsPerSquare = 10.0,
                        skeinWeightGrams = 5.0,
                        bufferPercent = 10.0,
                    ),
            )

        requireNotNull(estimate)
        assertEquals(20.0, estimate.baseGrams, 1e-10)
        assertEquals(22.0, estimate.totalGrams, 1e-10)
        assertEquals(4.4, estimate.equivalentSkeins, 1e-10)
        assertEquals(5, estimate.recommendedWholeSkeins)
    }

    @Test
    fun `per design grams override takes precedence over global grams`() {
        val snapshot =
            grid(1, 2) { _, column ->
                CellState(CellCoordinate(0, column), designId = if (column == 0) "A" else "B")
            }
        val profiles =
            mapOf(
                "A" to DesignColorProfile("A", listOf("red"), listOf(1.0), gramsPerSquareOverride = 20.0),
                "B" to DesignColorProfile("B", listOf("blue"), listOf(1.0)),
            )

        val estimate =
            YarnCalculator.estimate(
                snapshot,
                profiles,
                YarnSettings(globalGramsPerSquare = 10.0, skeinWeightGrams = 10.0),
            )

        requireNotNull(estimate)
        assertEquals(30.0, estimate.totalGrams, 1e-10)
        assertEquals(3.0, estimate.equivalentSkeins, 1e-10)
    }

    @Test
    fun `cell grams override takes precedence over design override`() {
        val snapshot =
            grid(1, 1) { _, _ ->
                CellState(CellCoordinate(0, 0), designId = "A", gramsPerSquareOverride = 7.0)
            }
        val profiles =
            mapOf(
                "A" to DesignColorProfile("A", listOf("red"), listOf(1.0), gramsPerSquareOverride = 20.0),
            )

        val estimate =
            YarnCalculator.estimate(
                snapshot,
                profiles,
                YarnSettings(globalGramsPerSquare = 10.0, skeinWeightGrams = 10.0),
            )

        requireNotNull(estimate)
        assertEquals(7.0, estimate.totalGrams, 1e-10)
    }

    @Test
    fun `color yarn grams follow motif round weights including buffer`() {
        val snapshot =
            grid(1, 1) { _, _ ->
                CellState(CellCoordinate(0, 0), designId = "A")
            }
        val profiles =
            mapOf(
                "A" to
                    DesignColorProfile(
                        "A",
                        listOf("red", "blue", "red"),
                        listOf(0.25, 0.5, 0.25),
                    ),
            )

        val estimate =
            YarnCalculator.estimate(
                snapshot,
                profiles,
                YarnSettings(globalGramsPerSquare = 10.0, skeinWeightGrams = 10.0, bufferPercent = 20.0),
            )

        requireNotNull(estimate)
        assertEquals(6.0, estimate.colorGrams.getValue("red"), 1e-10)
        assertEquals(6.0, estimate.colorGrams.getValue("blue"), 1e-10)
    }

    @Test
    fun `missing or invalid yarn settings have no estimate`() {
        val snapshot = grid(1, 1) { _, _ -> CellState(CellCoordinate(0, 0), designId = "A") }

        assertNull(YarnCalculator.estimate(snapshot, emptyMap(), YarnSettings(null, 50.0)))
        assertNull(YarnCalculator.estimate(snapshot, emptyMap(), YarnSettings(10.0, null)))
        assertNull(YarnCalculator.estimate(snapshot, emptyMap(), YarnSettings(0.0, 50.0)))
        assertNull(YarnCalculator.estimate(snapshot, emptyMap(), YarnSettings(10.0, 0.0)))
        assertNull(YarnCalculator.estimate(snapshot, emptyMap(), YarnSettings(10.0, 50.0, -1.0)))
    }

    @Test
    fun `unknown design without global grams prevents partial estimate`() {
        val snapshot =
            grid(1, 2) { _, column ->
                CellState(CellCoordinate(0, column), designId = if (column == 0) "known" else "unknown")
            }
        val profiles =
            mapOf(
                "known" to
                    DesignColorProfile(
                        "known",
                        listOf("red"),
                        listOf(1.0),
                        gramsPerSquareOverride = 10.0,
                    ),
            )

        assertNull(
            YarnCalculator.estimate(
                snapshot,
                profiles,
                YarnSettings(globalGramsPerSquare = null, skeinWeightGrams = 50.0),
            ),
        )
    }

    @Test
    fun `normalizing non positive weights returns no usable color weights`() {
        assertTrue(ColorUsageCalculator.normalizeWeights(listOf(0.0, -1.0)).isEmpty())
    }
}
