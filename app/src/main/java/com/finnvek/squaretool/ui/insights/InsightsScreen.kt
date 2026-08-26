package com.finnvek.squaretool.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finnvek.squaretool.R
import com.finnvek.squaretool.data.repository.SquareToolRepository
import com.finnvek.squaretool.domain.model.MeasurementUnit
import com.finnvek.squaretool.ui.projects.ProjectBlanketPreview
import com.finnvek.squaretool.ui.theme.SquareToolSpacing
import java.text.NumberFormat
import kotlin.math.roundToInt

// CPD-OFF
@Suppress("kotlin:S107") // Route callbacks keep navigation and project actions independently typed.
@Composable
fun InsightsRoute(
    repository: SquareToolRepository,
    projectId: String,
    modifier: Modifier = Modifier,
    insightsViewModel: InsightsViewModel =
        viewModel(
            key = "insights-$projectId",
            factory = InsightsViewModel.factory(repository, projectId),
        ),
    onBack: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onSaveImage: () -> Unit = {},
    onSharePdf: () -> Unit = {},
    onShareImage: () -> Unit = {},
    onExportBackup: () -> Unit = {},
) {
    // CPD-ON
    val state by insightsViewModel.uiState.collectAsStateWithLifecycle()
    InsightsScreen(
        state = state,
        onBack = onBack,
        onExportPdf = onExportPdf,
        onSaveImage = onSaveImage,
        onSharePdf = onSharePdf,
        onShareImage = onShareImage,
        onExportBackup = onExportBackup,
        modifier = modifier,
    )
}

@Suppress("kotlin:S107") // Explicit insights actions are clearer than a generic event dispatcher.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    state: InsightsUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onSaveImage: () -> Unit = {},
    onSharePdf: () -> Unit = {},
    onShareImage: () -> Unit = {},
    onExportBackup: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.testTag("insights_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.insights_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.insights_back))
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
            val model = state.model
            if (model == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.insights_project_missing), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
                    val wide = maxWidth >= 720.dp
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(SquareToolSpacing.Standard),
                        verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Standard),
                    ) {
                        InsightsHero(model, wide)
                        if (wide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Standard),
                                verticalAlignment = Alignment.Top,
                            ) {
                                DistributionSection(model, Modifier.weight(1f))
                                ColorUsageSection(model, Modifier.weight(1f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Standard),
                                verticalAlignment = Alignment.Top,
                            ) {
                                DimensionsSection(model, Modifier.weight(1f))
                                YarnSection(model, Modifier.weight(1f))
                            }
                        } else {
                            DistributionSection(model)
                            ColorUsageSection(model)
                            DimensionsSection(model)
                            YarnSection(model)
                        }
                        model.progress?.let { ProgressSection(it) }
                        ExportSection(
                            onExportPdf = onExportPdf,
                            onSaveImage = onSaveImage,
                            onSharePdf = onSharePdf,
                            onShareImage = onShareImage,
                            onExportBackup = onExportBackup,
                        )
                        Spacer(Modifier.size(SquareToolSpacing.ExtraLarge))
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightsHero(
    model: InsightsModel,
    wide: Boolean,
) {
    Card(Modifier.fillMaxWidth()) {
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(SquareToolSpacing.Standard),
                horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Section),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProjectBlanketPreview(
                    project = model.preview,
                    contentDescription = model.project.name,
                    modifier = Modifier.size(220.dp),
                )
                HeroText(model, Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(SquareToolSpacing.Standard),
                verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Standard),
            ) {
                ProjectBlanketPreview(
                    project = model.preview,
                    contentDescription = model.project.name,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.35f),
                )
                HeroText(model)
            }
        }
    }
}

