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
