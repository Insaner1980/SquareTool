package com.finnvek.squaretool.ui.insights

import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.ProjectCellEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.local.SquareDesignWithRounds
import com.finnvek.squaretool.data.local.SquareRoundEntity
import com.finnvek.squaretool.domain.algorithm.ColorUsageCalculator
import com.finnvek.squaretool.domain.algorithm.DesignDistributionCalculator
import com.finnvek.squaretool.domain.algorithm.MeasurementCalculator
import com.finnvek.squaretool.domain.algorithm.ProgressCalculator
import com.finnvek.squaretool.domain.algorithm.YarnCalculator
import com.finnvek.squaretool.domain.model.BlanketDimensions
import com.finnvek.squaretool.domain.model.DesignColorProfile
import com.finnvek.squaretool.domain.model.MeasurementUnit
import com.finnvek.squaretool.domain.model.ProjectProgress
import com.finnvek.squaretool.domain.model.YarnEstimate
import com.finnvek.squaretool.domain.model.YarnSettings
import com.finnvek.squaretool.render.MotifTemplateRegistry
import com.finnvek.squaretool.ui.projects.ProjectCardModel
import com.finnvek.squaretool.ui.projects.buildProjectCardModel
import com.finnvek.squaretool.ui.projects.toSnapshot

data class DesignUsageItem(
    val designId: String,
    val name: String,
    val count: Int,
    val percentage: Double,
    val colorArgb: Int,
)

data class ColorUsageItem(
    val color: ColorEntity,
    val percentage: Double,
    val grams: Double?,
    val equivalentSkeins: Double?,
)

data class InsightsModel(
    val project: ProjectEntity,
    val preview: ProjectCardModel,
    val totalSquares: Int,
    val designCount: Int,
    val colorCount: Int,
    val progress: ProjectProgress?,
    val distribution: List<DesignUsageItem>,
    val colorUsage: List<ColorUsageItem>,
    val dimensions: BlanketDimensions?,
    val yarnEstimate: YarnEstimate?,
)

fun buildInsightsModel(
    project: ProjectEntity,
    cells: List<ProjectCellEntity>,
    designs: List<SquareDesignWithRounds>,
    colors: List<ColorEntity>,
    palette: List<ColorEntity>,
): InsightsModel {
    val snapshot = project.toSnapshot(cells)
    val colorsById = colors.associateBy(ColorEntity::id)
    val designsById = designs.associateBy { it.design.id }
    val profiles =
        designs
            .mapNotNull { relation ->
                val roundIds = relation.rounds.sortedBy(SquareRoundEntity::roundIndex).map(SquareRoundEntity::colorId)
                val weights =
                    runCatching {
                        MotifTemplateRegistry
                            .require(relation.design.motifTemplateId)
                            .areaWeights(roundIds.size)
                            .map(Float::toDouble)
                    }.getOrNull() ?: return@mapNotNull null
                relation.design.id to
                    DesignColorProfile(
                        designId = relation.design.id,
                        roundColorIds = roundIds,
                        roundWeights = weights,
                        gramsPerSquareOverride = relation.design.gramsPerSquareOverride,
                    )
            }.toMap()
    val yarn =
        YarnCalculator.estimate(
            snapshot,
            profiles,
            YarnSettings(
                globalGramsPerSquare = project.globalGramsPerSquare,
                skeinWeightGrams = project.skeinWeightGrams,
                bufferPercent = project.joiningAndEdgingBufferPercent,
            ),
        )
    val distribution = DesignDistributionCalculator.calculate(snapshot)
    val assignedTotal = distribution.assignedCount.coerceAtLeast(1)
    val designItems =
        distribution.designCounts
            .mapNotNull { (designId, count) ->
                val relation = designsById[designId] ?: return@mapNotNull null
                val firstColor =
                    relation.rounds
                        .sortedBy(SquareRoundEntity::roundIndex)
                        .firstNotNullOfOrNull { colorsById[it.colorId] }
                DesignUsageItem(
                    designId = designId,
                    name = relation.design.name,
                    count = count,
                    percentage = count * 100.0 / assignedTotal,
                    colorArgb = firstColor?.argb?.toInt() ?: 0xFF6B8A2E.toInt(),
                )
            }.sortedByDescending(DesignUsageItem::count)
    val usage = ColorUsageCalculator.calculate(snapshot, profiles)
    val colorItems =
        usage.percentages
            .mapNotNull { (colorId, percentage) ->
                val color = colorsById[colorId] ?: return@mapNotNull null
                val grams = yarn?.colorGrams?.get(colorId)
                ColorUsageItem(
                    color = color,
                    percentage = percentage,
                    grams = grams,
                    equivalentSkeins =
                        grams?.let { value ->
                            project.skeinWeightGrams?.takeIf { it > 0.0 }?.let(value::div)
                        },
                )
            }.sortedByDescending(ColorUsageItem::percentage)
    val unit =
        if (project.measurementUnit.startsWith("inch", ignoreCase = true)) {
            MeasurementUnit.INCHES
        } else {
            MeasurementUnit.CENTIMETERS
        }
    return InsightsModel(
        project = project,
        preview = buildProjectCardModel(project, cells, designs, colors, palette),
        totalSquares = project.rowCount * project.columnCount,
        designCount = distribution.designCounts.size,
        colorCount = colorItems.size,
        progress = ProgressCalculator.calculate(snapshot, project.trackingEnabled),
        distribution = designItems,
        colorUsage = colorItems,
        dimensions =
            MeasurementCalculator.blanketDimensions(
                size = snapshot.size,
                squareWidth = project.squareWidthValue,
                squareHeight = project.squareHeightValue,
                joiningGap = project.joiningGapValue,
                unit = unit,
            ),
        yarnEstimate = yarn,
    )
}
