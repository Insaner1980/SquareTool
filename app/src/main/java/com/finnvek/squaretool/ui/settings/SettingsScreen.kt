package com.finnvek.squaretool.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finnvek.squaretool.R
import com.finnvek.squaretool.data.repository.MeasurementUnitPreference
import com.finnvek.squaretool.data.repository.SettingsRepository
import com.finnvek.squaretool.data.repository.ThemePreference
import com.finnvek.squaretool.ui.theme.SquareToolSpacing
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale

@Composable
fun SettingsRoute(
    repository: SettingsRepository,
    onOpenBackup: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAccessiblePlanner: () -> Unit,
    onDeleteAllData: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onThemeChange = viewModel::setTheme,
        onUnitChange = viewModel::setUnit,
        onReduceMotionChange = viewModel::setReduceMotion,
        onHapticsChange = viewModel::setHaptics,
        onGridLinesChange = viewModel::setGridLines,
        onConfirmLayoutChange = viewModel::setConfirmLayout,
        onPreserveCompletedChange = viewModel::setPreserveCompleted,
        onShowLocksChange = viewModel::setShowLocks,
        onBufferChange = viewModel::setBuffer,
        onSkeinWeightChange = viewModel::setSkeinWeight,
        onOpenBackup = onOpenBackup,
        onOpenAbout = onOpenAbout,
        onOpenAccessiblePlanner = onOpenAccessiblePlanner,
        onDeleteAllData = onDeleteAllData,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    modifier: Modifier = Modifier,
    onThemeChange: (ThemePreference) -> Unit = {},
    onUnitChange: (MeasurementUnitPreference) -> Unit = {},
    onReduceMotionChange: (Boolean) -> Unit = {},
    onHapticsChange: (Boolean) -> Unit = {},
    onGridLinesChange: (Boolean) -> Unit = {},
    onConfirmLayoutChange: (Boolean) -> Unit = {},
    onPreserveCompletedChange: (Boolean) -> Unit = {},
    onShowLocksChange: (Boolean) -> Unit = {},
    onBufferChange: (Double) -> Unit = {},
    onSkeinWeightChange: (Double) -> Unit = {},
    onOpenBackup: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenAccessiblePlanner: () -> Unit = {},
    onDeleteAllData: suspend () -> Unit = {},
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val settings = state.settings
    LazyColumn(
        modifier = modifier.testTag("settings_list"),
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(bottom = SquareToolSpacing.ExtraLarge),
    ) {
        item { ScreenTitle(stringResource(R.string.settings_title)) }
        item { SectionTitle(stringResource(R.string.appearance)) }
        item {
            Text(
                stringResource(R.string.theme),
                modifier = Modifier.padding(horizontal = SquareToolSpacing.Standard),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(SquareToolSpacing.Standard),
                horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small),
            ) {
                ThemePreference.entries.forEach { preference ->
                    val label =
                        when (preference) {
                            ThemePreference.SYSTEM -> R.string.theme_system
                            ThemePreference.LIGHT -> R.string.theme_light
                            ThemePreference.DARK -> R.string.theme_dark
                        }
                    FilterChip(
                        selected = settings.theme == preference,
                        onClick = { onThemeChange(preference) },
                        label = { Text(stringResource(label)) },
                        modifier = Modifier.testTag("theme_${preference.name.lowercase()}"),
                    )
                }
            }
        }
        item { SwitchSetting(R.string.reduce_motion, R.string.reduce_motion_summary, settings.reduceMotion, onReduceMotionChange) }
        item { SwitchSetting(R.string.haptics, R.string.haptics_summary, settings.hapticsEnabled, onHapticsChange) }
        item { SectionTitle(stringResource(R.string.units_and_defaults)) }
        item {
            Text(
                stringResource(R.string.preferred_unit),
                modifier = Modifier.padding(horizontal = SquareToolSpacing.Standard),
                style = MaterialTheme.typography.titleMedium,
            )
            Column(Modifier.padding(SquareToolSpacing.Standard), verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small)) {
                MeasurementUnitPreference.entries.forEach { preference ->
                    val label =
                        when (preference) {
                            MeasurementUnitPreference.AUTOMATIC -> R.string.unit_automatic
                            MeasurementUnitPreference.CENTIMETERS -> R.string.unit_centimeters
                            MeasurementUnitPreference.INCHES -> R.string.unit_inches
                        }
                    FilterChip(
                        selected = settings.preferredMeasurementUnit == preference,
                        onClick = { onUnitChange(preference) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
        }
        item {
            NumberSetting(
                title = stringResource(R.string.default_buffer),
                value = settings.defaultJoiningAndEdgingBufferPercent,
                display = stringResource(R.string.percent_value, format(settings.defaultJoiningAndEdgingBufferPercent)),
                isValid = { it in 0.0..100.0 },
                errorText = stringResource(R.string.invalid_percentage),
                onSave = onBufferChange,
            )
        }
        item {
            NumberSetting(
                title = stringResource(R.string.default_skein_weight),
                value = settings.defaultSkeinWeightGrams,
                display = stringResource(R.string.grams_value, format(settings.defaultSkeinWeightGrams)),
                isValid = { it > 0.0 },
                errorText = stringResource(R.string.invalid_positive_number),
                onSave = onSkeinWeightChange,
            )
        }
        item { SectionTitle(stringResource(R.string.planner_settings)) }
        item { SwitchSetting(R.string.show_grid_lines, null, settings.showPlannerGridLines, onGridLinesChange) }
        item {
            SwitchSetting(
                R.string.confirm_layout_replacement,
                null,
                settings.confirmDestructiveLayoutGeneration,
                onConfirmLayoutChange,
            )
        }
        item { SwitchSetting(R.string.preserve_completed, null, settings.preserveCompletedCells, onPreserveCompletedChange) }
        item { SwitchSetting(R.string.show_lock_markers, null, settings.showLockMarkers, onShowLocksChange) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.accessible_grid_mode)) },
                supportingContent = { Text(stringResource(R.string.accessible_grid_mode_summary)) },
                leadingContent = { Icon(Icons.Outlined.AccessibilityNew, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenAccessiblePlanner),
            )
        }
        item { SectionTitle(stringResource(R.string.data_and_privacy)) }
        item {
            Column(Modifier.padding(horizontal = SquareToolSpacing.Standard, vertical = SquareToolSpacing.Small)) {
                Text(stringResource(R.string.offline_statement), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.local_data_statement),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { ActionSetting(R.string.export_full_backup, Icons.Outlined.SaveAlt, onOpenBackup) }
        item { ActionSetting(R.string.restore_backup, Icons.Outlined.Restore, onOpenBackup) }
        item { ActionSetting(R.string.delete_all_data, Icons.Outlined.DeleteForever) { confirmDelete = true } }
        item { SectionTitle(stringResource(R.string.about)) }
        item { ActionSetting(R.string.about_and_privacy, Icons.Outlined.Info, onOpenAbout) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_all_data_title)) },
            text = { Text(stringResource(R.string.delete_all_data_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        scope.launch { onDeleteAllData() }
                    },
                ) { Text(stringResource(R.string.delete_everything)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ScreenTitle(text: String) {
    Text(
        text,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SquareToolSpacing.Standard,
                    vertical = SquareToolSpacing.Section,
                ).semantics {
                    heading()
                },
        style = MaterialTheme.typography.headlineLarge,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = SquareToolSpacing.Standard,
                    end = SquareToolSpacing.Standard,
                    top = SquareToolSpacing.Section,
                    bottom = SquareToolSpacing.Small,
                ).semantics {
                    heading()
                },
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    HorizontalDivider()
}

@Composable
private fun SwitchSetting(
    titleRes: Int,
    summaryRes: Int?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(titleRes)) },
        supportingContent = summaryRes?.let { { Text(stringResource(it)) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
    )
}

@Composable
private fun ActionSetting(
    titleRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(titleRes)) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun NumberSetting(
    title: String,
    value: Double,
    display: String,
    isValid: (Double) -> Boolean,
    errorText: String,
    onSave: (Double) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(display) },
        modifier = Modifier.clickable { editing = true },
    )
    if (editing) {
        var text by remember(value) { mutableStateOf(format(value)) }
        val parsed = parseLocalizedNumber(text)
        val error = parsed != null && !isValid(parsed)
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(title) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.edit_value)) },
                    isError = error,
                    supportingText = if (error) ({ Text(errorText) }) else null,
                    singleLine = true,
                    modifier = Modifier.widthIn(min = 240.dp),
                )
            },
            confirmButton = {
                Button(
                    enabled = parsed != null && isValid(parsed),
                    onClick = {
                        onSave(requireNotNull(parsed))
                        editing = false
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun format(value: Double): String = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(value)

internal fun parseLocalizedNumber(
    text: String,
    locale: Locale = Locale.getDefault(),
): Double? {
    val value = text.trim()
    if (value.isEmpty()) return null
    val position = ParsePosition(0)
    val parsed = NumberFormat.getNumberInstance(locale).parse(value, position)?.toDouble()
    if (parsed != null && position.index == value.length && parsed.isFinite()) return parsed

    return value
        .replace("\u00A0", "")
        .replace("\u202F", "")
        .replace(" ", "")
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf(Double::isFinite)
}
