package com.finnvek.squaretool.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.squaretool.backup.BackupSettingsDto
import com.finnvek.squaretool.data.repository.MeasurementUnitPreference
import com.finnvek.squaretool.data.repository.SettingsRepository
import com.finnvek.squaretool.data.repository.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() =
        runBlocking {
            repository = SettingsRepository(ApplicationProvider.getApplicationContext<Context>())
            repository.reset()
        }

    @After
    fun tearDown() =
        runBlocking {
            repository.reset()
        }

    @Test
    fun defaultsArePrivateAndPlannerFriendly() =
        runTest {
            val settings = repository.settings.first()

            assertFalse(settings.onboardingCompleted)
            assertEquals(ThemePreference.SYSTEM, settings.theme)
            assertEquals(MeasurementUnitPreference.AUTOMATIC, settings.preferredMeasurementUnit)
            assertTrue(settings.hapticsEnabled)
            assertTrue(settings.showPlannerGridLines)
            assertTrue(settings.confirmDestructiveLayoutGeneration)
            assertTrue(settings.preserveCompletedCells)
        }

    @Test
    fun backupSettingsCanBeRestored() =
        runTest {
            val backup =
                BackupSettingsDto(
                    theme = "dark",
                    preferredMeasurementUnit = "inches",
                    defaultJoiningAndEdgingBufferPercent = 15.0,
                    defaultSkeinWeightGrams = 50.0,
                    hapticsEnabled = false,
                    reduceMotion = true,
                    showPlannerGridLines = false,
                    confirmDestructiveLayoutGeneration = false,
                    preserveCompletedCells = false,
                    showLockMarkers = false,
                )

            repository.restoreBackupSettings(backup)
            val settings = repository.settings.first()

            assertEquals(ThemePreference.DARK, settings.theme)
            assertEquals(MeasurementUnitPreference.INCHES, settings.preferredMeasurementUnit)
            assertEquals(15.0, settings.defaultJoiningAndEdgingBufferPercent, 0.0)
            assertEquals(50.0, settings.defaultSkeinWeightGrams, 0.0)
            assertFalse(settings.hapticsEnabled)
            assertTrue(settings.reduceMotion)
        }
}
