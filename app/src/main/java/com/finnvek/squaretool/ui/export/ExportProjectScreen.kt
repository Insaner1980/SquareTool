package com.finnvek.squaretool.ui.export

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finnvek.squaretool.R
import com.finnvek.squaretool.app.AppContainer
import com.finnvek.squaretool.export.ExportPaperSize
import com.finnvek.squaretool.export.ExportPolicy
import com.finnvek.squaretool.export.ExportSnapshotFactory
import com.finnvek.squaretool.export.PdfExportOptions
import com.finnvek.squaretool.export.PngExportOptions
import com.finnvek.squaretool.export.ProjectExportSnapshot
import com.finnvek.squaretool.export.ProjectPdfExporter
import com.finnvek.squaretool.export.ProjectPngExporter
import com.finnvek.squaretool.export.ShareFileManager
import com.finnvek.squaretool.ui.theme.SquareToolSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun ExportProjectRoute(
    projectId: String,
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var snapshot by remember(projectId) { mutableStateOf<ProjectExportSnapshot?>(null) }
    var loading by remember(projectId) { mutableStateOf(true) }
    var loadFailed by remember(projectId) { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var paperSize by remember { mutableStateOf(ExportPaperSize.AUTO) }
    var includeLabels by remember { mutableStateOf(true) }
    var includeLegend by remember { mutableStateOf(true) }
    var transparent by remember { mutableStateOf(false) }
    val savedMessage = stringResource(R.string.export_success)
    val failureTemplate = stringResource(R.string.export_failure, "%s")
    val shareFailureTemplate = stringResource(R.string.share_failure, "%s")
    val sharePdfChooser = stringResource(R.string.share_pdf_chooser)
    val sharePngChooser = stringResource(R.string.share_png_chooser)
    val pdfExporter = remember(context) { ProjectPdfExporter(context) }
    val pngExporter = remember { ProjectPngExporter() }
    val shareFiles = remember(context) { ShareFileManager(context) }

    LaunchedEffect(projectId) {
        runCatching { withContext(Dispatchers.IO) { ExportSnapshotFactory.create(container.repository, projectId) } }
            .onSuccess { snapshot = it }
            .onFailure { loadFailed = true }
        loading = false
    }

    fun showFailure(
        message: String,
        share: Boolean = false,
    ) {
        scope.launch { snackbar.showSnackbar((if (share) shareFailureTemplate else failureTemplate).format(message)) }
    }

    val savePdf =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
            val current = snapshot
            if (uri != null && current != null) {
                scope.launch {
                    working = true
                    runCatching {
                        requireNotNull(context.contentResolver.openOutputStream(uri)).use { output ->
                            pdfExporter.write(
                                current,
                                output,
                                PdfExportOptions(
                                    paperSize = paperSize,
                                    includeLabels = includeLabels,
                                    includeLegend = includeLegend,
                                ),
                            )
                        }
                    }.onSuccess { snackbar.showSnackbar(savedMessage) }
                        .onFailure { showFailure(it.message.orEmpty()) }
                    working = false
                }
            }
        }
    val savePng =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
            val current = snapshot
            if (uri != null && current != null) {
                scope.launch {
                    working = true
                    runCatching {
                        requireNotNull(context.contentResolver.openOutputStream(uri)).use { output ->
                            pngExporter.write(
                                current,
                                output,
                                PngExportOptions(
                                    includeLegend = includeLegend,
                                    transparentBackground = transparent,
                                    includeLabels = includeLabels,
                                ),
                            )
                        }
                    }.onSuccess { snackbar.showSnackbar(savedMessage) }
                        .onFailure { showFailure(it.message.orEmpty()) }
                    working = false
                }
            }
        }
    val saveBackup =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                scope.launch {
                    working = true
                    runCatching {
                        val json =
                            withContext(Dispatchers.IO) {
                                container.backupService.createProjectJson(projectId)
                            }
                        withContext(Dispatchers.IO) {
                            requireNotNull(context.contentResolver.openOutputStream(uri)).bufferedWriter().use { it.write(json) }
                        }
                    }.onSuccess { snackbar.showSnackbar(savedMessage) }
                        .onFailure { showFailure(it.message.orEmpty()) }
                    working = false
                }
            }
        }

    fun sharePdf() {
        val current = snapshot ?: return
        scope.launch {
            working = true
            runCatching {
                val file = shareFiles.createFile(current.project.name, "plan", "pdf")
                file.outputStream().use { output ->
                    pdfExporter.write(
                        current,
                        output,
                        PdfExportOptions(
                            paperSize = paperSize,
                            includeLabels = includeLabels,
                            includeLegend = includeLegend,
                        ),
                    )
                }
                context.startActivity(
                    shareFiles.shareIntent(file, "application/pdf", sharePdfChooser),
                )
            }.onFailure { showFailure(it.message.orEmpty(), share = true) }
            working = false
        }
    }

    fun sharePng() {
        val current = snapshot ?: return
        scope.launch {
            working = true
            runCatching {
                val file = shareFiles.createFile(current.project.name, "blanket", "png")
                file.outputStream().use {
                    pngExporter.write(
                        current,
                        it,
                        PngExportOptions(includeLegend = includeLegend, transparentBackground = transparent, includeLabels = includeLabels),
                    )
                }
                context.startActivity(
                    shareFiles.shareIntent(file, "image/png", sharePngChooser),
                )
            }.onFailure { showFailure(it.message.orEmpty(), share = true) }
            working = false
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ExportTopBar(onBack)
        },
    ) { padding ->
        when {
            loading -> {
                Column(
                    Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.export_loading), modifier = Modifier.padding(top = SquareToolSpacing.Standard))
                }
            }

            loadFailed || snapshot == null -> {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(SquareToolSpacing.Standard),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.export_project_missing))
                }
            }

            else -> {
                ExportProjectScreen(
                    projectName = requireNotNull(snapshot).project.name,
                    isWorking = working,
                    paperSize = paperSize,
                    includeLabels = includeLabels,
                    includeLegend = includeLegend,
                    transparentBackground = transparent,
                    onPaperSizeChange = { paperSize = it },
                    onIncludeLabelsChange = { includeLabels = it },
                    onIncludeLegendChange = { includeLegend = it },
                    onTransparentBackgroundChange = { transparent = it },
                    onSavePdf = {
                        val current = requireNotNull(snapshot)
                        savePdf.launch(fileName(current.project.name, "plan", "pdf"))
                    },
                    onSavePng = {
                        val current = requireNotNull(snapshot)
                        savePng.launch(fileName(current.project.name, "blanket", "png"))
                    },
                    onSharePdf = ::sharePdf,
                    onSharePng = ::sharePng,
                    onExportBackup = {
                        val current = requireNotNull(snapshot)
                        saveBackup.launch(fileName(current.project.name, "backup", "json"))
                    },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.export_project_title)) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) } },
    )
}

