package com.finnvek.squaretool.ui.projects

import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.ProjectCellEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.local.SquareDesignWithRounds
import com.finnvek.squaretool.data.local.SquareRoundEntity
import com.finnvek.squaretool.data.repository.AppSettings
import com.finnvek.squaretool.data.repository.MeasurementUnitPreference
import com.finnvek.squaretool.data.repository.ProjectCardData
import com.finnvek.squaretool.domain.algorithm.ProgressCalculator
import com.finnvek.squaretool.domain.algorithm.YarnCalculator
import com.finnvek.squaretool.domain.model.CellCoordinate
import com.finnvek.squaretool.domain.model.CellState
import com.finnvek.squaretool.domain.model.DesignColorProfile
import com.finnvek.squaretool.domain.model.GridSize
import com.finnvek.squaretool.domain.model.GridSnapshot
import com.finnvek.squaretool.domain.model.MeasurementUnit
import com.finnvek.squaretool.domain.model.ProjectProgress
import com.finnvek.squaretool.domain.model.YarnEstimate
import com.finnvek.squaretool.domain.model.YarnSettings
import com.finnvek.squaretool.render.MotifTemplateRegistry
import java.util.Locale

data class ProjectDesignVisual(
    val id: String,
    val name: String,
    val templateId: String,
    val roundColors: List<Int>,
)

data class ProjectCardModel(
    val project: ProjectEntity,
    val cells: List<ProjectCellEntity> = emptyList(),
    val designs: Map<String, ProjectDesignVisual> = emptyMap(),
    val palette: List<ColorEntity> = emptyList(),
    val progress: ProjectProgress? = null,
    val yarnEstimate: YarnEstimate? = null,
) {
    val totalSquares: Int get() = project.rowCount * project.columnCount
}

fun buildProjectCardModel(
    project: ProjectEntity,
    cells: List<ProjectCellEntity>,
    designs: List<SquareDesignWithRounds>,
    colors: List<ColorEntity>,
    palette: List<ColorEntity>,
): ProjectCardModel {
    val boundedCells =
        cells.filter {
            it.rowIndex in 0 until project.rowCount && it.columnIndex in 0 until project.columnCount
        }
    val colorsById = colors.associateBy(ColorEntity::id)
    val visuals =
        designs.associate { relation ->
            relation.design.id to
                ProjectDesignVisual(
                    id = relation.design.id,
                    name = relation.design.name,
                    templateId = relation.design.motifTemplateId,
                    roundColors =
                        relation.rounds
                            .sortedBy(SquareRoundEntity::roundIndex)
                            .mapNotNull { colorsById[it.colorId]?.argb?.toInt() },
                )
        }
    val snapshot = project.toSnapshot(boundedCells)
    val profiles =
        designs
            .mapNotNull { relation ->
                val colorIds = relation.rounds.sortedBy(SquareRoundEntity::roundIndex).map(SquareRoundEntity::colorId)
                val weights =
                    runCatching {
                        MotifTemplateRegistry
                            .require(relation.design.motifTemplateId)
                            .areaWeights(colorIds.size)
                            .map(Float::toDouble)
                    }.getOrNull() ?: return@mapNotNull null
                relation.design.id to
                    DesignColorProfile(
                        designId = relation.design.id,
                        roundColorIds = colorIds,
                        roundWeights = weights,
                        gramsPerSquareOverride = relation.design.gramsPerSquareOverride,
                    )
            }.toMap()
    return ProjectCardModel(
        project = project,
        cells = boundedCells,
        designs = visuals,
        palette = palette,
        progress = ProgressCalculator.calculate(snapshot, project.trackingEnabled),
        yarnEstimate =
            YarnCalculator.estimate(
                snapshot,
                profiles,
                YarnSettings(
                    globalGramsPerSquare = project.globalGramsPerSquare,
                    skeinWeightGrams = project.skeinWeightGrams,
                    bufferPercent = project.joiningAndEdgingBufferPercent,
                ),
            ),
    )
}

enum class ProjectSort { RECENT, ALPHABETICAL }

