package com.finnvek.squaretool.data.repository

import com.finnvek.squaretool.backup.BackupColorDto
import com.finnvek.squaretool.backup.BackupPaletteColorDto
import com.finnvek.squaretool.backup.BackupPaletteDto
import com.finnvek.squaretool.backup.BackupProjectCellDto
import com.finnvek.squaretool.backup.BackupProjectDto
import com.finnvek.squaretool.backup.BackupProjectPaletteDto
import com.finnvek.squaretool.backup.BackupSquareDesignDto
import com.finnvek.squaretool.backup.BackupSquareRoundDto
import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.PaletteColorCrossRef
import com.finnvek.squaretool.data.local.PaletteEntity
import com.finnvek.squaretool.data.local.ProjectCellEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.local.ProjectPaletteCrossRef
import com.finnvek.squaretool.data.local.SquareDesignEntity
import com.finnvek.squaretool.data.local.SquareRoundEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupMappersTest {
    // CPD-OFF
    @Test
    fun projectMapsInBothDirectionsWithoutLosingFields() {
        val entity =
            ProjectEntity(
                id = "project-1",
                name = "Blanket",
                rowCount = 7,
                columnCount = 9,
                squareWidthValue = 12.5,
                squareHeightValue = 13.5,
                measurementUnit = "centimeters",
                joiningGapValue = 0.75,
                trackingEnabled = true,
                favorite = true,
                notes = "Project notes",
                createdAt = 100,
                updatedAt = 200,
                lastOpenedAt = 300,
                generationSeed = 42,
                defaultSquareDesignId = "design-1",
                globalGramsPerSquare = 18.5,
                skeinWeightGrams = 100.0,
                joiningAndEdgingBufferPercent = 12.0,
                demoProject = false,
            )
        val dto =
            BackupProjectDto(
                id = "project-1",
                name = "Blanket",
                rowCount = 7,
                columnCount = 9,
                squareWidthValue = 12.5,
                squareHeightValue = 13.5,
                measurementUnit = "centimeters",
                joiningGapValue = 0.75,
                trackingEnabled = true,
                favorite = true,
                notes = "Project notes",
                createdAt = 100,
                updatedAt = 200,
                lastOpenedAt = 300,
                generationSeed = 42,
                defaultSquareDesignId = "design-1",
                globalGramsPerSquare = 18.5,
                skeinWeightGrams = 100.0,
                joiningAndEdgingBufferPercent = 12.0,
                demoProject = false,
            )

        assertEquals(dto, entity.toBackupDto())
        assertEquals(entity, dto.toEntity())
    }
    // CPD-ON

    // CPD-OFF
    @Test
    fun squareDesignMapsInBothDirectionsWithoutLosingFields() {
        val entity =
            SquareDesignEntity(
                id = "design-1",
                name = "Sunburst",
                motifTemplateId = "sunburst",
                note = "Design notes",
                favorite = true,
                builtIn = false,
                category = "Modern",
                gramsPerSquareOverride = 21.5,
                createdAt = 101,
                updatedAt = 201,
            )
        val dto =
            BackupSquareDesignDto(
                id = "design-1",
                name = "Sunburst",
                motifTemplateId = "sunburst",
                note = "Design notes",
                favorite = true,
                builtIn = false,
                category = "Modern",
                gramsPerSquareOverride = 21.5,
                createdAt = 101,
                updatedAt = 201,
            )

        assertEquals(dto, entity.toBackupDto())
        assertEquals(entity, dto.toEntity())
    }
    // CPD-ON

    @Test
    fun squareRoundMapsInBothDirectionsWithoutLosingFields() {
        val entity = SquareRoundEntity("design-1", 3, "color-1")
        val dto = BackupSquareRoundDto("design-1", 3, "color-1")

        assertEquals(dto, entity.toBackupDto())
        assertEquals(entity, dto.toEntity())
    }

    // CPD-OFF
    @Test
    fun colorMapsInBothDirectionsWithoutLosingFields() {
        val entity =
            ColorEntity(
                id = "color-1",
                name = "Ocean",
                argb = 0xFF123456,
                yarnBrand = "Brand",
                yarnLine = "Line",
                shadeName = "Deep Ocean",
                shadeCode = "OCEAN-7",
                skeinWeightGrams = 50.0,
                yarnLength = 125.0,
                yarnLengthUnit = "meters",
                notes = "Color notes",
                builtIn = false,
                createdAt = 102,
                updatedAt = 202,
            )
        val dto =
            BackupColorDto(
                id = "color-1",
                name = "Ocean",
                argb = 0xFF123456,
                yarnBrand = "Brand",
                yarnLine = "Line",
                shadeName = "Deep Ocean",
                shadeCode = "OCEAN-7",
                skeinWeightGrams = 50.0,
                yarnLength = 125.0,
                yarnLengthUnit = "meters",
                notes = "Color notes",
                builtIn = false,
                createdAt = 102,
                updatedAt = 202,
            )

        assertEquals(dto, entity.toBackupDto())
        assertEquals(entity, dto.toEntity())
    }
    // CPD-ON

    @Test
    fun paletteMapsInBothDirectionsWithoutLosingFields() {
        val entity = PaletteEntity("palette-1", "Coastal", false, 103, 203)
        val dto = BackupPaletteDto("palette-1", "Coastal", false, 103, 203)

        assertEquals(dto, entity.toBackupDto())
        assertEquals(entity, dto.toEntity())
    }

    @Test
    fun paletteColorMapsInBothDirectionsWithoutLosingFields() {
        val entity = PaletteColorCrossRef("palette-1", "color-1", 4)
        val dto = BackupPaletteColorDto("palette-1", "color-1", 4)

        assertEquals(dto, entity.toBackupDto())
        assertEquals(entity, dto.toEntity())
    }

    @Test
    fun projectPaletteMapsInBothDirectionsWithoutLosingFields() {
        val entity = ProjectPaletteCrossRef("project-1", "color-1", 5)
        val dto = BackupProjectPaletteDto("project-1", "color-1", 5)

        assertEquals(dto, entity.toBackupDto())
        assertEquals(entity, dto.toEntity())
    }

    @Test
    fun projectCellMapsInBothDirectionsWithoutLosingFields() {
        val entity =
            ProjectCellEntity(
                projectId = "project-1",
                rowIndex = 2,
                columnIndex = 3,
                squareDesignId = "design-1",
                locked = true,
                completed = true,
                gramsPerSquareOverride = 19.5,
            )
        val dto =
            BackupProjectCellDto(
                projectId = "project-1",
                rowIndex = 2,
                columnIndex = 3,
                squareDesignId = "design-1",
                locked = true,
                completed = true,
                gramsPerSquareOverride = 19.5,
            )

        assertEquals(dto, entity.toBackupDto())
        assertEquals(entity, dto.toEntity())
    }
}
