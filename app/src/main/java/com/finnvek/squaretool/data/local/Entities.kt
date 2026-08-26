package com.finnvek.squaretool.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = SquareDesignEntity::class,
            parentColumns = ["id"],
            childColumns = ["defaultSquareDesignId"],
            onDelete = ForeignKey.SET_NULL,
            deferred = true,
        ),
    ],
    indices = [
        Index("defaultSquareDesignId"),
        Index(value = ["favorite", "lastOpenedAt"]),
    ],
)
data class ProjectEntity(
    @androidx.room.PrimaryKey val id: String,
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

@Entity(
    tableName = "square_designs",
    indices = [Index("name"), Index("category")],
)
data class SquareDesignEntity(
    @androidx.room.PrimaryKey val id: String,
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

@Entity(
    tableName = "colors",
    indices = [Index("name")],
)
data class ColorEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val argb: Long,
    val yarnBrand: String? = null,
    val yarnLine: String? = null,
    val shadeName: String? = null,
    val shadeCode: String? = null,
    val skeinWeightGrams: Double? = null,
    val yarnLength: Double? = null,
    val yarnLengthUnit: String? = null,
    val notes: String = "",
    val builtIn: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

// CPD-OFF
@Entity(
    tableName = "square_rounds",
    primaryKeys = ["squareDesignId", "roundIndex"],
    foreignKeys = [
        ForeignKey(
            entity = SquareDesignEntity::class,
            parentColumns = ["id"],
            childColumns = ["squareDesignId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ColorEntity::class,
            parentColumns = ["id"],
            childColumns = ["colorId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("colorId")],
)
// CPD-ON
data class SquareRoundEntity(
    val squareDesignId: String,
    val roundIndex: Int,
    val colorId: String,
)

@Entity(
    tableName = "palettes",
    indices = [Index("name")],
)
data class PaletteEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val builtIn: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

// CPD-OFF
@Entity(
    tableName = "palette_color_cross_ref",
    primaryKeys = ["paletteId", "colorId"],
    foreignKeys = [
        ForeignKey(
            entity = PaletteEntity::class,
            parentColumns = ["id"],
            childColumns = ["paletteId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ColorEntity::class,
            parentColumns = ["id"],
            childColumns = ["colorId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("colorId"),
        Index(value = ["paletteId", "displayOrder"], unique = true),
    ],
)
// CPD-ON
data class PaletteColorCrossRef(
    val paletteId: String,
    val colorId: String,
    val displayOrder: Int,
)

@Entity(
    tableName = "project_palette_cross_ref",
    primaryKeys = ["projectId", "colorId"],
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ColorEntity::class,
            parentColumns = ["id"],
            childColumns = ["colorId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("colorId"),
        Index(value = ["projectId", "displayOrder"], unique = true),
    ],
)
data class ProjectPaletteCrossRef(
    val projectId: String,
    val colorId: String,
    val displayOrder: Int,
)

@Entity(
    tableName = "project_cells",
    primaryKeys = ["projectId", "rowIndex", "columnIndex"],
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SquareDesignEntity::class,
            parentColumns = ["id"],
            childColumns = ["squareDesignId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("projectId"), Index("squareDesignId")],
)
data class ProjectCellEntity(
    val projectId: String,
    val rowIndex: Int,
    val columnIndex: Int,
    val squareDesignId: String?,
    val locked: Boolean,
    val completed: Boolean,
    val gramsPerSquareOverride: Double? = null,
)
