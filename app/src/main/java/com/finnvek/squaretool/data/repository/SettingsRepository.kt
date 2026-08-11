package com.finnvek.squaretool.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.finnvek.squaretool.backup.BackupSettingsDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.squareToolSettingsDataStore by preferencesDataStore(name = "squaretool_settings")

enum class ThemePreference { SYSTEM, LIGHT, DARK }

enum class MeasurementUnitPreference { AUTOMATIC, CENTIMETERS, INCHES }

data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val preferredMeasurementUnit: MeasurementUnitPreference = MeasurementUnitPreference.AUTOMATIC,
    val defaultJoiningAndEdgingBufferPercent: Double = 10.0,
    val defaultSkeinWeightGrams: Double = 100.0,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    val showPlannerGridLines: Boolean = true,
    val confirmDestructiveLayoutGeneration: Boolean = true,
    val preserveCompletedCells: Boolean = true,
    val showLockMarkers: Boolean = true,
    val lastSelectedNavigationDestination: String = "home",
    val sampleProjectOffered: Boolean = false,
    val sampleProjectCreated: Boolean = false,
)

class SettingsRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.squareToolSettingsDataStore

    val settings: Flow<AppSettings> =
        dataStore.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }.map(::toSettings)

    suspend fun setOnboardingCompleted(value: Boolean) = set(Keys.onboardingCompleted, value)

    suspend fun setTheme(value: ThemePreference) = set(Keys.theme, value.name)

    suspend fun setPreferredMeasurementUnit(value: MeasurementUnitPreference) = set(Keys.preferredMeasurementUnit, value.name)

    suspend fun setDefaultJoiningAndEdgingBufferPercent(value: Double) {
        require(value.isFinite() && value in 0.0..100.0)
        set(Keys.defaultJoiningAndEdgingBufferPercent, value)
    }

    suspend fun setDefaultSkeinWeightGrams(value: Double) {
        require(value.isFinite() && value > 0.0)
        set(Keys.defaultSkeinWeightGrams, value)
    }

    suspend fun setHapticsEnabled(value: Boolean) = set(Keys.hapticsEnabled, value)

    suspend fun setReduceMotion(value: Boolean) = set(Keys.reduceMotion, value)

    suspend fun setShowPlannerGridLines(value: Boolean) = set(Keys.showPlannerGridLines, value)

    suspend fun setConfirmDestructiveLayoutGeneration(value: Boolean) = set(Keys.confirmDestructiveLayoutGeneration, value)

    suspend fun setPreserveCompletedCells(value: Boolean) = set(Keys.preserveCompletedCells, value)

    suspend fun setShowLockMarkers(value: Boolean) = set(Keys.showLockMarkers, value)

    suspend fun setLastSelectedNavigationDestination(value: String) {
        require(value.isNotBlank())
        set(Keys.lastSelectedNavigationDestination, value)
    }

    suspend fun setSampleProjectOffered(value: Boolean) = set(Keys.sampleProjectOffered, value)

    suspend fun setSampleProjectCreated(value: Boolean) = set(Keys.sampleProjectCreated, value)

    suspend fun backupSettings(): BackupSettingsDto = settings.first().toBackupDto()

    suspend fun restoreBackupSettings(value: BackupSettingsDto) {
        dataStore.edit { preferences ->
            preferences[Keys.theme] = ThemePreference.valueOf(value.theme.uppercase()).name
            preferences[Keys.preferredMeasurementUnit] =
                MeasurementUnitPreference.valueOf(value.preferredMeasurementUnit.uppercase()).name
            preferences[Keys.defaultJoiningAndEdgingBufferPercent] =
                value.defaultJoiningAndEdgingBufferPercent
            preferences[Keys.defaultSkeinWeightGrams] = value.defaultSkeinWeightGrams
            preferences[Keys.hapticsEnabled] = value.hapticsEnabled
            preferences[Keys.reduceMotion] = value.reduceMotion
            preferences[Keys.showPlannerGridLines] = value.showPlannerGridLines
            preferences[Keys.confirmDestructiveLayoutGeneration] =
                value.confirmDestructiveLayoutGeneration
            preferences[Keys.preserveCompletedCells] = value.preserveCompletedCells
            preferences[Keys.showLockMarkers] = value.showLockMarkers
        }
    }

    suspend fun reset() {
        dataStore.edit { it.clear() }
    }

    private suspend fun <T> set(
        key: Preferences.Key<T>,
        value: T,
    ) {
        dataStore.edit { it[key] = value }
    }

    private fun toSettings(preferences: Preferences) =
        AppSettings(
            onboardingCompleted = preferences[Keys.onboardingCompleted] ?: false,
            theme = preferences[Keys.theme].toEnumOrDefault(ThemePreference.SYSTEM),
            preferredMeasurementUnit =
                preferences[Keys.preferredMeasurementUnit]
                    .toEnumOrDefault(MeasurementUnitPreference.AUTOMATIC),
            defaultJoiningAndEdgingBufferPercent =
                preferences[Keys.defaultJoiningAndEdgingBufferPercent] ?: 10.0,
            defaultSkeinWeightGrams = preferences[Keys.defaultSkeinWeightGrams] ?: 100.0,
            hapticsEnabled = preferences[Keys.hapticsEnabled] ?: true,
            reduceMotion = preferences[Keys.reduceMotion] ?: false,
            showPlannerGridLines = preferences[Keys.showPlannerGridLines] ?: true,
            confirmDestructiveLayoutGeneration =
                preferences[Keys.confirmDestructiveLayoutGeneration] ?: true,
            preserveCompletedCells = preferences[Keys.preserveCompletedCells] ?: true,
            showLockMarkers = preferences[Keys.showLockMarkers] ?: true,
            lastSelectedNavigationDestination =
                preferences[Keys.lastSelectedNavigationDestination] ?: "home",
            sampleProjectOffered = preferences[Keys.sampleProjectOffered] ?: false,
            sampleProjectCreated = preferences[Keys.sampleProjectCreated] ?: false,
        )

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
        this?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default

    private fun AppSettings.toBackupDto() =
        BackupSettingsDto(
            theme = theme.name.lowercase(),
            preferredMeasurementUnit = preferredMeasurementUnit.name.lowercase(),
            defaultJoiningAndEdgingBufferPercent = defaultJoiningAndEdgingBufferPercent,
            defaultSkeinWeightGrams = defaultSkeinWeightGrams,
            hapticsEnabled = hapticsEnabled,
            reduceMotion = reduceMotion,
            showPlannerGridLines = showPlannerGridLines,
            confirmDestructiveLayoutGeneration = confirmDestructiveLayoutGeneration,
            preserveCompletedCells = preserveCompletedCells,
            showLockMarkers = showLockMarkers,
        )

    private object Keys {
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val theme = stringPreferencesKey("theme")
        val preferredMeasurementUnit = stringPreferencesKey("preferred_measurement_unit")
        val defaultJoiningAndEdgingBufferPercent =
            doublePreferencesKey("default_joining_and_edging_buffer_percent")
        val defaultSkeinWeightGrams = doublePreferencesKey("default_skein_weight_grams")
        val hapticsEnabled = booleanPreferencesKey("haptics_enabled")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val showPlannerGridLines = booleanPreferencesKey("show_planner_grid_lines")
        val confirmDestructiveLayoutGeneration =
            booleanPreferencesKey("confirm_destructive_layout_generation")
        val preserveCompletedCells = booleanPreferencesKey("preserve_completed_cells")
        val showLockMarkers = booleanPreferencesKey("show_lock_markers")
        val lastSelectedNavigationDestination = stringPreferencesKey("last_selected_destination")
        val sampleProjectOffered = booleanPreferencesKey("sample_project_offered")
        val sampleProjectCreated = booleanPreferencesKey("sample_project_created")
    }
}