@Composable
private fun HeroText(
    model: InsightsModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium)) {
        Text(
            model.project.name,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small)) {
            InsightStat(model.totalSquares.toString(), stringResource(R.string.insights_total_squares), Modifier.weight(1f))
            InsightStat(model.designCount.toString(), stringResource(R.string.insights_designs), Modifier.weight(1f))
            InsightStat(model.colorCount.toString(), stringResource(R.string.insights_colors), Modifier.weight(1f))
        }
        if (model.progress != null || model.yarnEstimate != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small)) {
                model.progress?.let {
                    InsightStat(
                        stringResource(R.string.insights_percent_complete, it.percentage),
                        stringResource(R.string.insights_completion),
                        Modifier.weight(1f),
                    )
                }
                model.yarnEstimate?.let {
                    InsightStat(
                        formatNumber(it.equivalentSkeins),
                        stringResource(R.string.insights_estimated_skeins),
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DistributionSection(
    model: InsightsModel,
    modifier: Modifier = Modifier,
) {
    SectionCard(stringResource(R.string.insights_distribution), modifier) {
        if (model.distribution.isEmpty()) {
            Text(stringResource(R.string.insights_no_distribution), style = MaterialTheme.typography.bodyLarge)
        } else {
            val semanticsBuilder = StringBuilder()
            for (item in model.distribution) {
                val count = pluralStringResource(R.plurals.insights_square_count, item.count, item.count)
                semanticsBuilder.append(
                    pluralStringResource(
                        R.plurals.insights_chart_item_description,
                        item.percentage.roundToInt(),
                        item.name,
                        count,
                        item.percentage.roundToInt(),
                    ),
                )
                if (item != model.distribution.last()) semanticsBuilder.append(' ')
            }
            val description = stringResource(R.string.insights_chart_description, semanticsBuilder.toString())
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 280.dp)
                        .aspectRatio(1f)
                        .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center,
            ) {
                val centerColor = MaterialTheme.colorScheme.surface
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .testTag("insights_distribution_chart")
                        .semantics { contentDescription = description },
                ) {
                    var startAngle = -90f
                    model.distribution.forEach { item ->
                        val sweep = (item.percentage * 3.6).toFloat()
                        drawArc(
                            color = Color(item.colorArgb),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true,
                        )
                        startAngle += sweep
                    }
                    drawCircle(centerColor, radius = size.minDimension * 0.23f)
                }
                Text(
                    model.distribution.sumOf { it.count }.let { assigned ->
                        pluralStringResource(
                            R.plurals.insights_assigned_total,
                            assigned,
                            assigned,
                        )
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            model.distribution.forEach { item ->
                LegendRow(
                    color = Color(item.colorArgb),
                    name = item.name,
                    value =
                        stringResource(
                            R.string.insights_distribution_row,
                            item.count,
                            item.percentage.roundToInt(),
                        ),
                )
            }
        }
    }
}

@Composable
private fun ColorUsageSection(
    model: InsightsModel,
    modifier: Modifier = Modifier,
) {
    SectionCard(stringResource(R.string.insights_color_usage), modifier) {
        if (model.colorUsage.isEmpty()) {
            Text(stringResource(R.string.insights_no_color_usage), style = MaterialTheme.typography.bodyLarge)
        } else {
            val directItems = if (model.colorUsage.size > 7) model.colorUsage.take(6) else model.colorUsage
            directItems.forEach { ColorUsageRow(it) }
            if (model.colorUsage.size > 7) {
                val other = model.colorUsage.drop(6)
                val percentage = other.sumOf(ColorUsageItem::percentage)
                val grams = other.mapNotNull(ColorUsageItem::grams).takeIf { it.isNotEmpty() }?.sum()
                val skeins = other.mapNotNull(ColorUsageItem::equivalentSkeins).takeIf { it.isNotEmpty() }?.sum()
                OtherColorUsageRow(percentage, grams, skeins)
            }
        }
    }
}

@Composable
private fun ColorUsageRow(item: ColorUsageItem) {
    Column(verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.ExtraSmall)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color(item.color.argb.toInt()))
                    .semantics { contentDescription = item.color.name },
            )
            Spacer(Modifier.width(SquareToolSpacing.Small))
            Text(item.color.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.insights_color_percentage, item.percentage))
        }
        LinearProgressIndicator(
            progress = { (item.percentage / 100.0).toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = Color(item.color.argb.toInt()),
        )
        item.grams?.let { grams ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(stringResource(R.string.insights_color_grams, grams), style = MaterialTheme.typography.bodyMedium)
                item.equivalentSkeins?.let { skeins ->
                    Spacer(Modifier.width(SquareToolSpacing.Small))
                    Text(stringResource(R.string.insights_color_skeins, skeins), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun OtherColorUsageRow(
    percentage: Double,
    grams: Double?,
    skeins: Double?,
) {
    val color = MaterialTheme.colorScheme.secondary
    val otherLabel = stringResource(R.string.insights_other)
    Column(verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.ExtraSmall)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(color)
                    .semantics { contentDescription = otherLabel },
            )
            Spacer(Modifier.width(SquareToolSpacing.Small))
            Text(otherLabel, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.insights_color_percentage, percentage))
        }
        LinearProgressIndicator(
            progress = { (percentage / 100.0).toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = color,
        )
        if (grams != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(stringResource(R.string.insights_color_grams, grams), style = MaterialTheme.typography.bodyMedium)
                if (skeins != null) {
                    Spacer(Modifier.width(SquareToolSpacing.Small))
                    Text(stringResource(R.string.insights_color_skeins, skeins), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun LegendRow(
    color: Color,
    name: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(SquareToolSpacing.Small))
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DimensionsSection(
    model: InsightsModel,
    modifier: Modifier = Modifier,
) {
    SectionCard(stringResource(R.string.insights_dimensions), modifier) {
        Text(
            stringResource(R.string.insights_grid_size, model.project.columnCount, model.project.rowCount),
            style = MaterialTheme.typography.titleMedium,
        )
        val dimensions = model.dimensions
        if (dimensions == null) {
            Text(stringResource(R.string.insights_dimensions_missing), style = MaterialTheme.typography.bodyLarge)
        } else {
            val unit =
                stringResource(
                    if (dimensions.unit == MeasurementUnit.INCHES) R.string.insights_unit_in else R.string.insights_unit_cm,
                )
            Text(
                stringResource(
                    R.string.insights_finished_size,
                    formatNumber(dimensions.width),
                    formatNumber(dimensions.height),
                    unit,
                ),
                style = MaterialTheme.typography.headlineMedium,
            )
            model.project.joiningGapValue?.let { gap ->
                Text(stringResource(R.string.insights_joining_gap, formatNumber(gap), unit))
            }
        }
    }
}

@Composable
private fun YarnSection(
    model: InsightsModel,
    modifier: Modifier = Modifier,
) {
    SectionCard(stringResource(R.string.insights_yarn_estimate), modifier) {
        val estimate = model.yarnEstimate
        if (estimate == null) {
            Text(stringResource(R.string.insights_yarn_missing), style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(
                stringResource(R.string.insights_equivalent_skeins, formatNumber(estimate.equivalentSkeins)),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                pluralStringResource(
                    R.plurals.insights_recommended_skeins,
                    estimate.recommendedWholeSkeins,
                    estimate.recommendedWholeSkeins,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(stringResource(R.string.insights_buffer, formatNumber(model.project.joiningAndEdgingBufferPercent)))
            model.project.skeinWeightGrams?.let {
                Text(stringResource(R.string.insights_skein_weight, formatNumber(it)))
            }
            Text(
                stringResource(R.string.insights_yarn_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProgressSection(progress: com.finnvek.squaretool.domain.model.ProjectProgress) {
    SectionCard(stringResource(R.string.insights_progress)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress.percentage / 100f },
                    modifier = Modifier.fillMaxSize(),
                )
                Text(stringResource(R.string.insights_percent_complete, progress.percentage))
            }
            Spacer(Modifier.width(SquareToolSpacing.Section))
            Column(verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Small)) {
                Text(
                    stringResource(R.string.insights_completed_count, progress.completedCount),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.insights_remaining_count, progress.remainingCount),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun ExportSection(
    onExportPdf: () -> Unit,
    onSaveImage: () -> Unit,
    onSharePdf: () -> Unit,
    onShareImage: () -> Unit,
    onExportBackup: () -> Unit,
) {
    SectionCard(stringResource(R.string.insights_export_share)) {
        Text(
            stringResource(R.string.insights_export_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ExportButton(R.string.insights_export_pdf, Icons.Default.Description, "insights_export_pdf", onExportPdf)
        ExportButton(R.string.insights_save_image, Icons.Default.Image, "insights_save_image", onSaveImage)
        ExportButton(R.string.insights_share_pdf, Icons.Default.Share, "insights_share_pdf", onSharePdf)
        ExportButton(R.string.insights_share_image, Icons.Default.Share, "insights_share_image", onShareImage)
        ExportButton(R.string.insights_export_backup, Icons.Default.Backup, "insights_export_backup", onExportBackup)
    }
}

@Composable
private fun ExportButton(
    labelRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(tag),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(SquareToolSpacing.Small))
        Text(stringResource(labelRes), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(SquareToolSpacing.Standard),
            verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Medium),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

@Composable
private fun formatNumber(value: Double): String {
    val formatter =
        remember {
            NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 }
        }
    return formatter.format(value)
}
