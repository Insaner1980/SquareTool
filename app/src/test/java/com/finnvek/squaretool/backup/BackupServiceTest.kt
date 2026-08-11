package com.finnvek.squaretool.backup

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BackupServiceTest {
    @Test
    fun projectBackupContainsOnlyProjectAndItsTransitiveDesignsAndColors() {
        val source =
            SquareToolBackupDto(
                exportedAtEpochMillis = 1,
                projects = listOf(project("kept", "design-kept"), project("other", "design-other")),
                squareDesigns = listOf(design("design-kept"), design("design-other")),
                squareRounds =
                    listOf(
                        BackupSquareRoundDto("design-kept", 0, "color-kept"),
                        BackupSquareRoundDto("design-kept", 1, "color-palette"),
                        BackupSquareRoundDto("design-kept", 2, "color-kept"),
                        BackupSquareRoundDto("design-other", 0, "color-other"),
                        BackupSquareRoundDto("design-other", 1, "color-other"),
                        BackupSquareRoundDto("design-other", 2, "color-other"),
                    ),
                colors =
                    listOf(
                        color("color-kept"),
                        color("color-palette"),
                        color("color-other"),
                    ),
                projectPalettes =
                    listOf(
                        BackupProjectPaletteDto("kept", "color-palette", 0),
                        BackupProjectPaletteDto("other", "color-other", 0),
                    ),
                projectCells =
                    listOf(
                        BackupProjectCellDto("kept", 0, 0, "design-kept", false, false),
                        BackupProjectCellDto("other", 0, 0, "design-other", false, false),
                    ),
            )

        val projectBackup = source.forProject("kept")

        assertEquals(listOf("kept"), projectBackup.projects.map { it.id })
        assertEquals(listOf("design-kept"), projectBackup.squareDesigns.map { it.id })
        assertEquals(
            setOf("color-kept", "color-palette"),
            projectBackup.colors.map { it.id }.toSet(),
        )
        assertEquals(listOf("kept"), projectBackup.projectCells.map { it.projectId })
        assertEquals(null, projectBackup.settings)
    }

    @Test
    fun settingsFailureDoesNotTouchDatabase() =
        runTest {
            val events = mutableListOf<String>()

            try {
                restoreWithRollback(
                    applySettings = {
                        events += "apply settings"
                        throw IllegalStateException("settings failed")
                    },
                    applyDatabase = { events += "apply database" },
                    rollbackSettings = { events += "rollback settings" },
                )
                throw AssertionError("Restore unexpectedly succeeded")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertEquals(listOf("apply settings", "rollback settings"), events)
        }

    @Test
    fun databaseFailureRollsBackSettingsAndLeavesDatabaseToItsTransaction() =
        runTest {
            val events = mutableListOf<String>()
            val failure = IllegalStateException("database failed")

            val thrown =
                try {
                    restoreWithRollback(
                        applySettings = { events += "apply settings" },
                        applyDatabase = {
                            events += "apply database"
                            throw failure
                        },
                        rollbackSettings = { events += "rollback settings" },
                    )
                    throw AssertionError("Restore unexpectedly succeeded")
                } catch (error: IllegalStateException) {
                    error
                }

            assertSame(failure, thrown)
            assertEquals(
                listOf(
                    "apply settings",
                    "apply database",
                    "rollback settings",
                ),
                events,
            )
        }

    private fun project(
        id: String,
        defaultDesignId: String,
    ) = BackupProjectDto(
        id = id,
        name = id,
        rowCount = 1,
        columnCount = 1,
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
        defaultSquareDesignId = defaultDesignId,
        globalGramsPerSquare = null,
        skeinWeightGrams = null,
        joiningAndEdgingBufferPercent = 10.0,
        demoProject = false,
    )

    private fun design(id: String) =
        BackupSquareDesignDto(
            id = id,
            name = id,
            motifTemplateId = "classic_granny",
            note = "",
            favorite = false,
            builtIn = false,
            category = "Classic",
            gramsPerSquareOverride = null,
            createdAt = 1,
            updatedAt = 1,
        )

    private fun color(id: String) =
        BackupColorDto(
            id = id,
            name = id,
            argb = 0xFF6B8A2E,
            yarnBrand = null,
            yarnLine = null,
            shadeName = null,
            shadeCode = null,
            skeinWeightGrams = null,
            yarnLength = null,
            yarnLengthUnit = null,
            notes = "",
            builtIn = false,
            createdAt = 1,
            updatedAt = 1,
        )
}
