package com.finnvek.squaretool.data.repository

import androidx.room.withTransaction
import com.finnvek.squaretool.backup.BackupSettingsDto
import com.finnvek.squaretool.backup.BackupValidator
import com.finnvek.squaretool.backup.SquareToolBackupDto
import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.PaletteColorCrossRef
import com.finnvek.squaretool.data.local.PaletteEntity
import com.finnvek.squaretool.data.local.ProjectCellEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.local.ProjectPaletteCrossRef
import com.finnvek.squaretool.data.local.SquareDesignEntity
import com.finnvek.squaretool.data.local.SquareDesignWithRounds
import com.finnvek.squaretool.data.local.SquareRoundEntity
import com.finnvek.squaretool.data.local.SquareToolDatabase
import com.finnvek.squaretool.domain.model.CellCoordinate
import com.finnvek.squaretool.domain.model.CellState
import com.finnvek.squaretool.domain.model.GridSize
import com.finnvek.squaretool.domain.model.GridSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class ProjectResizeResult(
    val project: ProjectEntity,
    val lostCellCount: Int,
    val lostAssignedCellCount: Int,
)

data class DesignUsage(
    val projectCellCount: Int,
    val defaultProjectCount: Int,
) {
    val totalReferenceCount: Int get() = projectCellCount + defaultProjectCount
}

data class ColorUsage(
    val squareRoundCount: Int,
    val paletteCount: Int,
    val projectCount: Int,
) {
    val totalReferenceCount: Int get() = squareRoundCount + paletteCount + projectCount
}

data class LibrarySearchResult(
    val colors: List<ColorEntity>,
    val palettes: List<PaletteEntity>,
)

data class ProjectCardData(
    val projects: List<ProjectEntity>,
    val cells: List<ProjectCellEntity>,
    val designs: List<SquareDesignWithRounds>,
    val colors: List<ColorEntity>,
    val projectPaletteRefs: List<ProjectPaletteCrossRef>,
)

