package com.finnvek.squaretool.data.repository

import com.finnvek.squaretool.data.local.ProjectCellEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class GridSnapshotMappingTest {
    @Test
    fun transientCellsOutsideShrunkProjectAreIgnored() {
        val project = project(rows = 1, columns = 1)
        val snapshot =
            project.toGridSnapshot(
                listOf(
                    ProjectCellEntity("project", 0, 0, "kept", false, false),
                    ProjectCellEntity("project", 1, 1, "removed", false, false),
                ),
            )

        assertEquals(1, snapshot.cells.size)
        assertEquals("kept", snapshot.cells.single().designId)
    }

    private fun project(
        rows: Int,
        columns: Int,
    ) = ProjectEntity(
        id = "project",
        name = "Blanket",
        rowCount = rows,
        columnCount = columns,
        squareWidthValue = null,
        squareHeightValue = null,
        measurementUnit = "centimeters",
        joiningGapValue = null,
        trackingEnabled = true,
        favorite = false,
        notes = "",
        createdAt = 1,
        updatedAt = 1,
        lastOpenedAt = 1,
        generationSeed = 1,
        defaultSquareDesignId = null,
        globalGramsPerSquare = null,
        skeinWeightGrams = null,
        joiningAndEdgingBufferPercent = 10.0,
        demoProject = false,
    )
}
