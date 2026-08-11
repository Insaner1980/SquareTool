package com.finnvek.squaretool.ui.insights

import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.ProjectCellEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.local.SquareDesignEntity
import com.finnvek.squaretool.data.local.SquareDesignWithRounds
import com.finnvek.squaretool.data.local.SquareRoundEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsModelsTest {
    @Test
    fun `insights use real cells motif weights dimensions yarn settings and progress`() {
        val project = project(tracking = true, measurements = true, yarn = true)
        val colors = listOf(color("a", "Cream", 0xFFF3E6C9), color("b", "Moss", 0xFF6B7A2C))
        val design =
            SquareDesignEntity(
                id = "classic",
                name = "Classic",
                motifTemplateId = "classic_granny",
                note = "",
                favorite = false,
                builtIn = false,
                category = "Classic",
                gramsPerSquareOverride = null,
                createdAt = 1,
                updatedAt = 1,
            )
        val relation =
            SquareDesignWithRounds(
                design,
                listOf(
                    SquareRoundEntity("classic", 0, "a"),
                    SquareRoundEntity("classic", 1, "b"),
                    SquareRoundEntity("classic", 2, "a"),
                ),
            )
        val cells = listOf(cell(0, completed = true), cell(1, completed = false))

        val insights = buildInsightsModel(project, cells, listOf(relation), colors, colors)

        assertEquals(2, insights.totalSquares)
        assertEquals(1, insights.designCount)
        assertEquals(2, insights.colorCount)
        assertEquals(50, insights.progress?.percentage)
        assertEquals(21.0, insights.dimensions?.width ?: 0.0, 0.0001)
        assertEquals(12.0, insights.dimensions?.height ?: 0.0, 0.0001)
        assertEquals(0.88, insights.yarnEstimate?.equivalentSkeins ?: 0.0, 0.0001)
        assertEquals(1, insights.yarnEstimate?.recommendedWholeSkeins)
        assertEquals(86.0, insights.colorUsage.first { it.color.id == "a" }.percentage, 0.01)
        assertEquals(14.0, insights.colorUsage.first { it.color.id == "b" }.percentage, 0.01)
        assertEquals(100.0, insights.colorUsage.sumOf { it.percentage }, 0.01)
        assertEquals(2, insights.distribution.single().count)
    }

    @Test
    fun `tracking measurements and yarn sections stay absent when not configured`() {
        val insights =
            buildInsightsModel(
                project = project(tracking = false, measurements = false, yarn = false),
                cells = listOf(cell(0, completed = true), cell(1, completed = false)),
                designs = emptyList(),
                colors = emptyList(),
                palette = emptyList(),
            )

        assertNull(insights.progress)
        assertNull(insights.dimensions)
        assertNull(insights.yarnEstimate)
        assertTrue(insights.distribution.isEmpty())
        assertTrue(insights.colorUsage.isEmpty())
        assertNotNull(insights.project)
    }

    private fun project(
        tracking: Boolean,
        measurements: Boolean,
        yarn: Boolean,
    ) = ProjectEntity(
        id = "p",
        name = "Blanket",
        rowCount = 1,
        columnCount = 2,
        squareWidthValue = if (measurements) 10.0 else null,
        squareHeightValue = if (measurements) 12.0 else null,
        measurementUnit = "centimeters",
        joiningGapValue = if (measurements) 1.0 else null,
        trackingEnabled = tracking,
        favorite = false,
        notes = "",
        createdAt = 1,
        updatedAt = 1,
        lastOpenedAt = 1,
        generationSeed = 1,
        defaultSquareDesignId = null,
        globalGramsPerSquare = if (yarn) 20.0 else null,
        skeinWeightGrams = if (yarn) 50.0 else null,
        joiningAndEdgingBufferPercent = 10.0,
        demoProject = false,
    )

    private fun color(
        id: String,
        name: String,
        argb: Long,
    ) = ColorEntity(
        id = id,
        name = name,
        argb = argb,
        builtIn = false,
        createdAt = 1,
        updatedAt = 1,
    )

    private fun cell(
        column: Int,
        completed: Boolean,
    ) = ProjectCellEntity(
        projectId = "p",
        rowIndex = 0,
        columnIndex = column,
        squareDesignId = "classic",
        locked = false,
        completed = completed,
    )
}
