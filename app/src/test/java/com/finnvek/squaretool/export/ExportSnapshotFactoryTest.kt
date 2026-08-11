package com.finnvek.squaretool.export

import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.ProjectCellEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.local.SquareDesignEntity
import com.finnvek.squaretool.data.local.SquareDesignWithRounds
import com.finnvek.squaretool.data.local.SquareRoundEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportSnapshotFactoryTest {
    @Test
    fun create_ordersRoundsAndMapsStoredArgbValues() {
        val project = project()
        val design = design()
        val colors =
            listOf(
                color("cream", 0xFFF3E6C9),
                color("olive", 0xFF6B8A2E),
                color("orange", 0xFFD75A1F),
            )
        val rounds =
            listOf(
                SquareRoundEntity("design", 2, "orange"),
                SquareRoundEntity("design", 0, "olive"),
                SquareRoundEntity("design", 1, "cream"),
            )

        val snapshot =
            ExportSnapshotFactory.create(
                project = project,
                cells = listOf(ProjectCellEntity("project", 0, 0, "design", false, false)),
                designs = listOf(SquareDesignWithRounds(design, rounds)),
                colors = colors,
            )

        assertEquals(
            listOf(0xFF6B8A2E.toInt(), 0xFFF3E6C9.toInt(), 0xFFD75A1F.toInt()),
            snapshot.designs.single().roundColors,
        )
        assertEquals(0xFFF3E6C9.toInt(), snapshot.colors.first { it.id == "cream" }.argb)
    }

    @Test
    fun exportColorsIncludeUsedRoundColorsOutsideProjectPalette() {
        val paletteColor = color("olive", 0xFF6B8A2E)
        val usedOutsidePalette = color("cream", 0xFFF3E6C9)
        val unrelated = color("unused", 0xFF000000)
        val rounds =
            listOf(
                SquareRoundEntity("design", 0, "olive"),
                SquareRoundEntity("design", 1, "cream"),
                SquareRoundEntity("design", 2, "olive"),
            )

        val selected =
            selectExportColors(
                projectPalette = listOf(paletteColor),
                designs = listOf(SquareDesignWithRounds(design(), rounds)),
                allColors = listOf(paletteColor, usedOutsidePalette, unrelated),
            )

        assertTrue(selected.any { it.id == "olive" })
        assertTrue(selected.any { it.id == "cream" })
        assertFalse(selected.any { it.id == "unused" })
    }

    private fun project() =
        ProjectEntity(
            id = "project",
            name = "Test",
            rowCount = 1,
            columnCount = 1,
            squareWidthValue = 15.0,
            squareHeightValue = 16.0,
            measurementUnit = "CENTIMETERS",
            joiningGapValue = 0.5,
            trackingEnabled = true,
            favorite = false,
            notes = "Notes",
            createdAt = 1,
            updatedAt = 1,
            lastOpenedAt = 1,
            generationSeed = 7,
            defaultSquareDesignId = null,
            globalGramsPerSquare = 20.0,
            skeinWeightGrams = 100.0,
            joiningAndEdgingBufferPercent = 10.0,
            demoProject = false,
        )

    private fun design() = SquareDesignEntity("design", "Olive Bloom", "classic_granny", "", false, false, "Floral", null, 1, 1)

    private fun color(
        id: String,
        argb: Long,
    ) = ColorEntity(id = id, name = id, argb = argb, builtIn = false, createdAt = 1, updatedAt = 1)
}
