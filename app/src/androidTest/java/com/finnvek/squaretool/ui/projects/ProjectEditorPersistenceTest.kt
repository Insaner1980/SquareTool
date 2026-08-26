package com.finnvek.squaretool.ui.projects

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.local.SquareDesignEntity
import com.finnvek.squaretool.data.local.SquareRoundEntity
import com.finnvek.squaretool.data.local.SquareToolDatabase
import com.finnvek.squaretool.data.repository.SquareToolRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectEditorPersistenceTest {
    @Test
    fun failedNewProjectPaletteRollsBackProjectAndInitialCells() =
        runTest {
            withRepository { repository, _ ->
                repository.saveColor(color())
                repository.saveDesign(design(), rounds())

                val result =
                    runCatching {
                        repository.saveProjectWithLayoutAndPalette(
                            project = project("new", 1, 1),
                            orderedColorIds = listOf("missing-color"),
                            initialAssignments = listOf("design"),
                        )
                    }

                assertTrue(result.isFailure)
                assertNull(repository.getProject("new"))
                assertEquals(emptyList<com.finnvek.squaretool.data.local.ProjectCellEntity>(), repository.getProjectCells("new"))
            }
        }

    @Test
    fun failedEditedProjectPaletteRollsBackResizeCellsAndPreviousPalette() =
        runTest {
            withRepository { repository, _ ->
                repository.saveColor(color())
                repository.createProject(project("saved", 2, 2))
                repository.setProjectPalette("saved", listOf("cream"))

                val result =
                    runCatching {
                        repository.saveProjectWithLayoutAndPalette(
                            project = project("saved", 1, 1),
                            orderedColorIds = listOf("missing-color"),
                            initialAssignments = null,
                        )
                    }

                assertTrue(result.isFailure)
                assertEquals(2, repository.getProject("saved")?.rowCount)
                assertEquals(2, repository.getProject("saved")?.columnCount)
                assertEquals(4, repository.getProjectCells("saved").size)
                assertEquals(listOf("cream"), repository.getProjectPalette("saved").map(ColorEntity::id))
            }
        }

    private suspend fun withRepository(block: suspend (SquareToolRepository, SquareToolDatabase) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database =
            Room
                .inMemoryDatabaseBuilder(context, SquareToolDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            block(SquareToolRepository(database), database)
        } finally {
            database.close()
        }
    }

    // CPD-OFF
    private fun project(
        id: String,
        rows: Int,
        columns: Int,
    ) = ProjectEntity(
        id = id,
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

    private fun color() =
        ColorEntity(
            id = "cream",
            name = "Cream",
            argb = 0xFFF3E6C9,
            builtIn = false,
            createdAt = 1,
            updatedAt = 1,
        )

    private fun design() =
        SquareDesignEntity(
            id = "design",
            name = "Design",
            motifTemplateId = "classic_granny",
            note = "",
            favorite = false,
            builtIn = false,
            category = "Classic",
            gramsPerSquareOverride = null,
            createdAt = 1,
            updatedAt = 1,
        )

    private fun rounds() = List(3) { index -> SquareRoundEntity("design", index, "cream") }
    // CPD-ON
}
