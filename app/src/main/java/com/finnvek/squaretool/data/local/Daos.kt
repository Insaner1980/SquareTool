package com.finnvek.squaretool.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY favorite DESC, lastOpenedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeById(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects ORDER BY favorite DESC, lastOpenedAt DESC")
    suspend fun getAll(): List<ProjectEntity>

    @Query(
        """
        SELECT * FROM projects
        WHERE name LIKE '%' || :query || '%' COLLATE NOCASE
           OR notes LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY favorite DESC, lastOpenedAt DESC
        """,
    )
    fun search(query: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE demoProject = 1 LIMIT 1")
    suspend fun getDemoProject(): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(project: ProjectEntity)

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ProjectCellDao {
    @Query("SELECT * FROM project_cells ORDER BY projectId, rowIndex, columnIndex")
    fun observeAll(): Flow<List<ProjectCellEntity>>

    @Query("SELECT * FROM project_cells WHERE projectId = :projectId ORDER BY rowIndex, columnIndex")
    fun observeForProject(projectId: String): Flow<List<ProjectCellEntity>>

    @Query("SELECT * FROM project_cells WHERE projectId = :projectId ORDER BY rowIndex, columnIndex")
    suspend fun getForProject(projectId: String): List<ProjectCellEntity>

    @Query(
        """
        SELECT * FROM project_cells
        WHERE projectId = :projectId AND rowIndex = :rowIndex AND columnIndex = :columnIndex
        """,
    )
    suspend fun getCell(
        projectId: String,
        rowIndex: Int,
        columnIndex: Int,
    ): ProjectCellEntity?

    @Upsert
    suspend fun upsert(cell: ProjectCellEntity)

    @Upsert
    suspend fun upsertAll(cells: List<ProjectCellEntity>)

    @Query("DELETE FROM project_cells WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: String)

    @Query(
        """
        DELETE FROM project_cells
        WHERE projectId = :projectId AND (rowIndex >= :rowCount OR columnIndex >= :columnCount)
        """,
    )
    suspend fun deleteOutside(
        projectId: String,
        rowCount: Int,
        columnCount: Int,
    )
}

@Dao
interface SquareDesignDao {
    @Query("SELECT * FROM square_designs ORDER BY favorite DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<SquareDesignEntity>>

    @Transaction
    @Query("SELECT * FROM square_designs ORDER BY favorite DESC, name COLLATE NOCASE")
    fun observeAllWithRounds(): Flow<List<SquareDesignWithRounds>>

    @Query("SELECT * FROM square_designs WHERE id = :id")
    suspend fun getById(id: String): SquareDesignEntity?

    @Transaction
    @Query("SELECT * FROM square_designs WHERE id = :id")
    suspend fun getWithRounds(id: String): SquareDesignWithRounds?

    @Transaction
    @Query("SELECT * FROM square_designs ORDER BY favorite DESC, name COLLATE NOCASE")
    suspend fun getAllWithRounds(): List<SquareDesignWithRounds>

    @Query(
        """
        SELECT * FROM square_designs
        WHERE name LIKE '%' || :query || '%' COLLATE NOCASE
           OR note LIKE '%' || :query || '%' COLLATE NOCASE
           OR category LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY favorite DESC, name COLLATE NOCASE
        """,
    )
    fun search(query: String): Flow<List<SquareDesignEntity>>

    @Upsert
    suspend fun upsert(design: SquareDesignEntity)

    @Query("DELETE FROM square_rounds WHERE squareDesignId = :designId")
    suspend fun deleteRounds(designId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRounds(rounds: List<SquareRoundEntity>)

    @Query("DELETE FROM square_designs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM project_cells WHERE squareDesignId = :id")
    suspend fun countCellReferences(id: String): Int

    @Query("SELECT COUNT(*) FROM projects WHERE defaultSquareDesignId = :id")
    suspend fun countDefaultProjectReferences(id: String): Int
}

@Dao
interface ColorDao {
    @Query("SELECT * FROM colors ORDER BY builtIn DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<ColorEntity>>

    @Query("SELECT * FROM colors ORDER BY builtIn DESC, name COLLATE NOCASE")
    suspend fun getAll(): List<ColorEntity>

    @Query("SELECT * FROM colors WHERE id = :id")
    suspend fun getById(id: String): ColorEntity?

    @Query(
        """
        SELECT * FROM colors
        WHERE name LIKE '%' || :query || '%' COLLATE NOCASE
           OR COALESCE(yarnBrand, '') LIKE '%' || :query || '%' COLLATE NOCASE
           OR COALESCE(yarnLine, '') LIKE '%' || :query || '%' COLLATE NOCASE
           OR COALESCE(shadeName, '') LIKE '%' || :query || '%' COLLATE NOCASE
           OR COALESCE(shadeCode, '') LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY builtIn DESC, name COLLATE NOCASE
        """,
    )
    fun search(query: String): Flow<List<ColorEntity>>

    @Upsert
    suspend fun upsert(color: ColorEntity)

    @Query("DELETE FROM colors WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM square_rounds WHERE colorId = :id")
    suspend fun countRoundReferences(id: String): Int

    @Query("SELECT COUNT(*) FROM palette_color_cross_ref WHERE colorId = :id")
    suspend fun countPaletteReferences(id: String): Int

    @Query("SELECT COUNT(*) FROM project_palette_cross_ref WHERE colorId = :id")
    suspend fun countProjectReferences(id: String): Int
}

@Dao
interface PaletteDao {
    @Query("SELECT * FROM palettes ORDER BY builtIn DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<PaletteEntity>>

    @Query("SELECT * FROM palettes ORDER BY builtIn DESC, name COLLATE NOCASE")
    suspend fun getAll(): List<PaletteEntity>

    @Query("SELECT * FROM palettes WHERE id = :id")
    suspend fun getById(id: String): PaletteEntity?

    @Query(
        """
        SELECT * FROM palettes
        WHERE name LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY builtIn DESC, name COLLATE NOCASE
        """,
    )
    fun search(query: String): Flow<List<PaletteEntity>>

    @Query(
        """
        SELECT colors.* FROM colors
        INNER JOIN palette_color_cross_ref AS links ON links.colorId = colors.id
        WHERE links.paletteId = :paletteId
        ORDER BY links.displayOrder
        """,
    )
    fun observeColors(paletteId: String): Flow<List<ColorEntity>>

    @Query(
        """
        SELECT colors.* FROM colors
        INNER JOIN palette_color_cross_ref AS links ON links.colorId = colors.id
        WHERE links.paletteId = :paletteId
        ORDER BY links.displayOrder
        """,
    )
    suspend fun getColors(paletteId: String): List<ColorEntity>

    @Query("SELECT * FROM palette_color_cross_ref WHERE paletteId = :paletteId ORDER BY displayOrder")
    suspend fun getColorRefs(paletteId: String): List<PaletteColorCrossRef>

    @Query(
        """
        SELECT colors.* FROM colors
        INNER JOIN project_palette_cross_ref AS links ON links.colorId = colors.id
        WHERE links.projectId = :projectId
        ORDER BY links.displayOrder
        """,
    )
    fun observeProjectColors(projectId: String): Flow<List<ColorEntity>>

    @Query(
        """
        SELECT colors.* FROM colors
        INNER JOIN project_palette_cross_ref AS links ON links.colorId = colors.id
        WHERE links.projectId = :projectId
        ORDER BY links.displayOrder
        """,
    )
    suspend fun getProjectColors(projectId: String): List<ColorEntity>

    @Query("SELECT * FROM project_palette_cross_ref WHERE projectId = :projectId ORDER BY displayOrder")
    suspend fun getProjectColorRefs(projectId: String): List<ProjectPaletteCrossRef>

    @Query("SELECT * FROM project_palette_cross_ref ORDER BY projectId, displayOrder")
    fun observeAllProjectColorRefs(): Flow<List<ProjectPaletteCrossRef>>

    @Upsert
    suspend fun upsert(palette: PaletteEntity)

    @Query("DELETE FROM palette_color_cross_ref WHERE paletteId = :paletteId")
    suspend fun deleteColors(paletteId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertColors(colors: List<PaletteColorCrossRef>)

    @Query("DELETE FROM project_palette_cross_ref WHERE projectId = :projectId")
    suspend fun deleteProjectColors(projectId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProjectColors(colors: List<ProjectPaletteCrossRef>)

    @Query("DELETE FROM palettes WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface BackupDao {
    @Query("SELECT * FROM projects ORDER BY id")
    suspend fun getProjects(): List<ProjectEntity>

    @Query("SELECT * FROM square_designs ORDER BY id")
    suspend fun getSquareDesigns(): List<SquareDesignEntity>

    @Query("SELECT * FROM square_rounds ORDER BY squareDesignId, roundIndex")
    suspend fun getSquareRounds(): List<SquareRoundEntity>

    @Query("SELECT * FROM colors ORDER BY id")
    suspend fun getColors(): List<ColorEntity>

    @Query("SELECT * FROM palettes ORDER BY id")
    suspend fun getPalettes(): List<PaletteEntity>

    @Query("SELECT * FROM palette_color_cross_ref ORDER BY paletteId, displayOrder")
    suspend fun getPaletteColors(): List<PaletteColorCrossRef>

    @Query("SELECT * FROM project_palette_cross_ref ORDER BY projectId, displayOrder")
    suspend fun getProjectPalettes(): List<ProjectPaletteCrossRef>

    @Query("SELECT * FROM project_cells ORDER BY projectId, rowIndex, columnIndex")
    suspend fun getProjectCells(): List<ProjectCellEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProjects(values: List<ProjectEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSquareDesigns(values: List<SquareDesignEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSquareRounds(values: List<SquareRoundEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertColors(values: List<ColorEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPalettes(values: List<PaletteEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPaletteColors(values: List<PaletteColorCrossRef>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProjectPalettes(values: List<ProjectPaletteCrossRef>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProjectCells(values: List<ProjectCellEntity>)

    @Query("DELETE FROM project_cells")
    suspend fun clearProjectCells()

    @Query("DELETE FROM project_palette_cross_ref")
    suspend fun clearProjectPalettes()

    @Query("DELETE FROM projects")
    suspend fun clearProjects()

    @Query("DELETE FROM palette_color_cross_ref")
    suspend fun clearPaletteColors()

    @Query("DELETE FROM palettes")
    suspend fun clearPalettes()

    @Query("DELETE FROM square_rounds")
    suspend fun clearSquareRounds()

    @Query("DELETE FROM square_designs")
    suspend fun clearSquareDesigns()

    @Query("DELETE FROM colors")
    suspend fun clearColors()
}
