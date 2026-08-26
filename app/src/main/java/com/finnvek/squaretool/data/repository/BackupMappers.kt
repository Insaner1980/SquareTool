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

internal fun ProjectEntity.toBackupDto() =
    BackupProjectDto(
        id = id,
        name = name,
        rowCount = rowCount,
        columnCount = columnCount,
        squareWidthValue = squareWidthValue,
        squareHeightValue = squareHeightValue,
        measurementUnit = measurementUnit,
        joiningGapValue = joiningGapValue,
        trackingEnabled = trackingEnabled,
        favorite = favorite,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastOpenedAt = lastOpenedAt,
        generationSeed = generationSeed,
        defaultSquareDesignId = defaultSquareDesignId,
        globalGramsPerSquare = globalGramsPerSquare,
        skeinWeightGrams = skeinWeightGrams,
        joiningAndEdgingBufferPercent = joiningAndEdgingBufferPercent,
        demoProject = demoProject,
    )

// CPD-OFF
internal fun BackupProjectDto.toEntity() =
    ProjectEntity(
        id = id,
        name = name,
        rowCount = rowCount,
        columnCount = columnCount,
        squareWidthValue = squareWidthValue,
        squareHeightValue = squareHeightValue,
        measurementUnit = measurementUnit,
        joiningGapValue = joiningGapValue,
        trackingEnabled = trackingEnabled,
        favorite = favorite,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastOpenedAt = lastOpenedAt,
        generationSeed = generationSeed,
        defaultSquareDesignId = defaultSquareDesignId,
        globalGramsPerSquare = globalGramsPerSquare,
        skeinWeightGrams = skeinWeightGrams,
        joiningAndEdgingBufferPercent = joiningAndEdgingBufferPercent,
        demoProject = demoProject,
    )
// CPD-ON

internal fun SquareDesignEntity.toBackupDto() =
    BackupSquareDesignDto(
        id,
        name,
        motifTemplateId,
        note,
        favorite,
        builtIn,
        category,
        gramsPerSquareOverride,
        createdAt,
        updatedAt,
    )

internal fun BackupSquareDesignDto.toEntity() =
    SquareDesignEntity(
        id,
        name,
        motifTemplateId,
        note,
        favorite,
        builtIn,
        category,
        gramsPerSquareOverride,
        createdAt,
        updatedAt,
    )

internal fun SquareRoundEntity.toBackupDto() = BackupSquareRoundDto(squareDesignId, roundIndex, colorId)

internal fun BackupSquareRoundDto.toEntity() = SquareRoundEntity(squareDesignId, roundIndex, colorId)

internal fun ColorEntity.toBackupDto() =
    BackupColorDto(
        id,
        name,
        argb,
        yarnBrand,
        yarnLine,
        shadeName,
        shadeCode,
        skeinWeightGrams,
        yarnLength,
        yarnLengthUnit,
        notes,
        builtIn,
        createdAt,
        updatedAt,
    )

internal fun BackupColorDto.toEntity() =
    ColorEntity(
        id,
        name,
        argb,
        yarnBrand,
        yarnLine,
        shadeName,
        shadeCode,
        skeinWeightGrams,
        yarnLength,
        yarnLengthUnit,
        notes,
        builtIn,
        createdAt,
        updatedAt,
    )

internal fun PaletteEntity.toBackupDto() = BackupPaletteDto(id, name, builtIn, createdAt, updatedAt)

internal fun BackupPaletteDto.toEntity() = PaletteEntity(id, name, builtIn, createdAt, updatedAt)

internal fun PaletteColorCrossRef.toBackupDto() = BackupPaletteColorDto(paletteId, colorId, displayOrder)

internal fun BackupPaletteColorDto.toEntity() = PaletteColorCrossRef(paletteId, colorId, displayOrder)

internal fun ProjectPaletteCrossRef.toBackupDto() = BackupProjectPaletteDto(projectId, colorId, displayOrder)

internal fun BackupProjectPaletteDto.toEntity() = ProjectPaletteCrossRef(projectId, colorId, displayOrder)

internal fun ProjectCellEntity.toBackupDto() =
    BackupProjectCellDto(
        projectId,
        rowIndex,
        columnIndex,
        squareDesignId,
        locked,
        completed,
        gramsPerSquareOverride,
    )

internal fun BackupProjectCellDto.toEntity() =
    ProjectCellEntity(
        projectId,
        rowIndex,
        columnIndex,
        squareDesignId,
        locked,
        completed,
        gramsPerSquareOverride,
    )
