package com.finnvek.squaretool.export

import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.ProjectCellEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.local.SquareDesignWithRounds
import com.finnvek.squaretool.data.repository.SquareToolRepository
import com.finnvek.squaretool.render.MotifTemplateRegistry

object ExportSnapshotFactory {
    private const val MISSING_COLOR_ARGB = 0xFF8C8C80.toInt()

    suspend fun create(
        repository: SquareToolRepository,
        projectId: String,
    ): ProjectExportSnapshot {
        val project = requireNotNull(repository.getProject(projectId)) { "Project $projectId does not exist" }
        val cells = repository.getProjectCells(projectId)
        val usedDesignIds = cells.mapNotNullTo(linkedSetOf(), ProjectCellEntity::squareDesignId)
        val designs = repository.getDesignsWithRounds().filter { it.design.id in usedDesignIds }
        return create(
            project = project,
            cells = cells,
            designs = designs,
            colors =
                selectExportColors(
                    projectPalette = repository.getProjectPalette(projectId),
                    designs = designs,
                    allColors = repository.getColors(),
                ),
        )
    }

    fun create(
        project: ProjectEntity,
        cells: List<ProjectCellEntity>,
        designs: List<SquareDesignWithRounds>,
        colors: List<ColorEntity>,
    ): ProjectExportSnapshot {
        val colorsById = colors.associateBy(ColorEntity::id)
        val exportDesigns =
            designs.mapNotNull { designWithRounds ->
                val orderedRounds = designWithRounds.rounds.sortedBy { it.roundIndex }
                val template = MotifTemplateRegistry.find(designWithRounds.design.motifTemplateId) ?: return@mapNotNull null
                if (!template.supportsRoundCount(orderedRounds.size)) return@mapNotNull null
                ExportDesign(
                    id = designWithRounds.design.id,
                    name = designWithRounds.design.name,
                    templateId = designWithRounds.design.motifTemplateId,
                    roundColors = orderedRounds.map { colorsById[it.colorId]?.argb?.toInt() ?: MISSING_COLOR_ARGB },
                    gramsPerSquareOverride = designWithRounds.design.gramsPerSquareOverride,
                    notes = designWithRounds.design.note,
                    roundColorIds = orderedRounds.map { it.colorId },
                )
            }
        return ProjectExportSnapshot(
            project =
                ExportProject(
                    id = project.id,
                    name = project.name,
                    notes = project.notes,
                    rows = project.rowCount,
                    columns = project.columnCount,
                    squareWidth = project.squareWidthValue,
                    squareHeight = project.squareHeightValue,
                    joiningGap = project.joiningGapValue,
                    measurementUnit = project.measurementUnit,
                    trackingEnabled = project.trackingEnabled,
                    globalGramsPerSquare = project.globalGramsPerSquare,
                    skeinWeightGrams = project.skeinWeightGrams,
                    bufferPercent = project.joiningAndEdgingBufferPercent,
                ),
            designs = exportDesigns,
            colors =
                colors.map { color ->
                    ExportColor(
                        id = color.id,
                        name = color.name,
                        argb = color.argb.toInt(),
                        yarnBrand = color.yarnBrand,
                        yarnLine = color.yarnLine,
                        shadeName = color.shadeName,
                        shadeCode = color.shadeCode,
                        skeinWeightGrams = color.skeinWeightGrams,
                    )
                },
            cells =
                cells.map { cell ->
                    ExportCell(
                        row = cell.rowIndex,
                        column = cell.columnIndex,
                        designId = cell.squareDesignId,
                        locked = cell.locked,
                        completed = cell.completed,
                        gramsPerSquareOverride = cell.gramsPerSquareOverride,
                    )
                },
        )
    }
}

internal fun selectExportColors(
    projectPalette: List<ColorEntity>,
    designs: List<SquareDesignWithRounds>,
    allColors: List<ColorEntity>,
): List<ColorEntity> {
    val includedIds = linkedSetOf<String>()
    projectPalette.mapTo(includedIds, ColorEntity::id)
    designs.flatMapTo(includedIds) { design ->
        design.rounds.sortedBy { it.roundIndex }.map { it.colorId }
    }
    val colorsById = allColors.associateBy(ColorEntity::id)
    return includedIds.mapNotNull(colorsById::get)
}