@Composable
fun ExportProjectScreen(
    projectName: String,
    onSavePdf: () -> Unit,
    onSavePng: () -> Unit,
    onSharePdf: () -> Unit,
    onSharePng: () -> Unit,
    onExportBackup: () -> Unit,
    modifier: Modifier = Modifier,
    isWorking: Boolean = false,
    paperSize: ExportPaperSize = ExportPaperSize.AUTO,
    includeLabels: Boolean = true,
    includeLegend: Boolean = true,
    transparentBackground: Boolean = false,
    onPaperSizeChange: (ExportPaperSize) -> Unit = {},
    onIncludeLabelsChange: (Boolean) -> Unit = {},
    onIncludeLegendChange: (Boolean) -> Unit = {},
    onTransparentBackgroundChange: (Boolean) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(SquareToolSpacing.Standard),
        verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium),
    ) {
        Text(projectName, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Text(
            stringResource(R.string.export_project_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(
                R.string.export_options,
            ),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .padding(
                        top = SquareToolSpacing.Small,
                    ).semantics {
                        heading()
                    },
        )
        Text(stringResource(R.string.paper_size), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small)) {
            ExportPaperSize.entries.forEach { option ->
                FilterChip(
                    selected = paperSize == option,
                    onClick = { onPaperSizeChange(option) },
                    label = {
                        Text(
                            stringResource(
                                when (option) {
                                    ExportPaperSize.AUTO -> R.string.paper_auto
                                    ExportPaperSize.A4 -> R.string.paper_a4
                                    ExportPaperSize.LETTER -> R.string.paper_letter
                                },
                            ),
                        )
                    },
                )
            }
        }
        ExportSwitch(R.string.include_grid_labels, includeLabels, onIncludeLabelsChange)
        ExportSwitch(R.string.include_legend, includeLegend, onIncludeLegendChange)
        ExportSwitch(R.string.transparent_background, transparentBackground, onTransparentBackgroundChange)
        Text(
            stringResource(R.string.export_local_privacy),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ExportAction(R.string.export_pdf_plan, Icons.Outlined.PictureAsPdf, "save_pdf", isWorking, onSavePdf)
        ExportAction(R.string.export_png_plan, Icons.Outlined.Image, "save_png", isWorking, onSavePng)
        ExportAction(R.string.share_pdf, Icons.Outlined.Share, "share_pdf", isWorking, onSharePdf, outlined = true)
        ExportAction(R.string.share_image, Icons.Outlined.Share, "share_png", isWorking, onSharePng, outlined = true)
        ExportAction(R.string.export_backup, Icons.Outlined.Backup, "export_backup", isWorking, onExportBackup, outlined = true)
    }
}

@Composable
private fun ExportSwitch(
    labelRes: Int,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(labelRes)) },
        trailingContent = { Switch(checked, onCheckedChange = onChange) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ExportAction(
    labelRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    disabled: Boolean,
    onClick: () -> Unit,
    outlined: Boolean = false,
) {
    val content: @Composable () -> Unit = {
        Icon(icon, contentDescription = null)
        Text(stringResource(labelRes), modifier = Modifier.padding(start = SquareToolSpacing.Small))
    }
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            enabled = !disabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(tag),
        ) { content() }
    } else {
        Button(onClick = onClick, enabled = !disabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(tag)) { content() }
    }
}

private fun fileName(
    projectName: String,
    purpose: String,
    extension: String,
): String = "${ExportPolicy.sanitizedBaseName(projectName)}_${purpose}_${LocalDate.now()}.$extension"
