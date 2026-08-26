package com.finnvek.squaretool.backup

import kotlinx.serialization.Serializable

const val CURRENT_BACKUP_SCHEMA_VERSION = 1

@Serializable
data class SquareToolBackupDto(
    val schemaVersion: Int = CURRENT_BACKUP_SCHEMA_VERSION,
    val exportedAtEpochMillis: Long,
    val projects: List<BackupProjectDto> = emptyList(),
    val squareDesigns: List<BackupSquareDesignDto> = emptyList(),
    val squareRounds: List<BackupSquareRoundDto> = emptyList(),
    val colors: List<BackupColorDto> = emptyList(),
    val palettes: List<BackupPaletteDto> = emptyList(),
    val paletteColors: List<BackupPaletteColorDto> = emptyList(),
    val projectPalettes: List<BackupProjectPaletteDto> = emptyList(),
    val projectCells: List<BackupProjectCellDto> = emptyList(),
    val settings: BackupSettingsDto? = null,
)

@Serializable
// CPD-OFF
data class BackupProjectDto(
    val id: String,
    val name: String,
    val rowCount: Int,
    val columnCount: Int,
    val squareWidthValue: Double?,
    val squareHeightValue: Double?,
    val measurementUnit: String,
    val joiningGapValue: Double?,
    val trackingEnabled: Boolean,
    val favorite: Boolean,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastOpenedAt: Long,
    val generationSeed: Long,
    val defaultSquareDesignId: String?,
    val globalGramsPerSquare: Double?,
    val skeinWeightGrams: Double?,
    val joiningAndEdgingBufferPercent: Double,
    val demoProject: Boolean,
)
// CPD-ON

@Serializable
// CPD-OFF
data class BackupSquareDesignDto(
    val id: String,
    val name: String,
    val motifTemplateId: String,
    val note: String,
    val favorite: Boolean,
    val builtIn: Boolean,
    val category: String,
    val gramsPerSquareOverride: Double?,
    val createdAt: Long,
    val updatedAt: Long,
)
// CPD-ON

@Serializable
data class BackupSquareRoundDto(
    val squareDesignId: String,
    val roundIndex: Int,
    val colorId: String,
)

@Serializable
data class BackupColorDto(
    val id: String,
    val name: String,
    val argb: Long,
    val yarnBrand: String?,
    val yarnLine: String?,
    val shadeName: String?,
    val shadeCode: String?,
    val skeinWeightGrams: Double?,
    val yarnLength: Double?,
    val yarnLengthUnit: String?,
    val notes: String,
    val builtIn: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupPaletteDto(
    val id: String,
    val name: String,
    val builtIn: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupPaletteColorDto(
    val paletteId: String,
    val colorId: String,
    val displayOrder: Int,
)

@Serializable
data class BackupProjectPaletteDto(
    val projectId: String,
    val colorId: String,
    val displayOrder: Int,
)

@Serializable
data class BackupProjectCellDto(
    val projectId: String,
    val rowIndex: Int,
    val columnIndex: Int,
    val squareDesignId: String?,
    val locked: Boolean,
    val completed: Boolean,
    val gramsPerSquareOverride: Double? = null,
)

@Serializable
data class BackupSettingsDto(
    val theme: String,
    val preferredMeasurementUnit: String,
    val defaultJoiningAndEdgingBufferPercent: Double,
    val defaultSkeinWeightGrams: Double,
    val hapticsEnabled: Boolean,
    val reduceMotion: Boolean,
    val showPlannerGridLines: Boolean,
    val confirmDestructiveLayoutGeneration: Boolean,
    val preserveCompletedCells: Boolean,
    val showLockMarkers: Boolean,
)

enum class BackupValidationCode {
    UNSUPPORTED_SCHEMA,
    DUPLICATE_ID,
    INVALID_ID,
    INVALID_DIMENSIONS,
    INVALID_COLOR,
    INVALID_NUMBER,
    INVALID_ROUNDS,
    INVALID_CELL,
    INVALID_ORDER,
    MISSING_REFERENCE,
}

data class BackupValidationError(
    val code: BackupValidationCode,
    val path: String,
    val detail: String,
)

data class BackupValidationResult(
    val errors: List<BackupValidationError>,
) {
    val isValid: Boolean get() = errors.isEmpty()
}

class InvalidBackupException(
    val errors: List<BackupValidationError>,
) : IllegalArgumentException(
        errors.firstOrNull()?.let { first ->
            "Backup validation failed at ${first.path}: ${first.detail}"
        } ?: "Backup validation failed",
    )
