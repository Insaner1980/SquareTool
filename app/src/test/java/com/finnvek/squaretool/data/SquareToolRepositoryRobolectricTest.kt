package com.finnvek.squaretool.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.PaletteColorCrossRef
import com.finnvek.squaretool.data.local.PaletteEntity
import com.finnvek.squaretool.data.local.ProjectCellEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.local.SquareDesignEntity
import com.finnvek.squaretool.data.local.SquareRoundEntity
import com.finnvek.squaretool.data.local.SquareToolDatabase
import com.finnvek.squaretool.data.repository.SquareToolRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// CPD-OFF
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SquareToolRepositoryRobolectricTest {
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
            assertEquals(6, repository.getGrid("project-1")?.cells?.size)
        }
    // CPD-ON

    @Test
    fun projectFlowsExposeStoredProjectGridAndCardData() =
        runTest {
            repository.createProject(project(rows = 1, columns = 1))

            assertEquals(
                "project-1",
                repository
                    .observeProjects()
                    .first()
                    .single()
                    .id,
            )
            assertEquals("Blanket", repository.observeProject("project-1").first()?.name)
            assertEquals(1, repository.observeProjectCells("project-1").first().size)
            assertEquals(
                1,
                repository
                    .observeGrid("project-1")
                    .first()
                    ?.cells
                    ?.size,
            )
            assertEquals(listOf("Blanket"), repository.searchProjects("  Blank  ").first().map { it.name })

            val card = repository.observeProjectCardData().first { it.projects.isNotEmpty() }
            assertEquals(1, card.projects.size)
            assertEquals(1, card.cells.size)
            assertTrue(card.designs.isEmpty())
            assertTrue(card.colors.isEmpty())
            assertTrue(card.projectPaletteRefs.isEmpty())
        }

    @Test
    fun missingProjectHasNoGrid() =
        runTest {
            assertNull(repository.getGrid("missing"))
            assertNull(repository.observeGrid("missing").first())
        }

    @Test
    fun libraryFlowsAndSearchesExposeDesignsColorsAndPalettes() =
        runTest {
            repository.saveColor(color("color-1", "Cream"))
            repository.saveColor(color("color-2", "Blue"))
            repository.saveDesign(design(), rounds())
            repository.savePalette(
                palette("palette-1", "Coastal"),
                listOf(
                    PaletteColorCrossRef("palette-1", "color-2", 0),
                    PaletteColorCrossRef("palette-1", "color-1", 1),
                ),
            )

            assertEquals(
                "Design",
                repository
                    .observeDesigns()
                    .first()
                    .single()
                    .name,
            )
            assertEquals(
                3,
                repository
                    .observeDesignsWithRounds()
                    .first()
                    .single()
                    .rounds.size,
            )
            assertEquals(
                "design-1",
                repository
                    .searchDesigns("  Des  ")
                    .first()
                    .single()
                    .id,
            )
            assertEquals(3, repository.getDesignWithRounds("design-1")?.rounds?.size)
            assertEquals(1, repository.getDesignsWithRounds().size)

            assertEquals(2, repository.observeColors().first().size)
            assertEquals(2, repository.getColors().size)
            assertEquals("Cream", repository.getColor("color-1")?.name)
            assertEquals(
                "color-2",
                repository
                    .searchColors("  Blue ")
                    .first()
                    .single()
                    .id,
            )

            assertEquals(
                "Coastal",
                repository
                    .observePalettes()
                    .first()
                    .single()
                    .name,
            )
            assertEquals(1, repository.getPalettes().size)
            assertEquals("palette-1", repository.getPalette("palette-1")?.id)
            assertEquals(
                "palette-1",
                repository
                    .searchPalettes("  Coast ")
                    .first()
                    .single()
                    .id,
            )
            assertEquals(
                listOf("Blue", "Cream"),
                repository.observePaletteColors("palette-1").first().map { it.name },
            )
            val library = repository.searchLibrary("  C  ").first()
            assertEquals(listOf("Cream"), library.colors.map { it.name })
            assertEquals(listOf("Coastal"), library.palettes.map { it.name })
        }

    // CPD-OFF
    @Test
    fun bulkCellUpdatePersistsAssignmentsForMultipleProjects() =
        runTest {
            repository.saveColor(color("color-1", "Cream"))
            repository.saveDesign(design(), rounds())
            repository.createProject(project(rows = 1, columns = 2))
            repository.createProject(project(rows = 1, columns = 1, id = "project-2", name = "Scarf"))
            repository = SquareToolRepository(database) { 50L }

            repository.saveCells(emptyList())
            repository.saveCells(
                listOf(
                    ProjectCellEntity("project-1", 0, 0, "design-1", locked = true, completed = false),
                    ProjectCellEntity("project-1", 0, 1, "design-1", locked = false, completed = true),
                    ProjectCellEntity("project-2", 0, 0, "design-1", locked = false, completed = false),
                ),
            )

            val cells = repository.getProjectCells("project-1")
            assertEquals(listOf("design-1", "design-1"), cells.map { it.squareDesignId })
            assertEquals(1, cells.count { it.locked })
            assertEquals(1, cells.count { it.completed })
            assertEquals(50L, repository.getProject("project-1")?.updatedAt)
            assertEquals(50L, repository.getProject("project-2")?.updatedAt)
        }
    // CPD-ON

    @Test
    fun updateProjectResizesGridAndPreservesCellsInsideNewBounds() =
        runTest {
            repository.createProject(project(rows = 2, columns = 2))
            repository.saveCell(ProjectCellEntity("project-1", 0, 0, null, locked = true, completed = true))

            repository.updateProject(project(rows = 1, columns = 3).copy(name = "Resized"))
            repository.updateProject(project(rows = 1, columns = 3).copy(name = "Renamed"))

            val cells = repository.getProjectCells("project-1")
            assertEquals(3, cells.size)
            assertTrue(cells.single { it.columnIndex == 0 }.locked)
            assertEquals("Renamed", repository.getProject("project-1")?.name)
        }

    @Test
    fun saveProjectWithLayoutAndPaletteCreatesAndThenResizesProject() =
        runTest {
            repository.saveColor(color("color-1", "Cream"))
            repository.saveDesign(design(), rounds())

            repository.saveProjectWithLayoutAndPalette(
                project = project(rows = 1, columns = 2),
                orderedColorIds = listOf("color-1"),
                initialAssignments = listOf("design-1", null),
            )

            assertEquals(listOf("design-1", null), repository.getProjectCells("project-1").map { it.squareDesignId })
            assertEquals(listOf("Cream"), repository.getProjectPalette("project-1").map { it.name })

            repository.saveProjectWithLayoutAndPalette(
                project = project(rows = 2, columns = 1).copy(name = "Updated"),
                orderedColorIds = emptyList(),
                initialAssignments = null,
            )

            assertEquals(2, repository.getProjectCells("project-1").size)
            assertEquals("design-1", repository.getProjectCells("project-1").first().squareDesignId)
            assertTrue(repository.getProjectPalette("project-1").isEmpty())
        }

    @Test
    fun resizeProjectReportsDiscardedAndAssignedCells() =
        runTest {
            repository.saveColor(color("color-1", "Cream"))
            repository.saveDesign(design(), rounds())
            repository.createProject(project(rows = 2, columns = 2))
            repository.saveCells(
                listOf(
                    ProjectCellEntity("project-1", 0, 1, "design-1", false, false),
                    ProjectCellEntity("project-1", 1, 0, null, false, false),
                    ProjectCellEntity("project-1", 1, 1, null, false, false),
                ),
            )
            repository = SquareToolRepository(database) { 1234L }

            val result = repository.resizeProject("project-1", rows = 1, columns = 1)

            assertEquals(3, result.lostCellCount)
            assertEquals(1, result.lostAssignedCellCount)
            assertEquals(1234L, result.project.updatedAt)
            assertEquals(1, repository.getProjectCells("project-1").size)
        }

    @Test
    fun duplicateProjectCopiesCellsAndPaletteAndResetsProjectMetadata() =
        runTest {
            repository.saveColor(color("color-1", "Cream"))
            repository.saveDesign(design(), rounds())
            repository.createProject(project(rows = 1, columns = 1).copy(favorite = true, demoProject = true))
            repository.setProjectPalette("project-1", listOf("color-1"))
            repository.saveCell(ProjectCellEntity("project-1", 0, 0, "design-1", true, true))
            repository = SquareToolRepository(database) { 200L }

            val duplicate = repository.duplicateProject("project-1", "project-2", "Blanket copy")

            assertEquals("project-2", duplicate.id)
            assertEquals("Blanket copy", duplicate.name)
            assertFalse(duplicate.favorite)
            assertFalse(duplicate.demoProject)
            assertEquals(200L, duplicate.createdAt)
            assertEquals("design-1", repository.getProjectCells("project-2").single().squareDesignId)
            assertEquals(listOf("Cream"), repository.observeProjectPalette("project-2").first().map { it.name })
        }

    @Test
    fun duplicateProjectAlsoSupportsSourceWithoutPalette() =
        runTest {
            repository.createProject(project(rows = 1, columns = 1))

            val duplicate = repository.duplicateProject("project-1", "project-2", "Copy")

            assertEquals(1, repository.getProjectCells(duplicate.id).size)
            assertTrue(repository.getProjectPalette(duplicate.id).isEmpty())
        }

    // CPD-OFF
    @Test
    fun markProjectOpenedAndDeleteProjectUpdateStoredState() =
        runTest {
            repository.createProject(project(rows = 1, columns = 1))
            repository = SquareToolRepository(database) { 1234L }

            repository.markProjectOpened("project-1")
            assertEquals(1234L, repository.getProject("project-1")?.lastOpenedAt)

            repository.deleteProject("project-1")
            assertNull(repository.getProject("project-1"))
            assertTrue(repository.getProjectCells("project-1").isEmpty())
        }
    // CPD-ON

    @Test
    fun replaceProjectCellsRequiresACompleteUniqueLayout() =
        runTest {
            repository.createProject(project(rows = 1, columns = 2))
            repository = SquareToolRepository(database) { 75L }
            val complete =
                listOf(
                    ProjectCellEntity("project-1", 0, 0, null, true, false),
                    ProjectCellEntity("project-1", 0, 1, null, false, true),
                )

            repository.replaceProjectCells("project-1", complete)
            assertEquals(complete, repository.getProjectCells("project-1"))
            assertEquals(75L, repository.getProject("project-1")?.updatedAt)

            assertIllegalArgument {
                repository.replaceProjectCells("project-1", complete.take(1))
            }
            assertIllegalArgument {
                repository.replaceProjectCells("project-1", listOf(complete[0], complete[0]))
            }
        }

    @Test
    fun designUsagePreventsReferencedDeletionAndAllowsUnusedDeletion() =
        runTest {
            repository.saveColor(color("color-1", "Cream"))
            repository.saveDesign(design(), rounds())
            repository.createProject(
                project(rows = 1, columns = 1).copy(defaultSquareDesignId = "design-1"),
            )
            repository.saveCell(ProjectCellEntity("project-1", 0, 0, "design-1", false, false))

            val usage = repository.getDesignUsage("design-1")
            assertEquals(1, usage.projectCellCount)
            assertEquals(1, usage.defaultProjectCount)
            assertEquals(2, usage.totalReferenceCount)
            assertFalse(repository.deleteDesignIfUnused("design-1"))
            assertIllegalState { repository.deleteDesign("design-1") }

            repository.saveDesign(design(id = "design-2", name = "Unused"), rounds(designId = "design-2"))
            repository.deleteDesign("design-2")
            assertNull(repository.getDesign("design-2"))
        }

    @Test
    fun colorUsagePreventsReferencedDeletionAndAllowsUnusedDeletion() =
        runTest {
            repository.saveColor(color("color-1", "Cream"))
            repository.saveColor(color("color-2", "Unused"))
            repository.saveDesign(design(), rounds())
            repository.savePalette(
                palette("palette-1", "Palette"),
                listOf(PaletteColorCrossRef("palette-1", "color-1", 0)),
            )
            repository.createProject(project(rows = 1, columns = 1))
            repository.setProjectPalette("project-1", listOf("color-1"))

            val usage = repository.getColorUsage("color-1")
            assertEquals(3, usage.squareRoundCount)
            assertEquals(1, usage.paletteCount)
            assertEquals(1, usage.projectCount)
            assertEquals(5, usage.totalReferenceCount)
            assertFalse(repository.deleteColorIfUnused("color-1"))
            assertIllegalState { repository.deleteColor("color-1") }

            repository.deleteColor("color-2")
            assertNull(repository.getColor("color-2"))
        }

    @Test
    fun palettesCanBeAppliedReplacedClearedAndDeleted() =
        runTest {
            repository.saveColor(color("color-1", "Cream"))
            repository.saveColor(color("color-2", "Blue"))
            repository.savePalette(
                palette("palette-1", "Full"),
                listOf(
                    PaletteColorCrossRef("palette-1", "color-2", 0),
                    PaletteColorCrossRef("palette-1", "color-1", 1),
                ),
            )
            repository.savePalette(palette("palette-2", "Empty"), emptyList())
            repository.createProject(project(rows = 1, columns = 1))

            repository.applyPaletteToProject("project-1", "palette-1")
            assertEquals(listOf("Blue", "Cream"), repository.getProjectPalette("project-1").map { it.name })

            repository.applyPaletteToProject("project-1", "palette-2")
            assertTrue(repository.getProjectPalette("project-1").isEmpty())

            repository.setProjectPalette("project-1", emptyList())
            repository.deletePalette("palette-2")
            assertNull(repository.getPalette("palette-2"))
        }

    // CPD-OFF
    @Test
    fun backupRoundTripRestoresEveryTableAndDeleteAllClearsThem() =
        runTest {
            repository.saveColor(color("color-1", "Cream"))
            repository.saveDesign(design(), rounds())
            repository.savePalette(
                palette("palette-1", "Palette"),
                listOf(PaletteColorCrossRef("palette-1", "color-1", 0)),
            )
            repository.createProject(project(rows = 1, columns = 1))
            repository.setProjectPalette("project-1", listOf("color-1"))
            repository.saveCell(ProjectCellEntity("project-1", 0, 0, "design-1", true, true))
            val backup = repository.createBackup(exportedAtEpochMillis = 500L)

            repository.deleteAll()
            assertTrue(repository.getProjects().isEmpty())
            assertTrue(repository.getColors().isEmpty())

            repository.restoreBackup(backup)
            assertEquals(500L, backup.exportedAtEpochMillis)
            assertEquals("Blanket", repository.getProjects().single().name)
            assertEquals(
                "Design",
                repository
                    .getDesignsWithRounds()
                    .single()
                    .design.name,
            )
            assertEquals("Cream", repository.getPaletteColors("palette-1").single().name)
            assertEquals("design-1", repository.getProjectCells("project-1").single().squareDesignId)
            assertEquals("Cream", repository.getProjectPalette("project-1").single().name)

            repository.deleteAll()
            val empty = repository.createBackup(exportedAtEpochMillis = 501L)
            assertTrue(empty.projects.isEmpty())
            assertTrue(empty.squareDesigns.isEmpty())
            assertTrue(empty.colors.isEmpty())
        }
    // CPD-ON

    @Test
    fun failedRestoreLeavesExistingDatabaseUnchanged() =
        runTest {
            repository.createProject(project(rows = 1, columns = 1))
            val original = repository.createBackup(exportedAtEpochMillis = 500L)
            val changed = original.copy(projects = listOf(original.projects.single().copy(name = "Restored")))
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

            val failure = runCatching { repository.restoreBackup(changed) }.exceptionOrNull()

            assertNotNull(failure)
            assertEquals(listOf("Blanket"), repository.getProjects().map { it.name })
            assertEquals(1, repository.getProjectCells("project-1").size)
        }

    // CPD-OFF
    @Test
    fun sampleProjectIsCreatedOnlyOnce() =
        runTest {
            repository = SquareToolRepository(database) { 1000L }

            val sample = repository.createSampleProject()
            val sameSample = repository.createSampleProject()

            val cells = repository.getProjectCells(sample.id)
            assertEquals(sample.id, sameSample.id)
            assertEquals(1, repository.getProjects().size)
            assertEquals(12, sample.rowCount)
            assertEquals(8, sample.columnCount)
            assertEquals(96, cells.size)
            assertEquals(69, cells.count { it.completed })
            assertEquals(10, cells.count { it.locked })
            assertEquals(7, repository.getProjectPalette(sample.id).size)
            assertEquals(6, repository.getDesignsWithRounds().count { it.design.id.startsWith("sample-design-") })
        }
    // CPD-ON

    @Test
    fun projectAndCellValidationRejectsInvalidValues() =
        runTest {
            assertIllegalArgument { repository.createProject(project(1, 1).copy(id = "")) }
            assertIllegalArgument { repository.createProject(project(1, 1).copy(name = "")) }
            assertIllegalArgument { repository.createProject(project(0, 1)) }
            assertIllegalArgument { repository.createProject(project(1, 1).copy(squareWidthValue = 0.0)) }
            assertIllegalArgument { repository.createProject(project(1, 1).copy(squareHeightValue = Double.NaN)) }
            assertIllegalArgument { repository.createProject(project(1, 1).copy(joiningGapValue = -1.0)) }
            assertIllegalArgument {
                repository.createProject(project(1, 1).copy(joiningAndEdgingBufferPercent = 101.0))
            }
            repository.createProject(project(1, 1))
            assertIllegalArgument {
                repository.saveCell(ProjectCellEntity("project-1", 1, 0, null, false, false))
            }
            assertIllegalArgument {
                repository.saveCell(
                    ProjectCellEntity("project-1", 0, 0, null, false, false, Double.NaN),
                )
            }
            assertIllegalState { repository.saveCell(ProjectCellEntity("missing", 0, 0, null, false, false)) }
        }

    @Test
    fun projectOperationValidationRejectsInvalidRequests() =
        runTest {
            assertIllegalState { repository.updateProject(project(1, 1, id = "missing")) }
            assertIllegalState { repository.resizeProject("missing", 1, 1) }
            assertIllegalState { repository.markProjectOpened("missing") }
            assertIllegalState { repository.duplicateProject("missing", "new", "Copy") }
            repository.createProject(project(1, 1))
            assertIllegalArgument { repository.duplicateProject("project-1", "", "Copy") }
            assertIllegalArgument { repository.duplicateProject("project-1", "new", "") }
            assertIllegalArgument { repository.setProjectPalette("missing", emptyList()) }
            assertIllegalArgument { repository.applyPaletteToProject("missing", "missing") }
            assertIllegalArgument { repository.applyPaletteToProject("project-1", "missing") }
        }

    @Test
    fun layoutPaletteAndRoundValidationRejectsMalformedCollections() =
        runTest {
            repository.saveColor(color("color-1", "Cream"))
            assertIllegalArgument {
                repository.saveDesign(design(), rounds().take(2))
            }
            assertIllegalArgument {
                repository.saveDesign(design(), rounds().map { it.copy(squareDesignId = "other") })
            }
            assertIllegalArgument {
                repository.saveDesign(design(), rounds().mapIndexed { index, round -> round.copy(roundIndex = index + 1) })
            }
            assertIllegalArgument {
                repository.savePalette(
                    palette("palette-1", "Invalid"),
                    listOf(PaletteColorCrossRef("other", "color-1", 0)),
                )
            }
            assertIllegalArgument {
                repository.savePalette(
                    palette("palette-1", "Invalid"),
                    listOf(
                        PaletteColorCrossRef("palette-1", "color-1", 0),
                        PaletteColorCrossRef("palette-1", "color-1", 1),
                    ),
                )
            }
            assertIllegalArgument {
                repository.savePalette(
                    palette("palette-1", "Invalid"),
                    listOf(PaletteColorCrossRef("palette-1", "color-1", -1)),
                )
            }
            assertIllegalArgument {
                repository.saveProjectWithLayoutAndPalette(project(1, 1), listOf("color-1", "color-1"), null)
            }
            assertIllegalArgument {
                repository.saveProjectWithLayoutAndPalette(project(1, 2), emptyList(), listOf(null))
            }
            repository.createProject(project(1, 1))
            assertIllegalArgument {
                repository.saveProjectWithLayoutAndPalette(project(1, 1), emptyList(), listOf(null))
            }
            assertIllegalArgument { repository.setProjectPalette("project-1", listOf("color-1", "color-1")) }
        }

    @Test
    fun colorValidationRejectsBlankAndOutOfRangeValues() =
        runTest {
            assertIllegalArgument { repository.saveColor(color("", "Cream")) }
            assertIllegalArgument { repository.saveColor(color("color-1", "")) }
            assertIllegalArgument { repository.saveColor(color("color-1", "Cream").copy(argb = -1L)) }
            assertIllegalArgument { repository.saveColor(color("color-1", "Cream").copy(argb = 0x1_0000_0000L)) }
        }

    private suspend fun assertIllegalArgument(block: suspend () -> Unit) {
        val failure = runCatching { block() }.exceptionOrNull()
        assertTrue("Expected IllegalArgumentException but got $failure", failure is IllegalArgumentException)
    }

    private suspend fun assertIllegalState(block: suspend () -> Unit) {
        val failure = runCatching { block() }.exceptionOrNull()
        assertTrue("Expected IllegalStateException but got $failure", failure is IllegalStateException)
    }

    // CPD-OFF
    private fun project(
        rows: Int,
        columns: Int,
        id: String = "project-1",
        name: String = "Blanket",
    ) = ProjectEntity(
        id = id,
        name = name,
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
    // CPD-ON

    private fun design(
        id: String = "design-1",
        name: String = "Design",
    ) = SquareDesignEntity(
        id = id,
        name = name,
        motifTemplateId = "classic_granny",
        note = "",
        favorite = false,
        builtIn = false,
        category = "Classic",
        gramsPerSquareOverride = null,
        createdAt = 1,
        updatedAt = 1,
    )

    private fun rounds(
        designId: String = "design-1",
        colorId: String = "color-1",
    ) = listOf(
        SquareRoundEntity(designId, 0, colorId),
        SquareRoundEntity(designId, 1, colorId),
        SquareRoundEntity(designId, 2, colorId),
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

    private fun palette(
        id: String,
        name: String,
    ) = PaletteEntity(
        id = id,
        name = name,
        builtIn = false,
        createdAt = 1,
        updatedAt = 1,
    )
}
