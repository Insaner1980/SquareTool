package com.finnvek.squaretool.backup

import com.finnvek.squaretool.data.repository.SettingsRepository
import com.finnvek.squaretool.data.repository.SquareToolRepository

data class BackupSummary(
    val projectCount: Int,
    val squareDesignCount: Int,
    val colorCount: Int,
    val paletteCount: Int,
    val cellCount: Int,
)

class BackupService(
    private val repository: SquareToolRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun createJson(exportedAtEpochMillis: Long = System.currentTimeMillis()): String {
        val backup =
            repository.createBackup(
                exportedAtEpochMillis = exportedAtEpochMillis,
                settings = settingsRepository.backupSettings(),
            )
        return BackupCodec.encode(backup)
    }

    suspend fun createProjectJson(
        projectId: String,
        exportedAtEpochMillis: Long = System.currentTimeMillis(),
    ): String =
        BackupCodec.encode(
            repository
                .createBackup(exportedAtEpochMillis = exportedAtEpochMillis)
                .forProject(projectId),
        )

    fun decodeAndValidate(json: String): SquareToolBackupDto = BackupCodec.decode(json).also(BackupValidator::requireValid)

    fun summary(backup: SquareToolBackupDto) =
        BackupSummary(
            projectCount = backup.projects.size,
            squareDesignCount = backup.squareDesigns.size,
            colorCount = backup.colors.size,
            paletteCount = backup.palettes.size,
            cellCount = backup.projectCells.size,
        )

    suspend fun restore(backup: SquareToolBackupDto) {
        BackupValidator.requireValid(backup)
        val previousSettings = settingsRepository.backupSettings()
        restoreWithRollback(
            applySettings = {
                backup.settings?.let { settingsRepository.restoreBackupSettings(it) }
            },
            applyDatabase = { repository.restoreBackup(backup) },
            rollbackSettings = {
                settingsRepository.restoreBackupSettings(previousSettings)
            },
        )
    }

    suspend fun restoreJson(json: String) {
        restore(decodeAndValidate(json))
    }
}

internal fun SquareToolBackupDto.forProject(projectId: String): SquareToolBackupDto {
    val project =
        requireNotNull(projects.firstOrNull { it.id == projectId }) {
            "Project $projectId does not exist"
        }
    val projectCells = projectCells.filter { it.projectId == projectId }
    val projectPalette = projectPalettes.filter { it.projectId == projectId }
    val designIds =
        buildSet {
            project.defaultSquareDesignId?.let(::add)
            projectCells.mapNotNullTo(this) { it.squareDesignId }
        }
    val projectRounds = squareRounds.filter { it.squareDesignId in designIds }
    val colorIds =
        buildSet {
            projectRounds.mapTo(this) { it.colorId }
            projectPalette.mapTo(this) { it.colorId }
        }
    return copy(
        projects = listOf(project),
        squareDesigns = squareDesigns.filter { it.id in designIds },
        squareRounds = projectRounds,
        colors = colors.filter { it.id in colorIds },
        palettes = emptyList(),
        paletteColors = emptyList(),
        projectPalettes = projectPalette,
        projectCells = projectCells,
        settings = null,
    )
}

@Suppress("TooGenericExceptionCaught")
internal suspend fun restoreWithRollback(
    applySettings: suspend () -> Unit,
    applyDatabase: suspend () -> Unit,
    rollbackSettings: suspend () -> Unit,
) {
    try {
        applySettings()
        applyDatabase()
    } catch (failure: Throwable) {
        try {
            rollbackSettings()
        } catch (rollbackFailure: Throwable) {
            failure.addSuppressed(rollbackFailure)
        }
        throw failure
    }
}
