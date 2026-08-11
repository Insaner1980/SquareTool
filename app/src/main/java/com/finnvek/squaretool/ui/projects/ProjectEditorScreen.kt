package com.finnvek.squaretool.ui.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finnvek.squaretool.R
import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.ProjectCellEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.local.SquareRoundEntity
import com.finnvek.squaretool.data.repository.AppSettings
import com.finnvek.squaretool.data.repository.SquareToolRepository
import com.finnvek.squaretool.domain.model.MeasurementUnit
import com.finnvek.squaretool.ui.theme.SquareToolSpacing

@Composable
fun ProjectEditorRoute(
    repository: SquareToolRepository,
    settings: AppSettings,
    modifier: Modifier = Modifier,
    projectId: String? = null,
    onClose: () -> Unit = {},
    onSaved: (String) -> Unit = {},
) {
    val editorViewModel: ProjectEditorViewModel =
        viewModel(
            key = "project-editor-$projectId",
            factory = ProjectEditorViewModel.factory(repository, projectId, settings),
        )
    val state by editorViewModel.uiState.collectAsStateWithLifecycle()
    ProjectEditorScreen(
        state = state,
        isEditing = projectId != null,
        onNameChange = editorViewModel::updateName,
        onRowsChange = editorViewModel::updateRows,
        onColumnsChange = editorViewModel::updateColumns,
        onUnitChange = editorViewModel::updateUnit,
        onSquareWidthChange = editorViewModel::updateSquareWidth,
        onSquareHeightChange = editorViewModel::updateSquareHeight,
        onJoiningGapChange = editorViewModel::updateJoiningGap,
        onTrackingChange = editorViewModel::updateTracking,
        onToggleColor = editorViewModel::toggleColor,
        onInitialFillChange = editorViewModel::updateInitialFill,
        onToggleDesign = editorViewModel::toggleDesign,
        onGlobalGramsChange = editorViewModel::updateGlobalGrams,
        onSkeinWeightChange = editorViewModel::updateSkeinWeight,
        onBufferChange = editorViewModel::updateBuffer,
        onNotesChange = editorViewModel::updateNotes,
        onSave = { editorViewModel.save(onSaved) },
        onCancel = onClose,
        onConfirmShrink = { editorViewModel.confirmShrink(onSaved) },
        onCancelShrink = editorViewModel::cancelShrink,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectEditorScreen(
    state: ProjectEditorUiState,
    isEditing: Boolean,
    modifier: Modifier = Modifier,
    onNameChange: (String) -> Unit = {},
    onRowsChange: (String) -> Unit = {},
    onColumnsChange: (String) -> Unit = {},
    onUnitChange: (MeasurementUnit) -> Unit = {},
    onSquareWidthChange: (String) -> Unit = {},
    onSquareHeightChange: (String) -> Unit = {},
    onJoiningGapChange: (String) -> Unit = {},
    onTrackingChange: (Boolean) -> Unit = {},
    onToggleColor: (String) -> Unit = {},
    onInitialFillChange: (InitialProjectFill) -> Unit = {},
    onToggleDesign: (String) -> Unit = {},
    onGlobalGramsChange: (String) -> Unit = {},
    onSkeinWeightChange: (String) -> Unit = {},
    onBufferChange: (String) -> Unit = {},
    onNotesChange: (String) -> Unit = {},
    onSave: () -> Unit = {},
    onCancel: () -> Unit = {},
    onConfirmShrink: () -> Unit = {},
    onCancelShrink: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.testTag("project_editor"),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (isEditing) R.string.project_editor_edit_title else R.string.project_editor_new_title))
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.project_editor_cancel))
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val preview =
                remember(state.draft, state.designs, state.colors, state.basePreview) {
                    createDraftPreview(state)
                }
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = SquareToolSpacing.Standard, vertical = SquareToolSpacing.Medium),
                verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Section),
            ) {
                EditorHeading(stringResource(R.string.project_editor_live_preview))
                ProjectBlanketPreview(
                    project = preview,
                    contentDescription = stringResource(R.string.project_editor_live_preview),
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.45f).testTag("project_live_preview"),
                )

                EditorHeading(stringResource(R.string.project_editor_basics))
                OutlinedTextField(
                    value = state.draft.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.project_editor_name)) },
                    modifier = Modifier.fillMaxWidth().testTag("project_name"),
                    isError = ProjectDraftError.NAME in state.validationErrors,
                    supportingText =
                        errorText(
                            ProjectDraftError.NAME in state.validationErrors,
                            R.string.project_editor_name_error,
                        ),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium)) {
                    NumberField(
                        value = state.draft.rows.toString(),
                        onValueChange = onRowsChange,
                        label = stringResource(R.string.project_editor_rows),
                        isError = ProjectDraftError.ROWS in state.validationErrors,
                        modifier = Modifier.weight(1f).testTag("project_rows"),
                    )
                    NumberField(
                        value = state.draft.columns.toString(),
                        onValueChange = onColumnsChange,
                        label = stringResource(R.string.project_editor_columns),
                        isError = ProjectDraftError.COLUMNS in state.validationErrors,
                        modifier = Modifier.weight(1f).testTag("project_columns"),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small)) {
                    FilterChip(
                        selected = state.draft.measurementUnit == MeasurementUnit.CENTIMETERS,
                        onClick = { onUnitChange(MeasurementUnit.CENTIMETERS) },
                        label = { Text(stringResource(R.string.project_editor_centimeters)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                    FilterChip(
                        selected = state.draft.measurementUnit == MeasurementUnit.INCHES,
                        onClick = { onUnitChange(MeasurementUnit.INCHES) },
                        label = { Text(stringResource(R.string.project_editor_inches)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }

                EditorHeading(stringResource(R.string.project_editor_measurements))
                Text(
                    stringResource(R.string.project_editor_measurements_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DecimalField(
                    state.draft.squareWidth,
                    onSquareWidthChange,
                    stringResource(R.string.project_editor_square_width),
                    ProjectDraftError.SQUARE_WIDTH in state.validationErrors,
                )
                DecimalField(
                    state.draft.squareHeight,
                    onSquareHeightChange,
                    stringResource(R.string.project_editor_square_height),
                    ProjectDraftError.SQUARE_HEIGHT in state.validationErrors,
                )
                DecimalField(
                    state.draft.joiningGap,
                    onJoiningGapChange,
                    stringResource(R.string.project_editor_joining_gap),
                    ProjectDraftError.JOINING_GAP in state.validationErrors,
                    allowZero = true,
                )
                ToggleRow(
                    label = stringResource(R.string.project_editor_tracking),
                    checked = state.draft.trackingEnabled,
                    onCheckedChange = onTrackingChange,
                )

                EditorHeading(stringResource(R.string.project_editor_palette))
                Text(
                    stringResource(R.string.project_editor_palette_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.colors.isEmpty()) {
                    Text(stringResource(R.string.project_editor_no_colors))
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small)) {
                        items(state.colors, key = ColorEntity::id) { color ->
                            FilterChip(
                                selected = color.id in state.draft.selectedColorIds,
                                onClick = { onToggleColor(color.id) },
                                leadingIcon = { ColorDot(color) },
                                label = { Text(color.name) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                }

                if (!isEditing) {
                    EditorHeading(stringResource(R.string.project_editor_initial_fill))
                    InitialProjectFill.entries.forEach { fill ->
                        val label =
                            when (fill) {
                                InitialProjectFill.BLANK -> R.string.project_editor_fill_blank
                                InitialProjectFill.FILL_ONE -> R.string.project_editor_fill_one
                                InitialProjectFill.BALANCED -> R.string.project_editor_fill_balanced
                            }
                        RadioRow(
                            label = stringResource(label),
                            selected = state.draft.initialFill == fill,
                            onClick = { onInitialFillChange(fill) },
                        )
                    }
                    if (state.draft.initialFill != InitialProjectFill.BLANK) {
                        Text(stringResource(R.string.project_editor_choose_designs), style = MaterialTheme.typography.titleMedium)
                        if (state.designs.isEmpty()) {
                            Text(stringResource(R.string.project_editor_no_designs))
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small)) {
                                items(state.designs, key = { it.design.id }) { relation ->
                                    FilterChip(
                                        selected = relation.design.id in state.draft.selectedDesignIds,
                                        onClick = { onToggleDesign(relation.design.id) },
                                        label = { Text(relation.design.name) },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    )
                                }
                            }
                        }
                        if (ProjectDraftError.DESIGNS in state.validationErrors) {
                            Text(stringResource(R.string.project_editor_design_error), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                EditorHeading(stringResource(R.string.project_editor_yarn))
                DecimalField(
                    state.draft.globalGramsPerSquare,
                    onGlobalGramsChange,
                    stringResource(R.string.project_editor_grams_per_square),
                    ProjectDraftError.GRAMS_PER_SQUARE in state.validationErrors,
                )
                DecimalField(
                    state.draft.skeinWeightGrams,
                    onSkeinWeightChange,
                    stringResource(R.string.project_editor_skein_weight),
                    ProjectDraftError.SKEIN_WEIGHT in state.validationErrors,
                )
                OutlinedTextField(
                    value = state.draft.bufferPercent,
                    onValueChange = onBufferChange,
                    label = { Text(stringResource(R.string.project_editor_buffer)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = ProjectDraftError.BUFFER_PERCENT in state.validationErrors,
                    supportingText =
                        errorText(
                            ProjectDraftError.BUFFER_PERCENT in state.validationErrors,
                            R.string.project_editor_buffer_error,
                        ),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.draft.notes,
                    onValueChange = onNotesChange,
                    label = { Text(stringResource(R.string.project_editor_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )

                state.notice?.let { notice ->
                    Text(
                        stringResource(
                            if (notice == ProjectEditorNotice.INVALID_DRAFT) {
                                R.string.project_editor_invalid
                            } else {
                                R.string.project_editor_save_failed
                            },
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium),
                ) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                        Text(stringResource(R.string.project_editor_cancel))
                    }
                    Button(
                        onClick = onSave,
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("project_save"),
                    ) {
                        Text(stringResource(if (state.isSaving) R.string.project_editor_saving else R.string.project_editor_save))
                    }
                }
                Spacer(Modifier.size(SquareToolSpacing.ExtraLarge))
            }
        }
    }

    state.pendingShrink?.let { impact ->
        AlertDialog(
            modifier = Modifier.testTag("project_shrink_confirmation"),
            onDismissRequest = onCancelShrink,
            title = { Text(stringResource(R.string.project_editor_shrink_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.project_editor_shrink_body,
                        impact.lostCellCount,
                        impact.lostAssignedCellCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmShrink, modifier = Modifier.testTag("project_confirm_shrink")) {
                    Text(stringResource(R.string.project_editor_shrink_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelShrink) { Text(stringResource(R.string.project_editor_cancel)) }
            },
        )
    }
}

@Composable
private fun EditorHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = isError,
        supportingText = errorText(isError, R.string.project_editor_dimension_error),
        singleLine = true,
    )
}

@Composable
private fun DecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    allowZero: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = isError,
        supportingText =
            errorText(
                isError,
                if (allowZero) R.string.project_editor_gap_error else R.string.project_editor_positive_error,
            ),
        singleLine = true,
    )
}

@Composable
private fun errorText(
    show: Boolean,
    resourceId: Int,
): (@Composable () -> Unit)? = if (show) ({ Text(stringResource(resourceId)) }) else null

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(SquareToolSpacing.Small))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ColorDot(color: ColorEntity) {
    Box(
        modifier =
            Modifier
                .size(24.dp)
                .semantics { contentDescription = color.name }
                .then(Modifier),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(color.argb.toInt()))
        }
    }
}

private fun createDraftPreview(state: ProjectEditorUiState): ProjectCardModel {
    val draft = state.draft
    val rows = draft.rows.coerceIn(1, 50)
    val columns = draft.columns.coerceIn(1, 50)
    val project =
        ProjectEntity(
            id = draft.id.ifBlank { "preview" },
            name = draft.name,
            rowCount = rows,
            columnCount = columns,
            squareWidthValue = draft.squareWidth.toDoubleOrNull(),
            squareHeightValue = draft.squareHeight.toDoubleOrNull(),
            measurementUnit = draft.measurementUnit.name.lowercase(),
            joiningGapValue = draft.joiningGap.toDoubleOrNull(),
            trackingEnabled = draft.trackingEnabled,
            favorite = false,
            notes = draft.notes,
            createdAt = 0,
            updatedAt = 0,
            lastOpenedAt = 0,
            generationSeed = 0,
            defaultSquareDesignId = null,
            globalGramsPerSquare = draft.globalGramsPerSquare.toDoubleOrNull(),
            skeinWeightGrams = draft.skeinWeightGrams.toDoubleOrNull(),
            joiningAndEdgingBufferPercent = draft.bufferPercent.toDoubleOrNull() ?: 0.0,
            demoProject = false,
        )
    val baseCells =
        state.basePreview
            ?.cells
            ?.associateBy { it.rowIndex to it.columnIndex }
            .orEmpty()
    val assignments = if (state.basePreview == null) draft.copy(rows = rows, columns = columns).initialAssignments() else emptyList()
    val cells =
        List(rows * columns) { index ->
            val row = index / columns
            val column = index % columns
            baseCells[row to column]?.copy(projectId = project.id)
                ?: ProjectCellEntity(
                    projectId = project.id,
                    rowIndex = row,
                    columnIndex = column,
                    squareDesignId = assignments.getOrNull(index),
                    locked = false,
                    completed = false,
                )
        }
    val selectedPalette = state.colors.filter { it.id in draft.selectedColorIds }
    return buildProjectCardModel(project, cells, state.designs, state.colors, selectedPalette)
}
