package com.finnvek.squaretool.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecValidatorTest {
    @Test
    fun roundTripPreservesEveryBackupCollection() {
        val backup = validBackup()

        val decoded = BackupCodec.decode(BackupCodec.encode(backup))

        assertEquals(backup, decoded)
    }

    @Test
    fun unsupportedSchemaIsRejected() {
        val result = BackupValidator.validate(validBackup().copy(schemaVersion = 2))

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == BackupValidationCode.UNSUPPORTED_SCHEMA })
    }

    @Test
    fun futureJsonWithAdditionalFieldsReachesSchemaValidation() {
        val json =
            BackupCodec
                .encode(validBackup())
                .replace("\"schemaVersion\": 1", "\"schemaVersion\": 2,\n    \"futureField\": true")

        val result = BackupValidator.validate(BackupCodec.decode(json))

        assertTrue(result.errors.any { it.code == BackupValidationCode.UNSUPPORTED_SCHEMA })
    }

    @Test
    fun missingRoundColorReferenceIsRejected() {
        val backup =
            validBackup().copy(
                squareRounds =
                    listOf(
                        BackupSquareRoundDto("design-1", 0, "missing-color"),
                        BackupSquareRoundDto("design-1", 1, "color-1"),
                        BackupSquareRoundDto("design-1", 2, "color-1"),
                    ),
            )

        val result = BackupValidator.validate(backup)

        assertTrue(result.errors.any { it.code == BackupValidationCode.MISSING_REFERENCE })
    }

    @Test
    fun missingReferencesAreReportedAtEverySupportedRelationship() {
        val valid = validBackup()
        val backup =
            valid.copy(
                projects = listOf(valid.projects.single().copy(defaultSquareDesignId = "missing-default-design")),
                squareRounds = listOf(valid.squareRounds.first().copy(squareDesignId = "missing-round-design")),
                paletteColors = listOf(valid.paletteColors.single().copy(colorId = "missing-palette-color")),
                projectPalettes = listOf(valid.projectPalettes.single().copy(colorId = "missing-project-color")),
                projectCells = listOf(valid.projectCells.single().copy(squareDesignId = "missing-cell-design")),
            )

        val paths =
            BackupValidator
                .validate(backup)
                .errors
                .filter { it.code == BackupValidationCode.MISSING_REFERENCE }
                .mapTo(mutableSetOf(), BackupValidationError::path)

        assertTrue("projects[0].defaultSquareDesignId" in paths)
        assertTrue("squareRounds[0].squareDesignId" in paths)
        assertTrue("paletteColors[0].colorId" in paths)
        assertTrue("projectPalettes[0].colorId" in paths)
        assertTrue("projectCells[0].squareDesignId" in paths)
    }

    @Test
    fun colorOutsideUnsignedArgbRangeIsRejected() {
        val backup =
            validBackup().copy(
                colors = listOf(validBackup().colors.single().copy(argb = 0x1_0000_0000L)),
            )

        val result = BackupValidator.validate(backup)

        assertTrue(result.errors.any { it.code == BackupValidationCode.INVALID_COLOR })
    }

    @Test
    fun projectDimensionOutsideSupportedRangeIsRejected() {
        val project = validBackup().projects.single().copy(rowCount = 51)

        val result = BackupValidator.validate(validBackup().copy(projects = listOf(project)))

        assertTrue(result.errors.any { it.code == BackupValidationCode.INVALID_DIMENSIONS })
    }

    @Test
    fun unknownMotifTemplateIsRejectedBeforeRestore() {
        val design = validBackup().squareDesigns.single().copy(motifTemplateId = "unknown")

        val result = BackupValidator.validate(validBackup().copy(squareDesigns = listOf(design)))

        assertTrue(result.errors.any { it.code == BackupValidationCode.INVALID_ID })
    }

    @Test
    fun motifTemplateSpecificRoundRangeIsEnforced() {
        val design = validBackup().squareDesigns.single().copy(motifTemplateId = "daisy")

        val result = BackupValidator.validate(validBackup().copy(squareDesigns = listOf(design)))

        assertTrue(result.errors.any { it.code == BackupValidationCode.INVALID_ROUNDS })
    }

    @Test
    fun invalidBackupExceptionExplainsTheFirstValidationFailure() {
        val invalid = validBackup().copy(schemaVersion = 99)

        val exception =
            try {
                BackupValidator.requireValid(invalid)
                throw AssertionError("Invalid backup was accepted")
            } catch (error: InvalidBackupException) {
                error
            }

        assertTrue(exception.message.orEmpty().contains("schemaVersion"))
        assertTrue(exception.message.orEmpty().contains("Expected 1 but was 99"))
    }

    @Test
    fun validBackupHasNoValidationErrors() {
        val result = BackupValidator.validate(validBackup())

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        BackupValidator.requireValid(validBackup())
    }

    @Test
    fun invalidNumericValuesAndSettingsAreRejectedAtTheirExactPaths() {
        val valid = validBackup()
        val backup =
            valid.copy(
                projects =
                    listOf(
                        valid.projects.single().copy(
                            squareWidthValue = 0.0,
                            squareHeightValue = Double.NaN,
                            joiningGapValue = -0.1,
                            globalGramsPerSquare = 0.0,
                            skeinWeightGrams = -1.0,
                            joiningAndEdgingBufferPercent = 101.0,
                        ),
                    ),
                squareDesigns =
                    listOf(valid.squareDesigns.single().copy(gramsPerSquareOverride = 0.0)),
                colors =
                    listOf(
                        valid.colors.single().copy(
                            skeinWeightGrams = 0.0,
                            yarnLength = Double.POSITIVE_INFINITY,
                        ),
                    ),
                projectCells =
                    listOf(valid.projectCells.single().copy(gramsPerSquareOverride = -1.0)),
                settings =
                    BackupSettingsDto(
                        theme = "unknown",
                        preferredMeasurementUnit = "yards",
                        defaultJoiningAndEdgingBufferPercent = -1.0,
                        defaultSkeinWeightGrams = 0.0,
                        hapticsEnabled = true,
                        reduceMotion = false,
                        showPlannerGridLines = true,
                        confirmDestructiveLayoutGeneration = true,
                        preserveCompletedCells = true,
                        showLockMarkers = true,
                    ),
            )

        val paths = BackupValidator.validate(backup).errors.mapTo(mutableSetOf(), BackupValidationError::path)

        assertTrue("projects[0].squareWidthValue" in paths)
        assertTrue("projects[0].squareHeightValue" in paths)
        assertTrue("projects[0].joiningGapValue" in paths)
        assertTrue("projects[0].globalGramsPerSquare" in paths)
        assertTrue("projects[0].skeinWeightGrams" in paths)
        assertTrue("projects[0].joiningAndEdgingBufferPercent" in paths)
        assertTrue("squareDesigns[0].gramsPerSquareOverride" in paths)
        assertTrue("colors[0].skeinWeightGrams" in paths)
        assertTrue("colors[0].yarnLength" in paths)
        assertTrue("projectCells[0].gramsPerSquareOverride" in paths)
        assertTrue("settings.theme" in paths)
        assertTrue("settings.preferredMeasurementUnit" in paths)
        assertTrue("settings.defaultJoiningAndEdgingBufferPercent" in paths)
        assertTrue("settings.defaultSkeinWeightGrams" in paths)
    }

    @Test
    fun duplicateRelationshipsInvalidOrdersAndBrokenCoordinatesAreRejected() {
        val valid = validBackup()
        val cell = valid.projectCells.single()
        val paletteColor = valid.paletteColors.single().copy(displayOrder = -1)
        val projectPalette = valid.projectPalettes.single().copy(displayOrder = -1)
        val backup =
            valid.copy(
                squareRounds = valid.squareRounds + valid.squareRounds.first(),
                paletteColors =
                    listOf(
                        paletteColor,
                        paletteColor,
                        BackupPaletteColorDto("missing-palette", "color-1", 0),
                    ),
                projectPalettes =
                    listOf(
                        projectPalette,
                        projectPalette,
                        BackupProjectPaletteDto("missing-project", "color-1", 0),
                    ),
                projectCells =
                    listOf(
                        cell,
                        cell,
                        cell.copy(rowIndex = 1),
                        cell.copy(projectId = "missing-project"),
                    ),
            )

        val errors = BackupValidator.validate(backup).errors

        assertTrue(errors.any { it.code == BackupValidationCode.DUPLICATE_ID && it.path == "squareRounds" })
        assertTrue(errors.any { it.code == BackupValidationCode.INVALID_ROUNDS && it.path == "squareDesigns[0]" })
        assertTrue(errors.any { it.code == BackupValidationCode.INVALID_ORDER && it.path == "paletteColors[0].displayOrder" })
        assertTrue(errors.any { it.code == BackupValidationCode.DUPLICATE_ID && it.path == "paletteColors" })
        assertTrue(errors.any { it.code == BackupValidationCode.MISSING_REFERENCE && it.path == "paletteColors[2].paletteId" })
        assertTrue(errors.any { it.code == BackupValidationCode.INVALID_ORDER && it.path == "projectPalettes[0].displayOrder" })
        assertTrue(errors.any { it.code == BackupValidationCode.MISSING_REFERENCE && it.path == "projectPalettes[2].projectId" })
        assertTrue(errors.any { it.code == BackupValidationCode.DUPLICATE_ID && it.path == "projectCells" })
        assertTrue(errors.any { it.code == BackupValidationCode.INVALID_CELL && it.path == "projectCells[2]" })
        assertTrue(errors.any { it.code == BackupValidationCode.MISSING_REFERENCE && it.path == "projectCells[3].projectId" })
    }

    private fun validBackup() =
        SquareToolBackupDto(
            schemaVersion = 1,
            exportedAtEpochMillis = 1_725_000_000_000,
            projects =
                listOf(
                    BackupProjectDto(
                        id = "project-1",
                        name = "Test blanket",
                        rowCount = 1,
                        columnCount = 1,
                        squareWidthValue = 10.0,
                        squareHeightValue = 10.0,
                        measurementUnit = "centimeters",
                        joiningGapValue = 0.5,
                        trackingEnabled = true,
                        favorite = false,
                        notes = "",
                        createdAt = 100,
                        updatedAt = 200,
                        lastOpenedAt = 200,
                        generationSeed = 42,
                        defaultSquareDesignId = "design-1",
                        globalGramsPerSquare = 18.0,
                        skeinWeightGrams = 100.0,
                        joiningAndEdgingBufferPercent = 10.0,
                        demoProject = false,
                    ),
                ),
            squareDesigns =
                listOf(
                    BackupSquareDesignDto(
                        id = "design-1",
                        name = "Test square",
                        motifTemplateId = "classic_granny",
                        note = "",
                        favorite = false,
                        builtIn = false,
                        category = "Classic",
                        gramsPerSquareOverride = null,
                        createdAt = 100,
                        updatedAt = 200,
                    ),
                ),
            squareRounds =
                listOf(
                    BackupSquareRoundDto("design-1", 0, "color-1"),
                    BackupSquareRoundDto("design-1", 1, "color-1"),
                    BackupSquareRoundDto("design-1", 2, "color-1"),
                ),
            colors =
                listOf(
                    BackupColorDto(
                        id = "color-1",
                        name = "Cream",
                        argb = 0xFFF3E6C9,
                        yarnBrand = null,
                        yarnLine = null,
                        shadeName = null,
                        shadeCode = null,
                        skeinWeightGrams = null,
                        yarnLength = null,
                        yarnLengthUnit = null,
                        notes = "",
                        builtIn = false,
                        createdAt = 100,
                        updatedAt = 200,
                    ),
                ),
            palettes =
                listOf(
                    BackupPaletteDto("palette-1", "Test palette", false, 100, 200),
                ),
            paletteColors =
                listOf(
                    BackupPaletteColorDto("palette-1", "color-1", 0),
                ),
            projectPalettes =
                listOf(
                    BackupProjectPaletteDto("project-1", "color-1", 0),
                ),
            projectCells =
                listOf(
                    BackupProjectCellDto("project-1", 0, 0, "design-1", locked = true, completed = false),
                ),
        )
}
