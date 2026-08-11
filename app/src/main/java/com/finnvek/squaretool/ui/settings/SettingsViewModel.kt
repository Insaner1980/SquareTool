package com.finnvek.squaretool.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.finnvek.squaretool.data.repository.AppSettings
import com.finnvek.squaretool.data.repository.MeasurementUnitPreference
import com.finnvek.squaretool.data.repository.SettingsRepository
import com.finnvek.squaretool.data.repository.ThemePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val loading: Boolean = false,
)

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        repository.settings
            .map { SettingsUiState(settings = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(loading = true))

    fun setTheme(value: ThemePreference) = update { repository.setTheme(value) }

    fun setUnit(value: MeasurementUnitPreference) = update { repository.setPreferredMeasurementUnit(value) }

    fun setReduceMotion(value: Boolean) = update { repository.setReduceMotion(value) }

    fun setHaptics(value: Boolean) = update { repository.setHapticsEnabled(value) }

    fun setGridLines(value: Boolean) = update { repository.setShowPlannerGridLines(value) }

    fun setConfirmLayout(value: Boolean) = update { repository.setConfirmDestructiveLayoutGeneration(value) }

    fun setPreserveCompleted(value: Boolean) = update { repository.setPreserveCompletedCells(value) }

    fun setShowLocks(value: Boolean) = update { repository.setShowLockMarkers(value) }

    fun setBuffer(value: Double) = update { repository.setDefaultJoiningAndEdgingBufferPercent(value) }

    fun setSkeinWeight(value: Double) = update { repository.setDefaultSkeinWeightGrams(value) }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    class Factory(
        private val repository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(repository) as T
    }
}
