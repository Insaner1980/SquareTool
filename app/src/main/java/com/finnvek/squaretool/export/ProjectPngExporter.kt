package com.finnvek.squaretool.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import kotlin.math.ceil

data class PngExportOptions(
    val requestedLongEdge: Int = 2_048,
    val includeLegend: Boolean = true,
    val transparentBackground: Boolean = false,
    val includeLabels: Boolean = false,
)

class ProjectPngExporter(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun write(
        snapshot: ProjectExportSnapshot,
        output: OutputStream,
        options: PngExportOptions = PngExportOptions(),
    ) = withContext(ioDispatcher) {
        val blanketSize =
            ExportPolicy.bitmapSize(
                rows = snapshot.project.rows,
                columns = snapshot.project.columns,
                requestedLongEdge = options.requestedLongEdge.coerceIn(512, 8_192),
            )
        val legendEntries = if (options.includeLegend) snapshot.legendEntries() else emptyList()
        val legendColumns = if (blanketSize.width >= 1_000) 2 else 1
        val legendTextSize = (blanketSize.width * 0.025f).coerceIn(18f, 42f)
        val legendRows = ceil(legendEntries.size / legendColumns.toDouble()).toInt()
        val showLegend = options.includeLegend && legendEntries.isNotEmpty()
        val legendHeight =
            if (showLegend) {
                ceil(legendTextSize * 1.45f * (legendRows + 1)).toInt().coerceAtLeast(240)
            } else {
                0
            }
        val initialPixels = blanketSize.width.toLong() * (blanketSize.height + legendHeight)
        val scale =
            if (initialPixels > ExportPolicy.MAX_EXPORT_PIXELS) {
                kotlin.math.sqrt(ExportPolicy.MAX_EXPORT_PIXELS.toDouble() / initialPixels)
            } else {
                1.0
            }
        val width = (blanketSize.width * scale).toInt().coerceAtLeast(1)
        val blanketHeight = (blanketSize.height * scale).toInt().coerceAtLeast(1)
        val finalLegendHeight = (legendHeight * scale).toInt()
        val bitmap = createBitmap(width, blanketHeight + finalLegendHeight, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(if (options.transparentBackground) Color.TRANSPARENT else 0xFFFFFDF7.toInt())
            val margin = width * 0.035f
            ProjectPlanRenderer.drawBlanket(
                canvas,
                RectF(margin, margin, width - margin, blanketHeight - margin),
                snapshot,
                includeLabels = options.includeLabels,
            )
            if (showLegend) {
                drawLegend(
                    canvas = canvas,
                    entries = legendEntries,
                    top = blanketHeight.toFloat(),
                    width = width.toFloat(),
                    height = finalLegendHeight.toFloat(),
                    columns = legendColumns,
                )
            }
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "PNG compression failed" }
            output.flush()
        } finally {
            bitmap.recycle()
        }
    }

    private fun drawLegend(
        canvas: Canvas,
        entries: List<ExportLegendEntry>,
        top: Float,
        width: Float,
        height: Float,
        columns: Int,
    ) {
        val rowsPerColumn = ceil(entries.size / columns.toDouble()).toInt().coerceAtLeast(1)
        val lineHeight =
            minOf(
                (width * 0.025f).coerceIn(18f, 42f) * 1.45f,
                height / (rowsPerColumn + 1),
            )
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF1C2114.toInt()
                textSize = lineHeight / 1.45f
            }
        val columnWidth = width / columns
        entries.forEachIndexed { index, entry ->
            val column = index / rowsPerColumn
            val row = index % rowsPerColumn
            val x = columnWidth * column + width * 0.05f
            val y = top + lineHeight * (row + 1)
            canvas.drawText("${entry.code}  ${entry.design.name}  ·  ${entry.count}", x, y, paint)
        }
    }
}