class SquareToolRepository(
    private val database: SquareToolDatabase,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) {
    private val projectDao = database.projectDao()
    private val cellDao = database.projectCellDao()
    private val designDao = database.squareDesignDao()
    private val colorDao = database.colorDao()
    private val paletteDao = database.paletteDao()
    private val backupDao = database.backupDao()

    fun observeProjects(): Flow<List<ProjectEntity>> = projectDao.observeAll()

    fun observeProjectCardData(): Flow<ProjectCardData> {
        val projectsAndCells =
            combine(projectDao.observeAll(), cellDao.observeAll()) { projects, cells ->
                projects to cells
            }
        val designsWithRounds = designDao.observeAllWithRounds()
        val colorsAndProjectPalettes =
            combine(
                colorDao.observeAll(),
                paletteDao.observeAllProjectColorRefs(),
            ) { colors, refs -> colors to refs }
        return combine(
            projectsAndCells,
            designsWithRounds,
            colorsAndProjectPalettes,
        ) { (projects, cells), designs, (colors, refs) ->
            ProjectCardData(
                projects = projects,
                cells = cells,
                designs = designs,
                colors = colors,
                projectPaletteRefs = refs,
            )
        }
    }

    fun observeProject(id: String): Flow<ProjectEntity?> = projectDao.observeById(id)

    fun searchProjects(query: String): Flow<List<ProjectEntity>> = projectDao.search(query.trim())

    suspend fun getProjects(): List<ProjectEntity> = projectDao.getAll()

    suspend fun getProject(id: String): ProjectEntity? = projectDao.getById(id)

    fun observeProjectCells(projectId: String): Flow<List<ProjectCellEntity>> = cellDao.observeForProject(projectId)

    suspend fun getProjectCells(projectId: String): List<ProjectCellEntity> = cellDao.getForProject(projectId)

    fun observeGrid(projectId: String): Flow<GridSnapshot?> =
        combine(projectDao.observeById(projectId), cellDao.observeForProject(projectId)) { project, cells ->
            project?.toGridSnapshot(cells)
        }

    suspend fun getGrid(projectId: String): GridSnapshot? {
        val project = projectDao.getById(projectId) ?: return null
        return project.toGridSnapshot(cellDao.getForProject(projectId))
    }

    suspend fun createProject(project: ProjectEntity) =
        database.withTransaction {
            validateProject(project)
            projectDao.insert(project)
            cellDao.upsertAll(blankCells(project.id, GridSize(project.rowCount, project.columnCount)))
        }

    suspend fun updateProject(project: ProjectEntity) =
        database.withTransaction {
            validateProject(project)
            val existing = projectDao.getById(project.id) ?: error("Project ${project.id} does not exist")
            projectDao.update(project)
            if (existing.rowCount != project.rowCount || existing.columnCount != project.columnCount) {
                syncGrid(project)
            }
        }

    suspend fun saveProjectWithLayoutAndPalette(
        project: ProjectEntity,
        orderedColorIds: List<String>,
        initialAssignments: List<String?>?,
    ): ProjectEntity =
        database.withTransaction {
            validateProject(project)
            require(orderedColorIds.distinct().size == orderedColorIds.size)
            val existing = projectDao.getById(project.id)
            if (existing == null) {
                val size = GridSize(project.rowCount, project.columnCount)
                val assignments = initialAssignments ?: List(size.cellCount) { null }
                require(assignments.size == size.cellCount) {
                    "Initial assignments must match the project grid size"
                }
                val cells =
                    blankCells(project.id, size).mapIndexed { index, cell ->
                        cell.copy(squareDesignId = assignments[index])
                    }
                validateCells(project, cells)
                projectDao.insert(project)
                cellDao.upsertAll(cells)
            } else {
                require(initialAssignments == null) {
                    "Initial assignments are only valid when creating a project"
                }
                projectDao.update(project)
                if (existing.rowCount != project.rowCount || existing.columnCount != project.columnCount) {
                    syncGrid(project)
                }
            }
            replaceProjectPalette(project.id, orderedColorIds)
            project
        }

    suspend fun resizeProject(
        projectId: String,
        rows: Int,
        columns: Int,
    ): ProjectResizeResult =
        database.withTransaction {
            val size = GridSize(rows, columns)
            val project = projectDao.getById(projectId) ?: error("Project $projectId does not exist")
            val oldCells = cellDao.getForProject(projectId)
            val discarded = oldCells.filter { it.rowIndex >= rows || it.columnIndex >= columns }
            val updated =
                project.copy(
                    rowCount = size.rows,
                    columnCount = size.columns,
                    updatedAt = currentTimeMillis(),
                )
            projectDao.update(updated)
            syncGrid(updated)
            ProjectResizeResult(
                project = updated,
                lostCellCount = discarded.size,
                lostAssignedCellCount = discarded.count { it.squareDesignId != null },
            )
        }

    suspend fun deleteProject(projectId: String) {
        projectDao.deleteById(projectId)
    }

    suspend fun duplicateProject(
        sourceProjectId: String,
        newProjectId: String,
        newName: String,
    ): ProjectEntity =
        database.withTransaction {
            require(newProjectId.isNotBlank())
            require(newName.isNotBlank())
            val source = projectDao.getById(sourceProjectId) ?: error("Project $sourceProjectId does not exist")
            val now = currentTimeMillis()
            val duplicate =
                source.copy(
                    id = newProjectId,
                    name = newName,
                    favorite = false,
                    createdAt = now,
                    updatedAt = now,
                    lastOpenedAt = now,
                    demoProject = false,
                )
            projectDao.insert(duplicate)
            val copiedCells = cellDao.getForProject(sourceProjectId).map { it.copy(projectId = newProjectId) }
            if (copiedCells.isNotEmpty()) cellDao.upsertAll(copiedCells)
            val copiedColors = paletteDao.getProjectColorRefs(sourceProjectId).map { it.copy(projectId = newProjectId) }
            if (copiedColors.isNotEmpty()) paletteDao.insertProjectColors(copiedColors)
            duplicate
        }

    suspend fun markProjectOpened(projectId: String) =
        database.withTransaction {
            val project = projectDao.getById(projectId) ?: error("Project $projectId does not exist")
            projectDao.update(project.copy(lastOpenedAt = currentTimeMillis()))
        }

    suspend fun saveCell(cell: ProjectCellEntity) = saveCells(listOf(cell))

    suspend fun saveCells(cells: List<ProjectCellEntity>) =
        database.withTransaction {
            if (cells.isEmpty()) return@withTransaction
            cells.groupBy { it.projectId }.forEach { (projectId, projectCells) ->
                val project = projectDao.getById(projectId) ?: error("Project $projectId does not exist")
                validateCells(project, projectCells)
                cellDao.upsertAll(projectCells)
                projectDao.update(project.copy(updatedAt = currentTimeMillis()))
            }
        }

    suspend fun replaceProjectCells(
        projectId: String,
        cells: List<ProjectCellEntity>,
    ) = database.withTransaction {
        val project = projectDao.getById(projectId) ?: error("Project $projectId does not exist")
        validateCells(project, cells)
        require(cells.size == project.rowCount * project.columnCount) {
            "A complete layout must contain exactly ${project.rowCount * project.columnCount} cells"
        }
        require(cells.map { it.rowIndex to it.columnIndex }.toSet().size == cells.size) {
            "A complete layout must not contain duplicate coordinates"
        }
        cellDao.deleteForProject(projectId)
        cellDao.upsertAll(cells)
        projectDao.update(project.copy(updatedAt = currentTimeMillis()))
    }

    fun observeDesigns(): Flow<List<SquareDesignEntity>> = designDao.observeAll()

    fun observeDesignsWithRounds(): Flow<List<SquareDesignWithRounds>> = designDao.observeAllWithRounds()

    fun searchDesigns(query: String): Flow<List<SquareDesignEntity>> = designDao.search(query.trim())

    suspend fun getDesign(id: String): SquareDesignEntity? = designDao.getById(id)

    suspend fun getDesignWithRounds(id: String): SquareDesignWithRounds? = designDao.getWithRounds(id)

    suspend fun getDesignsWithRounds(): List<SquareDesignWithRounds> = designDao.getAllWithRounds()

    suspend fun saveDesign(
        design: SquareDesignEntity,
        rounds: List<SquareRoundEntity>,
    ) = database.withTransaction {
        validateRounds(design.id, rounds)
        designDao.upsert(design)
        designDao.deleteRounds(design.id)
        designDao.insertRounds(rounds)
    }

    suspend fun getDesignUsage(id: String): DesignUsage =
        database.withTransaction {
            DesignUsage(
                projectCellCount = designDao.countCellReferences(id),
                defaultProjectCount = designDao.countDefaultProjectReferences(id),
            )
        }

    suspend fun deleteDesignIfUnused(id: String): Boolean =
        database.withTransaction {
            val usage =
                DesignUsage(
                    projectCellCount = designDao.countCellReferences(id),
                    defaultProjectCount = designDao.countDefaultProjectReferences(id),
                )
            if (usage.totalReferenceCount != 0) return@withTransaction false
            designDao.deleteById(id)
            true
        }

    suspend fun deleteDesign(id: String) {
        check(deleteDesignIfUnused(id)) { "Square design $id is still in use" }
    }

    fun observeColors(): Flow<List<ColorEntity>> = colorDao.observeAll()

    fun searchColors(query: String): Flow<List<ColorEntity>> = colorDao.search(query.trim())

    suspend fun getColors(): List<ColorEntity> = colorDao.getAll()

    suspend fun getColor(id: String): ColorEntity? = colorDao.getById(id)

    suspend fun saveColor(color: ColorEntity) {
        require(color.id.isNotBlank() && color.name.isNotBlank())
        require(color.argb in 0L..0xFFFF_FFFFL) { "ARGB must be an unsigned 32-bit value" }
        colorDao.upsert(color)
    }

    suspend fun getColorUsage(id: String): ColorUsage =
        database.withTransaction {
            ColorUsage(
                squareRoundCount = colorDao.countRoundReferences(id),
                paletteCount = colorDao.countPaletteReferences(id),
                projectCount = colorDao.countProjectReferences(id),
            )
        }

    suspend fun deleteColorIfUnused(id: String): Boolean =
        database.withTransaction {
            val usage =
                ColorUsage(
                    squareRoundCount = colorDao.countRoundReferences(id),
                    paletteCount = colorDao.countPaletteReferences(id),
                    projectCount = colorDao.countProjectReferences(id),
                )
            if (usage.totalReferenceCount != 0) return@withTransaction false
            colorDao.deleteById(id)
            true
        }

    suspend fun deleteColor(id: String) {
        check(deleteColorIfUnused(id)) { "Color $id is still in use" }
    }

    fun observePalettes(): Flow<List<PaletteEntity>> = paletteDao.observeAll()

    suspend fun getPalettes(): List<PaletteEntity> = paletteDao.getAll()

    suspend fun getPalette(id: String): PaletteEntity? = paletteDao.getById(id)

    fun searchPalettes(query: String): Flow<List<PaletteEntity>> = paletteDao.search(query.trim())

    fun searchLibrary(query: String): Flow<LibrarySearchResult> =
        combine(colorDao.search(query.trim()), paletteDao.search(query.trim())) { colors, palettes ->
            LibrarySearchResult(colors, palettes)
        }

    fun observePaletteColors(paletteId: String): Flow<List<ColorEntity>> = paletteDao.observeColors(paletteId)

    suspend fun getPaletteColors(paletteId: String): List<ColorEntity> = paletteDao.getColors(paletteId)

    suspend fun savePalette(
        palette: PaletteEntity,
        colors: List<PaletteColorCrossRef>,
    ) = database.withTransaction {
        require(colors.all { it.paletteId == palette.id && it.displayOrder >= 0 })
        require(colors.map { it.colorId }.distinct().size == colors.size)
        require(colors.map { it.displayOrder }.distinct().size == colors.size)
        paletteDao.upsert(palette)
        paletteDao.deleteColors(palette.id)
        if (colors.isNotEmpty()) paletteDao.insertColors(colors)
    }

    suspend fun deletePalette(id: String) {
        paletteDao.deleteById(id)
    }

    fun observeProjectPalette(projectId: String): Flow<List<ColorEntity>> = paletteDao.observeProjectColors(projectId)

    suspend fun getProjectPalette(projectId: String): List<ColorEntity> = paletteDao.getProjectColors(projectId)

    suspend fun setProjectPalette(
        projectId: String,
        orderedColorIds: List<String>,
    ) = database.withTransaction {
        require(projectDao.getById(projectId) != null) { "Project $projectId does not exist" }
        require(orderedColorIds.distinct().size == orderedColorIds.size)
        replaceProjectPalette(projectId, orderedColorIds)
    }

    suspend fun applyPaletteToProject(
        projectId: String,
        paletteId: String,
    ) = database.withTransaction {
        require(projectDao.getById(projectId) != null) { "Project $projectId does not exist" }
        require(paletteDao.getById(paletteId) != null) { "Palette $paletteId does not exist" }
        val refs = paletteDao.getColorRefs(paletteId)
        paletteDao.deleteProjectColors(projectId)
        if (refs.isNotEmpty()) {
            paletteDao.insertProjectColors(
                refs.map { ProjectPaletteCrossRef(projectId, it.colorId, it.displayOrder) },
            )
        }
    }

    suspend fun createBackup(
        exportedAtEpochMillis: Long = currentTimeMillis(),
        settings: BackupSettingsDto? = null,
    ): SquareToolBackupDto =
        database.withTransaction {
            SquareToolBackupDto(
                exportedAtEpochMillis = exportedAtEpochMillis,
                projects = backupDao.getProjects().map { it.toBackupDto() },
                squareDesigns = backupDao.getSquareDesigns().map { it.toBackupDto() },
                squareRounds = backupDao.getSquareRounds().map { it.toBackupDto() },
                colors = backupDao.getColors().map { it.toBackupDto() },
                palettes = backupDao.getPalettes().map { it.toBackupDto() },
                paletteColors = backupDao.getPaletteColors().map { it.toBackupDto() },
                projectPalettes = backupDao.getProjectPalettes().map { it.toBackupDto() },
                projectCells = backupDao.getProjectCells().map { it.toBackupDto() },
                settings = settings,
            )
        }

    suspend fun restoreBackup(backup: SquareToolBackupDto) {
        BackupValidator.requireValid(backup)
        database.withTransaction {
            clearAllTables()
            insertIfNotEmpty(backup.colors.map { it.toEntity() }, backupDao::insertColors)
            insertIfNotEmpty(backup.squareDesigns.map { it.toEntity() }, backupDao::insertSquareDesigns)
            insertIfNotEmpty(backup.squareRounds.map { it.toEntity() }, backupDao::insertSquareRounds)
            insertIfNotEmpty(backup.palettes.map { it.toEntity() }, backupDao::insertPalettes)
            insertIfNotEmpty(backup.paletteColors.map { it.toEntity() }, backupDao::insertPaletteColors)
            insertIfNotEmpty(backup.projects.map { it.toEntity() }, backupDao::insertProjects)
            insertIfNotEmpty(backup.projectPalettes.map { it.toEntity() }, backupDao::insertProjectPalettes)
            insertIfNotEmpty(backup.projectCells.map { it.toEntity() }, backupDao::insertProjectCells)
        }
    }

    suspend fun deleteAll() =
        database.withTransaction {
            clearAllTables()
        }

    suspend fun createSampleProject(): ProjectEntity =
        database.withTransaction {
            projectDao.getDemoProject()?.let { return@withTransaction it }
            val sample = SampleDataFactory.create(currentTimeMillis())
            sample.colors.forEach { colorDao.upsert(it) }
            sample.designs.forEach { (design, rounds) ->
                designDao.upsert(design)
                designDao.deleteRounds(design.id)
                designDao.insertRounds(rounds)
            }
            paletteDao.upsert(sample.palette)
            paletteDao.deleteColors(sample.palette.id)
            paletteDao.insertColors(sample.paletteColors)
            projectDao.insert(sample.project)
            paletteDao.insertProjectColors(sample.projectColors)
            cellDao.upsertAll(sample.cells)
            sample.project
        }

    private suspend fun syncGrid(project: ProjectEntity) {
        val size = GridSize(project.rowCount, project.columnCount)
        cellDao.deleteOutside(project.id, size.rows, size.columns)
        val existing = cellDao.getForProject(project.id).associateBy { it.rowIndex to it.columnIndex }
        val missing =
            size
                .coordinates()
                .mapNotNull { coordinate ->
                    if ((coordinate.row to coordinate.column) in existing) {
                        null
                    } else {
                        ProjectCellEntity(project.id, coordinate.row, coordinate.column, null, false, false)
                    }
                }.toList()
        if (missing.isNotEmpty()) cellDao.upsertAll(missing)
    }

    private suspend fun replaceProjectPalette(
        projectId: String,
        orderedColorIds: List<String>,
    ) {
        paletteDao.deleteProjectColors(projectId)
        if (orderedColorIds.isNotEmpty()) {
            paletteDao.insertProjectColors(
                orderedColorIds.mapIndexed { index, colorId ->
                    ProjectPaletteCrossRef(projectId, colorId, index)
                },
            )
        }
    }

    private fun validateProject(project: ProjectEntity) {
        require(project.id.isNotBlank()) { "Project ID must not be blank" }
        require(project.name.isNotBlank()) { "Project name must not be blank" }
        GridSize(project.rowCount, project.columnCount)
        listOf(project.squareWidthValue, project.squareHeightValue).forEach { value ->
            require(value == null || (value.isFinite() && value > 0.0)) {
                "Square dimensions must be finite and greater than zero"
            }
        }
        require(
            project.joiningGapValue == null ||
                (project.joiningGapValue.isFinite() && project.joiningGapValue >= 0.0),
        )
        require(
            project.joiningAndEdgingBufferPercent.isFinite() &&
                project.joiningAndEdgingBufferPercent in 0.0..100.0,
        )
    }

    private fun validateCells(
        project: ProjectEntity,
        cells: List<ProjectCellEntity>,
    ) {
        val size = GridSize(project.rowCount, project.columnCount)
        cells.forEach { cell ->
            require(cell.projectId == project.id)
            require(size.contains(CellCoordinate(cell.rowIndex, cell.columnIndex))) {
                "Cell ${cell.rowIndex},${cell.columnIndex} is outside project dimensions"
            }
            require(
                cell.gramsPerSquareOverride == null ||
                    (cell.gramsPerSquareOverride.isFinite() && cell.gramsPerSquareOverride > 0.0),
            )
        }
    }

    private fun validateRounds(
        designId: String,
        rounds: List<SquareRoundEntity>,
    ) {
        require(rounds.size in 3..6) { "A square design must have three to six rounds" }
        require(rounds.all { it.squareDesignId == designId })
        require(rounds.map { it.roundIndex }.sorted() == rounds.indices.toList()) {
            "Round indices must be contiguous and zero-based"
        }
    }

    private suspend fun clearAllTables() {
        backupDao.clearProjectCells()
        backupDao.clearProjectPalettes()
        backupDao.clearProjects()
        backupDao.clearPaletteColors()
        backupDao.clearPalettes()
        backupDao.clearSquareRounds()
        backupDao.clearSquareDesigns()
        backupDao.clearColors()
    }

    private suspend fun <T> insertIfNotEmpty(
        values: List<T>,
        insert: suspend (List<T>) -> Unit,
    ) {
        if (values.isNotEmpty()) insert(values)
    }

    private fun blankCells(
        projectId: String,
        size: GridSize,
    ): List<ProjectCellEntity> =
        size
            .coordinates()
            .map { coordinate ->
                ProjectCellEntity(projectId, coordinate.row, coordinate.column, null, false, false)
            }.toList()
}

internal fun ProjectEntity.toGridSnapshot(cells: List<ProjectCellEntity>): GridSnapshot =
    GridSnapshot.of(
        GridSize(rowCount, columnCount),
        cells
            .asSequence()
            .filter { it.rowIndex in 0 until rowCount && it.columnIndex in 0 until columnCount }
            .map {
                CellState(
                    coordinate = CellCoordinate(it.rowIndex, it.columnIndex),
                    designId = it.squareDesignId,
                    locked = it.locked,
                    completed = it.completed,
                    gramsPerSquareOverride = it.gramsPerSquareOverride,
                )
            }.asIterable(),
    )
