package com.finnvek.squaretool.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.finnvek.squaretool.R
import com.finnvek.squaretool.domain.algorithm.MeasurementCalculator
import com.finnvek.squaretool.domain.algorithm.ProgressCalculator
import com.finnvek.squaretool.domain.model.GridSize
import com.finnvek.squaretool.domain.model.MeasurementUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.DateFormat
import java.util.Date
import java.util.Locale

enum class ExportPaperSize { AUTO, A4, LETTER }

data class PdfExportOptions(
    val paperSize: ExportPaperSize = ExportPaperSize.AUTO,
    val includeLabels: Boolean = true,
    val includeLegend: Boolean = true,
    val exportedAtMillis: Long = System.currentTimeMillis(),
)

class ProjectPdfExporter(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun write(
        snapshot: ProjectExportSnapshot,
        output: OutputStream,
        options: PdfExportOptions = PdfExportOptions(),
    ) = withContext(ioDispatcher) {
        val document = PdfDocument()
        try {
            val pageSize = resolvePageSize(snapshot, options.paperSize)
            drawOverviewPage(document, pageSize, snapshot, options)
            val materialsPageCount = drawMaterialsPages(document, pageSize, snapshot, options)
            if (snapshot.project.rows > 16 || snapshot.project.columns > 12) {
                ExportPolicy.gridSections(snapshot.project.rows, snapshot.project.columns, 16, 12).forEachIndexed { index, section ->
                    drawSectionPage(document, pageSize, snapshot, section, index + materialsPageCount + 2)
                }
            }
            document.writeTo(output)
            output.flush()
        } finally {
            document.close()
        }
    }

    private fun drawOverviewPage(
        document: PdfDocument,
        pageSize: PageSize,
        snapshot: ProjectExportSnapshot,
        options: PdfExportOptions,
    ) {
        val page = document.startPage(PdfDocument.PageInfo.Builder(pageSize.width, pageSize.height, 1).create())
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)
        val margin = 36f
        val titlePaint = titlePaint(22f)
        val headingPaint = titlePaint(17f)
        val bodyPaint = bodyPaint(10.5f)
        var y = 50f
        canvas.drawText(context.getString(R.string.export_squaretool_title), margin, y, titlePaint)
        y += 28f
        canvas.drawText(snapshot.project.name, margin, y, headingPaint)
        y += 20f
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(options.exportedAtMillis))
        canvas.drawText(context.getString(R.string.export_date, date), margin, y, bodyPaint)
        y += 17f
        canvas.drawText(
            context.getString(
                R.string.export_grid_summary,
                snapshot.project.columns,
                snapshot.project.rows,
                snapshot.project.rows * snapshot.project.columns,
            ),
            margin,
            y,
            bodyPaint,
        )
        y += 17f
        dimensionsLine(snapshot)?.let { line ->
            canvas.drawText(line, margin, y, bodyPaint)
            y += 17f
        }
        if (snapshot.project.notes.isNotBlank()) {
            y = drawWrappedText(canvas, snapshot.project.notes, margin, y, pageSize.width - margin * 2, bodyPaint, 3)
        }
        val disclaimerY = pageSize.height - 28f
        val gridTop = y + 10f
        ProjectPlanRenderer.drawBlanket(
            canvas,
            RectF(margin, gridTop, pageSize.width - margin, disclaimerY - 18f),
            snapshot,
            includeLabels = options.includeLabels,
        )
        canvas.drawText(context.getString(R.string.export_disclaimer), margin, disclaimerY, bodyPaint(8.5f))
        document.finishPage(page)
    }

    @Suppress("kotlin:S3776") // Pagination and drawing form one ordered PDF operation.
    private fun drawMaterialsPages(
        document: PdfDocument,
        pageSize: PageSize,
        snapshot: ProjectExportSnapshot,
        options: PdfExportOptions,
    ): Int {
        val margin = 36f
        val materials = snapshot.materialsSummary()
        val rows =
            buildList {
                if (options.includeLegend && snapshot.legendEntries().isNotEmpty()) {
                    add(PdfMaterialRow(context.getString(R.string.export_design_legend), 25f, heading = true))
                    snapshot.legendEntries().forEach { entry ->
                        add(
                            PdfMaterialRow(
                                context.resources.getQuantityString(
                                    R.plurals.export_design_line,
                                    entry.count,
                                    entry.code,
                                    entry.design.name,
                                    entry.count,
                                ),
                                17f,
                            ),
                        )
                    }
                    add(PdfMaterialRow(null, 12f))
                }
                if (snapshot.colors.isNotEmpty()) {
                    add(PdfMaterialRow(context.getString(R.string.export_color_palette), 25f, heading = true))
                }
                snapshot.colors.forEach { color ->
                    val hex = String.format(Locale.ROOT, "#%08X", color.argb)
                    val percentage = materials.colorUsagePercentages[color.id] ?: 0.0
                    add(
                        PdfMaterialRow(
                            text =
                                context.getString(
                                    R.string.export_color_usage_line,
                                    color.name,
                                    hex,
                                    decimal(percentage),
                                ),
                            height = 17f,
                            swatchArgb = color.argb,
                            indent = 20f,
                        ),
                    )
                    val details =
                        buildList {
                            color.yarnBrand?.takeIf(String::isNotBlank)?.let(::add)
                            color.yarnLine?.takeIf(String::isNotBlank)?.let(::add)
                            color.shadeName?.takeIf(String::isNotBlank)?.let(::add)
                            color.shadeCode?.takeIf(String::isNotBlank)?.let(::add)
                            materials.yarnEstimate?.colorGrams?.get(color.id)?.let { grams ->
                                add(context.getString(R.string.export_color_yarn_grams, decimal(grams)))
                            }
                        }
                    if (details.isNotEmpty()) {
                        add(PdfMaterialRow(details.joinToString(" · "), 15f, small = true, indent = 20f))
                    }
                }
                add(PdfMaterialRow(null, 12f))
                add(PdfMaterialRow(context.getString(R.string.export_materials_summary), 25f, heading = true))
                progressLine(snapshot)?.let { add(PdfMaterialRow(it, 17f)) }
                yarnLine(materials)?.let { add(PdfMaterialRow(it, 17f)) }
                add(
                    PdfMaterialRow(
                        context.getString(
                            R.string.export_buffer,
                            decimal(snapshot.project.bufferPercent),
                        ),
                        17f,
                    ),
                )
            }

        val availableHeight = pageSize.height - 130f
        val pages = mutableListOf<MutableList<PdfMaterialRow>>()
        var current = mutableListOf<PdfMaterialRow>()
        var usedHeight = 0f
        rows.forEach { row ->
            if (current.isNotEmpty() && usedHeight + row.height > availableHeight) {
                pages += current
                current = mutableListOf()
                usedHeight = 0f
            }
            current += row
            usedHeight += row.height
        }
        if (current.isNotEmpty() || pages.isEmpty()) pages += current

        pages.forEachIndexed { index, pageRows ->
            val pageNumber = index + 2
            val page =
                document.startPage(
                    PdfDocument.PageInfo.Builder(pageSize.width, pageSize.height, pageNumber).create(),
                )
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            canvas.drawText(
                context.getString(R.string.export_materials_summary),
                margin,
                52f,
                titlePaint(19f),
            )
            var y = 82f
            pageRows.forEach { row ->
                val text = row.text
                if (text != null) {
                    row.swatchArgb?.let { argb ->
                        val swatch = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb }
                        canvas.drawRoundRect(
                            RectF(margin, y - 11f, margin + 13f, y + 2f),
                            2f,
                            2f,
                            swatch,
                        )
                    }
                    val paint =
                        when {
                            row.heading -> titlePaint(15f)
                            row.small -> bodyPaint(9f)
                            else -> bodyPaint(10.5f)
                        }
                    canvas.drawText(text, margin + row.indent, y, paint)
                }
                y += row.height
            }
            canvas.drawText(
                context.getString(R.string.export_disclaimer),
                margin,
                pageSize.height - 28f,
                bodyPaint(8.5f),
            )
            document.finishPage(page)
        }
        return pages.size
    }

    private fun drawSectionPage(
        document: PdfDocument,
        pageSize: PageSize,
        snapshot: ProjectExportSnapshot,
        section: GridSection,
        pageNumber: Int,
    ) {
        val page = document.startPage(PdfDocument.PageInfo.Builder(pageSize.width, pageSize.height, pageNumber).create())
        page.canvas.drawColor(Color.WHITE)
        val margin = 30f
        page.canvas.drawText(
            context.getString(
                R.string.export_section_title,
                section.startRow + 1,
                section.startRow + section.rowCount,
                section.startColumn + 1,
                section.startColumn + section.columnCount,
            ),
            margin,
            42f,
            titlePaint(16f),
        )
        ProjectPlanRenderer.drawBlanket(
            page.canvas,
            RectF(margin, 58f, pageSize.width - margin, pageSize.height - 34f),
            snapshot,
            section,
            includeLabels = true,
        )
        document.finishPage(page)
    }

    private fun dimensionsLine(snapshot: ProjectExportSnapshot): String? {
        val unit = parseExportMeasurementUnit(snapshot.project.measurementUnit) ?: return null
        val dimensions =
            MeasurementCalculator.blanketDimensions(
                GridSize(snapshot.project.rows, snapshot.project.columns),
                snapshot.project.squareWidth,
                snapshot.project.squareHeight,
                snapshot.project.joiningGap,
                unit,
            ) ?: return context.getString(R.string.export_missing_measurements)
        return context.getString(
            R.string.export_dimensions,
            decimal(dimensions.width),
            decimal(dimensions.height),
            context.getString(
                if (unit == MeasurementUnit.CENTIMETERS) {
                    R.string.export_unit_centimeters
                } else {
                    R.string.export_unit_inches
                },
            ),
        )
    }

    private fun progressLine(snapshot: ProjectExportSnapshot): String? {
        val progress =
            ProgressCalculator.calculate(
                snapshot.cells.count(ExportCell::completed),
                snapshot.project.rows * snapshot.project.columns,
                snapshot.project.trackingEnabled,
            )
                ?: return null
        return context.getString(R.string.export_progress, progress.completedCount, progress.totalCount, progress.percentage)
    }

    private fun yarnLine(materials: ExportMaterialsSummary): String? {
        val estimate = materials.yarnEstimate ?: return null
        return context.getString(R.string.export_yarn_equivalent, decimal(estimate.equivalentSkeins), estimate.recommendedWholeSkeins)
    }

    private fun resolvePageSize(
        snapshot: ProjectExportSnapshot,
        requested: ExportPaperSize,
    ): PageSize {
        val actual =
            if (requested == ExportPaperSize.AUTO) {
                if (Locale.getDefault().country in setOf("US", "CA")) ExportPaperSize.LETTER else ExportPaperSize.A4
            } else {
                requested
            }
        val portrait = if (actual == ExportPaperSize.LETTER) PageSize(612, 792) else PageSize(595, 842)
        return if (snapshot.project.columns > snapshot.project.rows * 1.15) PageSize(portrait.height, portrait.width) else portrait
    }

    private fun titlePaint(size: Float) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF263016.toInt()
            textSize = size
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }

    private fun bodyPaint(size: Float) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF303426.toInt()
            textSize = size
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

    private fun drawWrappedText(
        canvas: android.graphics.Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        maxLines: Int,
    ): Float {
        val words = text.split(Regex("\\s+")).filter(String::isNotBlank)
        var line = ""
        var y = startY
        var lines = 0
        words.forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                canvas.drawText(line, x, y, paint)
                y += paint.textSize * 1.35f
                lines++
                line = word
            } else {
                line = candidate
            }
            if (lines >= maxLines) return@forEach
        }
        if (line.isNotEmpty() && lines < maxLines) {
            canvas.drawText(line, x, y, paint)
            y += paint.textSize * 1.35f
        }
        return y
    }

    private fun decimal(value: Double): String = String.format(Locale.getDefault(), "%.1f", value)

    private data class PdfMaterialRow(
        val text: String?,
        val height: Float,
        val heading: Boolean = false,
        val small: Boolean = false,
        val swatchArgb: Int? = null,
        val indent: Float = 0f,
    )

    private data class PageSize(
        val width: Int,
        val height: Int,
    )
}

internal fun parseExportMeasurementUnit(value: String): MeasurementUnit? =
    MeasurementUnit.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
