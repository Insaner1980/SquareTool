package com.finnvek.squaretool.domain.algorithm

import com.finnvek.squaretool.domain.model.ColorUsage
import com.finnvek.squaretool.domain.model.DesignColorProfile
import com.finnvek.squaretool.domain.model.DesignDistribution
import com.finnvek.squaretool.domain.model.GridSnapshot
import com.finnvek.squaretool.domain.model.ProjectProgress
import com.finnvek.squaretool.domain.model.YarnEstimate
import com.finnvek.squaretool.domain.model.YarnSettings
import kotlin.math.ceil
import kotlin.math.roundToInt

object ProgressCalculator {
    fun calculate(
        completedCount: Int,
        totalCount: Int,
        trackingEnabled: Boolean,
    ): ProjectProgress? {
        if (!trackingEnabled) return null
        val safeTotal = totalCount.coerceAtLeast(0)
        val safeCompleted = completedCount.coerceIn(0, safeTotal)
        val percentage =
            if (safeTotal == 0) {
                0
            } else {
                (safeCompleted * 100.0 / safeTotal).roundToInt()
            }
        return ProjectProgress(
            completedCount = safeCompleted,
            remainingCount = safeTotal - safeCompleted,
            totalCount = safeTotal,
            percentage = percentage,
        )
    }

    fun calculate(
        snapshot: GridSnapshot,
        trackingEnabled: Boolean,
    ): ProjectProgress? =
        calculate(
            completedCount = snapshot.cells.count { it.completed },
            totalCount = snapshot.size.cellCount,
            trackingEnabled = trackingEnabled,
        )
}

object DesignDistributionCalculator {
    fun calculate(snapshot: GridSnapshot): DesignDistribution {
        val designCounts =
            snapshot.cells
                .mapNotNull { it.designId }
                .groupingBy { it }
                .eachCount()
        return DesignDistribution(
            designCounts = designCounts,
            assignedCount = designCounts.values.sum(),
            blankCount = snapshot.cells.count { it.designId == null },
        )
    }
}

object ColorUsageCalculator {
    fun normalizeWeights(weights: List<Double>): List<Double> {
        if (weights.isEmpty() || weights.any { !it.isFinite() || it < 0.0 }) return emptyList()
        val total = weights.sum()
        if (total <= 0.0 || !total.isFinite()) return emptyList()
        return weights.map { it / total }
    }

    fun calculate(
        snapshot: GridSnapshot,
        profiles: Map<String, DesignColorProfile>,
    ): ColorUsage {
        val totals = linkedMapOf<String, Double>()
        var contributingCells = 0
        snapshot.cells.forEach { cell ->
            val designId = cell.designId ?: return@forEach
            val profile = profiles[designId] ?: return@forEach
            val weights = normalizeWeights(profile.roundWeights)
            if (weights.size != profile.roundColorIds.size) return@forEach
            contributingCells += 1
            profile.roundColorIds.zip(weights).forEach { (colorId, weight) ->
                totals[colorId] = totals.getOrDefault(colorId, 0.0) + weight
            }
        }
        val totalWeight = totals.values.sum()
        val percentages =
            if (totalWeight > 0.0) {
                totals.mapValues { (_, value) -> value * 100.0 / totalWeight }
            } else {
                emptyMap()
            }
        return ColorUsage(percentages, contributingCells)
    }
}

object YarnCalculator {
    fun estimate(
        snapshot: GridSnapshot,
        profiles: Map<String, DesignColorProfile>,
        settings: YarnSettings,
    ): YarnEstimate? {
        val skeinWeight = settings.skeinWeightGrams ?: return null
        if (!skeinWeight.isFinite() || skeinWeight <= 0.0) return null
        if (!settings.bufferPercent.isFinite() || settings.bufferPercent < 0.0) return null
        val globalGrams = settings.globalGramsPerSquare
        if (globalGrams != null && (!globalGrams.isFinite() || globalGrams <= 0.0)) return null

        var baseGrams = 0.0
        val colorBaseGrams = linkedMapOf<String, Double>()
        snapshot.cells.forEach { cell ->
            val designId = cell.designId ?: return@forEach
            val profile = profiles[designId]
            val grams =
                cell.gramsPerSquareOverride
                    ?: profile?.gramsPerSquareOverride
                    ?: globalGrams
                    ?: return null
            if (!grams.isFinite() || grams <= 0.0) return null
            baseGrams += grams

            if (profile != null) {
                val weights = ColorUsageCalculator.normalizeWeights(profile.roundWeights)
                if (weights.size == profile.roundColorIds.size) {
                    profile.roundColorIds.zip(weights).forEach { (colorId, weight) ->
                        colorBaseGrams[colorId] = colorBaseGrams.getOrDefault(colorId, 0.0) + grams * weight
                    }
                }
            }
        }

        val bufferMultiplier = 1.0 + settings.bufferPercent / 100.0
        val totalGrams = baseGrams * bufferMultiplier
        val equivalentSkeins = totalGrams / skeinWeight
        return YarnEstimate(
            baseGrams = baseGrams,
            totalGrams = totalGrams,
            equivalentSkeins = equivalentSkeins,
            recommendedWholeSkeins = ceil(equivalentSkeins).toInt(),
            colorGrams = colorBaseGrams.mapValues { (_, grams) -> grams * bufferMultiplier },
        )
    }
}
