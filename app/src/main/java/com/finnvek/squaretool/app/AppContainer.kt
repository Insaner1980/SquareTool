package com.finnvek.squaretool.app

import android.content.Context
import com.finnvek.squaretool.backup.BackupService
import com.finnvek.squaretool.data.local.SquareToolDatabase
import com.finnvek.squaretool.data.repository.SettingsRepository
import com.finnvek.squaretool.data.repository.SquareToolRepository

class AppContainer(
    context: Context,
) {
    val database: SquareToolDatabase = SquareToolDatabase.create(context)
    val repository = SquareToolRepository(database)
    val settingsRepository = SettingsRepository(context)
    val backupService = BackupService(repository, settingsRepository)

    suspend fun createSampleProject(): String {
        val project = repository.createSampleProject()
        settingsRepository.setSampleProjectOffered(true)
        settingsRepository.setSampleProjectCreated(true)
        return project.id
    }

    suspend fun deleteAllData() {
        repository.deleteAll()
        settingsRepository.reset()
    }
}
