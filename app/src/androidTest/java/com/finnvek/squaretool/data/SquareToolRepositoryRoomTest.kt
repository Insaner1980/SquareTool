package com.finnvek.squaretool.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.PaletteColorCrossRef
import com.finnvek.squaretool.data.local.PaletteEntity
import com.finnvek.squaretool.data.local.ProjectCellEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.local.SquareDesignEntity
import com.finnvek.squaretool.data.local.SquareRoundEntity
import com.finnvek.squaretool.data.local.SquareToolDatabase
import com.finnvek.squaretool.data.repository.SquareToolRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SquareToolRepositoryRoomTest {
    private lateinit var database: SquareToolDatabase
    private lateinit var repository: SquareToolRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, SquareToolDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = SquareToolRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun projectInsertCreatesRetrievableBlankGrid() =
        runTest {
            repository.createProject(project(rows = 2, columns = 3))

            assertEquals("Blanket", repository.getProject("project-1")?.name)
            assertEquals(6, repository.getProjectCells("project-1").size)
        }

    @Test
    fun bulkCellUpdatePersistsEveryAssignment() =
        runTest {
            repository.saveColor(color("color-1", "Cream"))
            repository.saveDesign(design(), rounds())
            repository.createProject(project(rows = 1, columns = 2))

            repository.saveCells(
                listOf(
                    ProjectCellEntity("project-1", 0, 0, "design-1", locked = true, completed = false),
                    ProjectCellEntity("project-1", 0, 1, "design-1", locked = false, completed = true),
                ),
            )

            val cells = repository.getProjectCells("project-1")
            assertEquals(listOf("design-1", "design-1"), cells.map { it.squareDesignId })
            assertEquals(1, cells.count { it.locked })
            assertEquals(1, cells.count { it.completed })
        }

    @Test
    fun projectDeleteCascadesCellsWithoutDeletingSharedDesign() =
        runTest {
            repository.saveColor(color("color-1", "Cream"))
            repository.saveDesign(design(), rounds())
            repository.createProject(project(rows = 1, columns = 1))
            repository.saveCells(listOf(ProjectCellEntity("project-1", 0, 0, "design-1", false, false)))

            repository.deleteProject("project-1")

            assertEquals(emptyList<ProjectCellEntity>(), repository.getProjectCells("project-1"))
            assertEquals("Design", repository.getDesign("design-1")?.name)
        }

    @Test
    fun referencedDesignCannotBeDeletedSilently() =
        runTest {
            repository.saveColor(color("color-1", "Cream"))
            repository.saveDesign(design(), rounds())
            repository.createProject(project(rows = 1, columns = 1))
            repository.saveCells(listOf(ProjectCellEntity("project-1", 0, 0, "design-1", false, false)))

            val deleted = repository.deleteDesignIfUnused("design-1")

            assertEquals(false, deleted)
            assertEquals("design-1", repository.getProjectCells("project-1").single().squareDesignId)
        }

    @Test
    fun paletteColorsAreReturnedInDisplayOrder() =
        runTest {
            val first = color("color-1", "First")
            val second = color("color-2", "Second")
            repository.saveColor(first)
            repository.saveColor(second)

            repository.savePalette(
                PaletteEntity("palette-1", "Palette", false, 1, 1),
                listOf(
                    PaletteColorCrossRef("palette-1", "color-1", 1),
                    PaletteColorCrossRef("palette-1", "color-2", 0),
                ),
            )

            assertEquals(listOf("Second", "First"), repository.getPaletteColors("palette-1").map { it.name })
        }

    @Test
    fun validRestoreReplacesExistingDataInOneOperation() =
        runTest {
            repository.createProject(project(rows = 1, columns = 1))
            val original = repository.createBackup(exportedAtEpochMillis = 500)
            val backup = original.copy(projects = listOf(original.projects.single().copy(name = "Restored")))

            repository.restoreBackup(backup)

            assertEquals(listOf("Restored"), repository.getProjects().map { it.name })
            assertEquals(1, repository.getProjectCells("project-1").size)
        }

    @Test
    fun failedRestoreLeavesExistingDatabaseUnchanged() =
        runTest {
            repository.createProject(project(rows = 1, columns = 1))
            val original = repository.createBackup(exportedAtEpochMillis = 500)
            val valid =
                original.copy(
                    projects = listOf(original.projects.single().copy(name = "Restored")),
                )
            database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER fail_restored_project
                BEFORE INSERT ON projects
                WHEN NEW.name = 'Restored'
                BEGIN
                    SELECT RAISE(ABORT, 'forced restore failure');
                END
                """.trimIndent(),
            )

            try {
                repository.restoreBackup(valid)
                throw AssertionError("Restore unexpectedly succeeded")
            } catch (_: Exception) {
                // The trigger forces an insert failure after the transaction has cleared the tables.
            }

            assertEquals(listOf("Blanket"), repository.getProjects().map { it.name })
            assertEquals(1, repository.getProjectCells("project-1").size)
        }

    @Test
    fun duplicateProjectCopiesCellsAndProjectPaletteUnderNewId() =
        runTest {
            repository.saveColor(color("color-1", "Cream"))
            repository.saveDesign(design(), rounds())
            repository.createProject(project(rows = 1, columns = 1))
            repository.setProjectPalette("project-1", listOf("color-1"))
            repository.saveCell(ProjectCellEntity("project-1", 0, 0, "design-1", true, true))

            val duplicate = repository.duplicateProject("project-1", "project-2", "Blanket copy")

            assertEquals("project-2", duplicate.id)
            assertEquals("Blanket copy", duplicate.name)
            assertEquals("design-1", repository.getProjectCells("project-2").single().squareDesignId)
            assertEquals(listOf("Cream"), repository.getProjectPalette("project-2").map { it.name })
        }

    @Test
    fun markProjectOpenedUsesRepositoryClock() =
        runTest {
            repository.createProject(project(rows = 1, columns = 1))
            repository = SquareToolRepository(database) { 1234L }

            repository.markProjectOpened("project-1")

            assertEquals(1234L, repository.getProject("project-1")?.lastOpenedAt)
        }

    @Test
    fun usageCountsPreventDeletingReferencedDesignAndColor() =
        runTest {
            repository.saveColor(color("color-1", "Cream"))
            repository.saveDesign(design(), rounds())
            repository.createProject(project(rows = 1, columns = 1))
            repository.setProjectPalette("project-1", listOf("color-1"))
            repository.saveCell(ProjectCellEntity("project-1", 0, 0, "design-1", false, false))

            assertEquals(1, repository.getDesignUsage("design-1").projectCellCount)
            assertEquals(3, repository.getColorUsage("color-1").squareRoundCount)
            assertEquals(false, repository.deleteDesignIfUnused("design-1"))
            assertEquals(false, repository.deleteColorIfUnused("color-1"))
        }

    @Test
    fun sampleProjectUsesProductionTablesAndSpecifiedCounts() =
        runTest {
            val sample = repository.createSampleProject()

            val cells = repository.getProjectCells(sample.id)
            assertEquals(12, sample.rowCount)
            assertEquals(8, sample.columnCount)
            assertEquals(96, cells.size)
            assertEquals(69, cells.count { it.completed })
            assertEquals(10, cells.count { it.locked })
            assertEquals(7, repository.getProjectPalette(sample.id).size)
            assertEquals(6, repository.getDesignsWithRounds().count { it.design.id.startsWith("sample-design-") })
        }

    private fun project(
        rows: Int,
        columns: Int,
    ) = ProjectEntity(
        id = "project-1",
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
        generationSeed = 42,
        defaultSquareDesignId = null,
        globalGramsPerSquare = null,
        skeinWeightGrams = null,
        joiningAndEdgingBufferPercent = 10.0,
        demoProject = false,
    )

    private fun design() =
        SquareDesignEntity(
            id = "design-1",
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

    private fun rounds() =
        listOf(
            SquareRoundEntity("design-1", 0, "color-1"),
            SquareRoundEntity("design-1", 1, "color-1"),
            SquareRoundEntity("design-1", 2, "color-1"),
        )

    private fun color(
        id: String,
        name: String,
    ) = ColorEntity(
        id = id,
        name = name,
        argb = 0xFFFF_FFFF,
        builtIn = false,
        createdAt = 1,
        updatedAt = 1,
    )
}