fun filterAndSortProjectCards(
    cards: List<ProjectCardModel>,
    query: String,
    favoriteOnly: Boolean,
    sort: ProjectSort,
): List<ProjectCardModel> {
    val normalized = query.trim()
    return cards
        .asSequence()
        .filter { !favoriteOnly || it.project.favorite }
        .filter {
            normalized.isEmpty() ||
                it.project.name.contains(normalized, ignoreCase = true) ||
                it.project.notes.contains(normalized, ignoreCase = true)
        }.let { sequence ->
            when (sort) {
                ProjectSort.RECENT -> {
                    sequence.sortedWith(
                        compareByDescending<ProjectCardModel> { it.project.updatedAt }
                            .thenBy { it.project.name.lowercase() },
                    )
                }

                ProjectSort.ALPHABETICAL -> {
                    sequence.sortedBy { it.project.name.lowercase() }
                }
            }
        }.toList()
}

enum class InitialProjectFill { BLANK, FILL_ONE, BALANCED }

enum class ProjectDraftError {
    NAME,
    ROWS,
    COLUMNS,
    SQUARE_WIDTH,
    SQUARE_HEIGHT,
    JOINING_GAP,
    DESIGNS,
    GRAMS_PER_SQUARE,
    SKEIN_WEIGHT,
    BUFFER_PERCENT,
}

data class ProjectEditorDraft(
    val id: String,
    val name: String,
    val rows: Int,
    val columns: Int,
    val measurementUnit: MeasurementUnit,
    val squareWidth: String,
    val squareHeight: String,
    val joiningGap: String,
    val trackingEnabled: Boolean,
    val selectedColorIds: Set<String>,
    val initialFill: InitialProjectFill,
    val selectedDesignIds: Set<String>,
    val globalGramsPerSquare: String,
    val skeinWeightGrams: String,
    val bufferPercent: String,
    val notes: String,
)

fun initialProjectEditorDraft(
    project: ProjectEntity?,
    selectedColorIds: Set<String>,
    settings: AppSettings,
    newProjectId: String,
    locale: Locale = Locale.getDefault(),
): ProjectEditorDraft {
    if (project != null) {
        return ProjectEditorDraft(
            id = project.id,
            name = project.name,
            rows = project.rowCount,
            columns = project.columnCount,
            measurementUnit =
                if (project.measurementUnit.startsWith("inch", ignoreCase = true)) {
                    MeasurementUnit.INCHES
                } else {
                    MeasurementUnit.CENTIMETERS
                },
            squareWidth = project.squareWidthValue?.toString().orEmpty(),
            squareHeight = project.squareHeightValue?.toString().orEmpty(),
            joiningGap = project.joiningGapValue?.toString().orEmpty(),
            trackingEnabled = project.trackingEnabled,
            selectedColorIds = selectedColorIds,
            initialFill = InitialProjectFill.BLANK,
            selectedDesignIds = project.defaultSquareDesignId?.let(::setOf).orEmpty(),
            globalGramsPerSquare = project.globalGramsPerSquare?.toString().orEmpty(),
            skeinWeightGrams = project.skeinWeightGrams?.toString().orEmpty(),
            bufferPercent = project.joiningAndEdgingBufferPercent.toString(),
            notes = project.notes,
        )
    }
    val unit =
        when (settings.preferredMeasurementUnit) {
            MeasurementUnitPreference.CENTIMETERS -> {
                MeasurementUnit.CENTIMETERS
            }

            MeasurementUnitPreference.INCHES -> {
                MeasurementUnit.INCHES
            }

            MeasurementUnitPreference.AUTOMATIC -> {
                if (locale.country.uppercase() in INCH_BASED_COUNTRIES) {
                    MeasurementUnit.INCHES
                } else {
                    MeasurementUnit.CENTIMETERS
                }
            }
        }
    return ProjectEditorDraft(
        id = newProjectId,
        name = "",
        rows = 8,
        columns = 12,
        measurementUnit = unit,
        squareWidth = "",
        squareHeight = "",
        joiningGap = "",
        trackingEnabled = true,
        selectedColorIds = selectedColorIds,
        initialFill = InitialProjectFill.BLANK,
        selectedDesignIds = emptySet(),
        globalGramsPerSquare = "",
        skeinWeightGrams = settings.defaultSkeinWeightGrams.toString(),
        bufferPercent = settings.defaultJoiningAndEdgingBufferPercent.toString(),
        notes = "",
    )
}

