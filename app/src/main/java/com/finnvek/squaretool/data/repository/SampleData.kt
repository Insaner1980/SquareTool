package com.finnvek.squaretool.data.repository

import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.PaletteColorCrossRef
import com.finnvek.squaretool.data.local.PaletteEntity
import com.finnvek.squaretool.data.local.ProjectCellEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.local.ProjectPaletteCrossRef
import com.finnvek.squaretool.data.local.SquareDesignEntity
import com.finnvek.squaretool.data.local.SquareRoundEntity

internal data class SampleData(
    val project: ProjectEntity,
    val colors: List<ColorEntity>,
    val designs: List<Pair<SquareDesignEntity, List<SquareRoundEntity>>>,
    val palette: PaletteEntity,
    val paletteColors: List<PaletteColorCrossRef>,
    val projectColors: List<ProjectPaletteCrossRef>,
    val cells: List<ProjectCellEntity>,
)

internal object SampleDataFactory {
    const val PROJECT_ID = "sample-project-autumn-garden"
    private const val PALETTE_ID = "sample-palette-autumn-garden"
    private const val MOSS_GREEN = "Moss Green"

    fun create(now: Long): SampleData {
        val colors =
            listOf(
                color("cream", "Cream", 0xFFF3E6C9, now),
                color("mustard", "Mustard", 0xFFD99A1E, now),
                color("rust", "Rust", 0xFFC9531A, now),
                color("moss", MOSS_GREEN, 0xFF6B7A2C, now),
                color("sage", "Sage", 0xFF9BA77A, now),
                color("blush", "Blush", 0xFFD98F8E, now),
                color("chocolate", "Chocolate", 0xFF4B2D18, now),
            )
        val ids = colors.associate { it.name to it.id }
        val designs =
            listOf(
                design("sunburst", "Sunburst", "sunburst", listOf("Mustard", "Cream", "Rust", MOSS_GREEN, "Chocolate"), ids, now),
                design(
                    "olive-bloom",
                    "Olive Bloom",
                    "flower_medallion",
                    listOf(MOSS_GREEN, "Cream", "Blush", "Sage", "Chocolate"),
                    ids,
                    now,
                ),
                design(
                    "harvest-star",
                    "Harvest Star",
                    "star_bloom",
                    listOf(MOSS_GREEN, "Mustard", "Cream", "Rust", "Chocolate", "Cream"),
                    ids,
                    now,
                ),
                design("soft-daisy", "Soft Daisy", "daisy", listOf("Cream", "Mustard", "Blush", "Cream", "Sage"), ids, now),
                design(
                    "maple-mist",
                    "Maple Mist",
                    "classic_granny",
                    listOf("Rust", "Cream", MOSS_GREEN, "Mustard", "Sage", "Chocolate"),
                    ids,
                    now,
                ),
                design(
                    "woodland-petal",
                    "Woodland Petal",
                    "corner_accent",
                    listOf("Cream", "Sage", MOSS_GREEN, "Mustard", "Chocolate"),
                    ids,
                    now,
                ),
            )
        val project =
            ProjectEntity(
                id = PROJECT_ID,
                name = "Autumn Garden Blanket",
                rowCount = 12,
                columnCount = 8,
                squareWidthValue = 8.0,
                squareHeightValue = 8.0,
                measurementUnit = "inches",
                joiningGapValue = 0.0,
                trackingEnabled = true,
                favorite = true,
                notes = "Editable sample project",
                createdAt = now,
                updatedAt = now,
                lastOpenedAt = now,
                generationSeed = 2_026_081_1,
                defaultSquareDesignId = designs.first().first.id,
                globalGramsPerSquare = 23.2,
                skeinWeightGrams = 100.0,
                joiningAndEdgingBufferPercent = 10.0,
                demoProject = true,
            )
        val lockedIndices = setOf(0, 9, 18, 27, 36, 45, 54, 63, 72, 81)
        val designIds = designs.map { it.first.id }
        val cells =
            List(96) { index ->
                ProjectCellEntity(
                    projectId = PROJECT_ID,
                    rowIndex = index / 8,
                    columnIndex = index % 8,
                    squareDesignId = designIds[(index * 5 + index / 8 + 3) % designIds.size],
                    locked = index in lockedIndices,
                    completed = index < 69,
                )
            }
        val palette = PaletteEntity(PALETTE_ID, "Autumn Garden", builtIn = false, now, now)
        return SampleData(
            project = project,
            colors = colors,
            designs = designs,
            palette = palette,
            paletteColors =
                colors.mapIndexed { index, color ->
                    PaletteColorCrossRef(PALETTE_ID, color.id, index)
                },
            projectColors =
                colors.mapIndexed { index, color ->
                    ProjectPaletteCrossRef(PROJECT_ID, color.id, index)
                },
            cells = cells,
        )
    }

    private fun color(
        slug: String,
        name: String,
        argb: Long,
        now: Long,
    ) = ColorEntity(
        id = "sample-color-$slug",
        name = name,
        argb = argb,
        builtIn = false,
        createdAt = now,
        updatedAt = now,
    )

    private fun design(
        slug: String,
        name: String,
        templateId: String,
        colorNames: List<String>,
        colorIds: Map<String, String>,
        now: Long,
    ): Pair<SquareDesignEntity, List<SquareRoundEntity>> {
        val id = "sample-design-$slug"
        val entity =
            SquareDesignEntity(
                id = id,
                name = name,
                motifTemplateId = templateId,
                note = "",
                favorite = slug == "olive-bloom",
                builtIn = false,
                category = "Autumn",
                gramsPerSquareOverride = null,
                createdAt = now,
                updatedAt = now,
            )
        val rounds =
            colorNames.mapIndexed { index, colorName ->
                SquareRoundEntity(id, index, colorIds.getValue(colorName))
            }
        return entity to rounds
    }
}
