package com.finnvek.squaretool.export

import com.finnvek.squaretool.domain.algorithm.ColorUsageCalculator
import com.finnvek.squaretool.domain.algorithm.YarnCalculator
import com.finnvek.squaretool.domain.model.CellCoordinate
import com.finnvek.squaretool.domain.model.CellState
import com.finnvek.squaretool.domain.model.DesignColorProfile
import com.finnvek.squaretool.domain.model.GridSize
import com.finnvek.squaretool.domain.model.GridSnapshot
import com.finnvek.squaretool.domain.model.YarnEstimate
import com.finnvek.squaretool.domain.model.YarnSettings
import com.finnvek.squaretool.render.MotifTemplateRegistry
import com.finnvek.squaretool.render.SquareDesignVisual

data class ExportProject(
    val name: String,
    val notes: String,
    val rows: Int,
    val columns: Int,
    val squareWidth: Double?,
    val squareHeight: Double?,
    val joiningGap: Double?,
    val measurementUnit: String,
    val trackingEnabled: Boolean,
    val globalGramsPerSquare: Double?,
    val skeinWeightGrams: Double?,
    val bufferPercent: Double,
    val id: String = "",
)

data class ExportDesign(
    val id: String,
    val name: String,
    val templateId: String,
    val roundColors: List<Int>,
    val gramsPerSquareOverride: Double?,
    val notes: String = "",
    val roundColorIds: List<String> = emptyList(),
)

data class ExportColor(
    val id: String,
    val name: String,
    val argb: Int,
    val yarnBrand: String? = null,
    val yarnLine: String? = null,
    val shadeName: String? = null,
    val shadeCode: String? = null,
    val skeinWeightGrams: Double? = null,
)

data class ExportCell(
    val row: Int,
    val column: Int,
    val designId: String?,
    val locked: Boolean,
    val completed: Boolean,
    val gramsPerSquareOverride: Double? = null,
)

data class ExportLegendEntry(
    val code: String,
    val design: ExportDesign,
    val count: Int,
)

data class ExportMaterialsSummary(
    val colorUsagePercentages: Map<String, Double>,
    val yarnEstimate: YarnEstimate?,
)

data class ProjectExportSnapshot(
    val project: ExportProject,
    val designs: List<ExportDesign>,
    val colors: List<ExportColor>,
    val cells: List<ExportCell>,
) {
    private val designsById = designs.associateBy(ExportDesign::id)

    fun legendEntries(): List<ExportLegendEntry> {
        val counts = cells.mapNotNull(ExportCell::designId).groupingBy { it }.eachCount()
        return designs.mapIndexedNotNull { index, design ->
            counts[design.id]?.takeIf { it > 0 }?.let { count ->
                ExportLegendEntry(legendCode(index), design, count)
            }
        }
    }

    fun visualForCell(cell: ExportCell): SquareDesignVisual? {
        val design = cell.designId?.let(designsById::get) ?: return null
        return SquareDesignVisual(design.templateId, design.roundColors)
    }

    fun materialsSummary(): ExportMaterialsSummary {
        val grid =
            GridSnapshot.of(
                GridSize(project.rows, project.columns),
                cells.map { cell ->
                    CellState(
                        coordinate = CellCoordinate(cell.row, cell.column),
                        designId = cell.designId,
                        locked = cell.locked,
                        completed = cell.completed,
                        gramsPerSquareOverride = cell.gramsPerSquareOverride,
                    )
                },
            )
        val profiles =
            designs.associate { design ->
                val template = MotifTemplateRegistry.find(design.templateId)
                design.id to
                    DesignColorProfile(
                        designId = design.id,
                        roundColorIds = design.roundColorIds,
                        roundWeights =
                            template
                                ?.areaWeights(design.roundColors.size)
                                ?.map(Float::toDouble)
                                .orEmpty(),
                        gramsPerSquareOverride = design.gramsPerSquareOverride,
                    )
            }
        return ExportMaterialsSummary(
            colorUsagePercentages = ColorUsageCalculator.calculate(grid, profiles).percentages,
            yarnEstimate =
                YarnCalculator.estimate(
                    grid,
                    profiles,
                    YarnSettings(
                        globalGramsPerSquare = project.globalGramsPerSquare,
                        skeinWeightGrams = project.skeinWeightGrams,
                        bufferPercent = project.bufferPercent,
                    ),
                ),
        )
    }
}

fun legendCode(index: Int): String {
    require(index >= 0)
    var value = index
    return buildString {
        do {
            insert(0, ('A'.code + value % 26).toChar())
            value = value / 26 - 1
        } while (value >= 0)
    }
}