private val INCH_BASED_COUNTRIES = setOf("US", "LR", "MM")

fun ProjectEditorDraft.validationErrors(): Set<ProjectDraftError> =
    buildSet {
        if (name.isBlank()) add(ProjectDraftError.NAME)
        if (rows !in GridSize.MIN_DIMENSION..GridSize.MAX_DIMENSION) add(ProjectDraftError.ROWS)
        if (columns !in GridSize.MIN_DIMENSION..GridSize.MAX_DIMENSION) add(ProjectDraftError.COLUMNS)
        if (!squareWidth.isBlankOrPositive()) add(ProjectDraftError.SQUARE_WIDTH)
        if (!squareHeight.isBlankOrPositive()) add(ProjectDraftError.SQUARE_HEIGHT)
        if (!joiningGap.isBlankOrNonNegative()) add(ProjectDraftError.JOINING_GAP)
        if (initialFill != InitialProjectFill.BLANK && selectedDesignIds.isEmpty()) add(ProjectDraftError.DESIGNS)
        if (!globalGramsPerSquare.isBlankOrPositive()) add(ProjectDraftError.GRAMS_PER_SQUARE)
        if (!skeinWeightGrams.isBlankOrPositive()) add(ProjectDraftError.SKEIN_WEIGHT)
        val buffer = bufferPercent.toDoubleOrNull()
        if (buffer == null || !buffer.isFinite() || buffer !in 0.0..100.0) add(ProjectDraftError.BUFFER_PERCENT)
    }

fun ProjectEditorDraft.initialAssignments(): List<String?> {
    val count = rows.coerceAtLeast(0) * columns.coerceAtLeast(0)
    val designs = selectedDesignIds.sorted()
    return when {
        initialFill == InitialProjectFill.BLANK || designs.isEmpty() -> List(count) { null }
        initialFill == InitialProjectFill.FILL_ONE -> List(count) { designs.first() }
        else -> List(count) { index -> designs[index % designs.size] }
    }
}

data class ShrinkImpact(
    val lostCellCount: Int,
    val lostAssignedCellCount: Int,
)

fun calculateShrinkImpact(
    cells: List<ProjectCellEntity>,
    newRows: Int,
    newColumns: Int,
): ShrinkImpact {
    val lost = cells.filter { it.rowIndex >= newRows || it.columnIndex >= newColumns }
    return ShrinkImpact(
        lostCellCount = lost.size,
        lostAssignedCellCount = lost.count { it.squareDesignId != null },
    )
}

internal fun ProjectEntity.toSnapshot(cells: List<ProjectCellEntity>): GridSnapshot =
    GridSnapshot.of(
        size = GridSize(rowCount, columnCount),
        cells =
            cells.map {
                CellState(
                    coordinate = CellCoordinate(it.rowIndex, it.columnIndex),
                    designId = it.squareDesignId,
                    locked = it.locked,
                    completed = it.completed,
                    gramsPerSquareOverride = it.gramsPerSquareOverride,
                )
            },
    )

private fun String.isBlankOrPositive(): Boolean {
    if (isBlank()) return true
    val value = toDoubleOrNull() ?: return false
    return value.isFinite() && value > 0.0
}

private fun String.isBlankOrNonNegative(): Boolean {
    if (isBlank()) return true
    val value = toDoubleOrNull() ?: return false
    return value.isFinite() && value >= 0.0
}

fun buildProjectCardModels(data: ProjectCardData): List<ProjectCardModel> {
    val cellsByProject = data.cells.groupBy(ProjectCellEntity::projectId)
    val colorsById = data.colors.associateBy(ColorEntity::id)
    val paletteByProject =
        data.projectPaletteRefs
            .groupBy { it.projectId }
            .mapValues { (_, refs) ->
                refs.sortedBy { it.displayOrder }.mapNotNull { colorsById[it.colorId] }
            }
    return data.projects.map { project ->
        buildProjectCardModel(
            project = project,
            cells = cellsByProject[project.id].orEmpty(),
            designs = data.designs,
            colors = data.colors,
            palette = paletteByProject[project.id].orEmpty(),
        )
    }
}
