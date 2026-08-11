package com.finnvek.squaretool.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.finnvek.squaretool.render.MotifRenderConfig
import com.finnvek.squaretool.render.MotifRenderDetail
import com.finnvek.squaretool.render.MotifRenderer
import com.finnvek.squaretool.render.MotifSurface
import kotlin.math.min

object ProjectPlanRenderer {
    private val gridPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF5F604F.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
    private val blankPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF3EFE4.toInt()
            style = Paint.Style.FILL
        }
    private val codeBackgroundPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xD9FFFDF7.toInt()
            style = Paint.Style.FILL
        }
    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1C2114.toInt()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

    fun drawBlanket(
        canvas: Canvas,
        area: RectF,
        snapshot: ProjectExportSnapshot,
        section: GridSection = GridSection(0, 0, snapshot.project.rows, snapshot.project.columns),
        includeLabels: Boolean = false,
    ) {
        val labelGutter = if (includeLabels) min(area.width(), area.height()) * 0.045f else 0f
        val availableWidth = area.width() - labelGutter
        val availableHeight = area.height() - labelGutter
        val cellSize = min(availableWidth / section.columnCount, availableHeight / section.rowCount)
        val gridWidth = cellSize * section.columnCount
        val gridHeight = cellSize * section.rowCount
        val left = area.left + labelGutter + (availableWidth - gridWidth) / 2f
        val top = area.top + labelGutter + (availableHeight - gridHeight) / 2f
        val cellsByCoordinate = snapshot.cells.associateBy { it.row to it.column }
        val codesByDesign = snapshot.legendEntries().associate { it.design.id to it.code }

        repeat(section.rowCount) { localRow ->
            repeat(section.columnCount) { localColumn ->
                val row = section.startRow + localRow
                val column = section.startColumn + localColumn
                val cell = cellsByCoordinate[row to column]
                val rect =
                    RectF(
                        left + localColumn * cellSize,
                        top + localRow * cellSize,
                        left + (localColumn + 1) * cellSize,
                        top + (localRow + 1) * cellSize,
                    )
                canvas.drawRect(rect, blankPaint)
                val visual = cell?.let(snapshot::visualForCell)
                if (visual != null) {
                    MotifRenderer.draw(
                        canvas = canvas,
                        bounds = RectF(rect).apply { inset(cellSize * 0.035f, cellSize * 0.035f) },
                        visual = visual,
                        config =
                            MotifRenderConfig(
                                detail = MotifRenderDetail.FULL,
                                surface = MotifSurface.LIGHT,
                                locked = cell.locked,
                                completed = snapshot.project.trackingEnabled && cell.completed,
                            ),
                    )
                    val code = cell.designId?.let(codesByDesign::get)
                    if (code != null && cellSize >= 18f) drawCellCode(canvas, rect, code, cellSize)
                }
                gridPaint.strokeWidth = (cellSize * 0.015f).coerceIn(0.7f, 2.2f)
                canvas.drawRect(rect, gridPaint)
            }
        }

        if (includeLabels && cellSize >= 12f) {
            textPaint.textSize = (cellSize * 0.28f).coerceIn(7f, 16f)
            repeat(section.columnCount) { localColumn ->
                val column = section.startColumn + localColumn + 1
                canvas.drawText(column.toString(), left + (localColumn + 0.5f) * cellSize, top - cellSize * 0.16f, textPaint)
            }
            repeat(section.rowCount) { localRow ->
                val row = section.startRow + localRow + 1
                val baseline = top + (localRow + 0.5f) * cellSize - (textPaint.ascent() + textPaint.descent()) / 2f
                canvas.drawText(row.toString(), left - cellSize * 0.22f, baseline, textPaint)
            }
        }
    }

    private fun drawCellCode(
        canvas: Canvas,
        rect: RectF,
        code: String,
        cellSize: Float,
    ) {
        val size = cellSize * 0.27f
        val badge =
            RectF(
                rect.left + cellSize * 0.06f,
                rect.top + cellSize * 0.06f,
                rect.left + cellSize * 0.06f + size,
                rect.top + cellSize * 0.06f + size,
            )
        canvas.drawRoundRect(badge, size * 0.2f, size * 0.2f, codeBackgroundPaint)
        textPaint.textSize = size * 0.58f
        textPaint.color = Color.rgb(28, 33, 20)
        val baseline = badge.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(code, badge.centerX(), baseline, textPaint)
    }
}
