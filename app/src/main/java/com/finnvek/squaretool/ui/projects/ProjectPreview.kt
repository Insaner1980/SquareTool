package com.finnvek.squaretool.ui.projects

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.finnvek.squaretool.render.MotifRenderConfig
import com.finnvek.squaretool.render.MotifRenderer
import com.finnvek.squaretool.render.MotifSurface
import com.finnvek.squaretool.render.SquareDesignVisual
import kotlin.math.min

@Composable
fun ProjectBlanketPreview(
    project: ProjectCardModel,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.surfaceVariant
    val blankColor = MaterialTheme.colorScheme.surface.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val motifSurface =
        if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
            MotifSurface.DARK
        } else {
            MotifSurface.LIGHT
        }
    Canvas(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .background(background)
                .semantics { this.contentDescription = contentDescription }
                .drawWithCache {
                    val blankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = blankColor }
                    val gridPaint =
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = gridColor
                            style = Paint.Style.STROKE
                            strokeWidth = 1f
                        }
                    onDrawBehind {
                        val rows = project.project.rowCount.coerceAtLeast(1)
                        val columns = project.project.columnCount.coerceAtLeast(1)
                        val tileSide = min(size.width / columns, size.height / rows)
                        val gridWidth = tileSide * columns
                        val gridHeight = tileSide * rows
                        val startX = (size.width - gridWidth) / 2f
                        val startY = (size.height - gridHeight) / 2f
                        val cells = project.cells.associateBy { it.rowIndex to it.columnIndex }
                        val canvas = drawContext.canvas.nativeCanvas
                        repeat(rows) { row ->
                            repeat(columns) { column ->
                                val left = startX + column * tileSide
                                val top = startY + row * tileSide
                                val bounds = RectF(left, top, left + tileSide, top + tileSide)
                                val cell = cells[row to column]
                                val visual = cell?.squareDesignId?.let(project.designs::get)
                                val rendered =
                                    visual?.let { design ->
                                        design.roundColors.takeIf { it.isNotEmpty() }?.let { colors ->
                                            runCatching {
                                                MotifRenderer.draw(
                                                    canvas = canvas,
                                                    bounds = bounds,
                                                    visual = SquareDesignVisual(design.templateId, colors),
                                                    config =
                                                        MotifRenderConfig(
                                                            surface = motifSurface,
                                                            locked = cell.locked,
                                                            completed =
                                                                shouldRenderCompletedOverlay(
                                                                    trackingEnabled = project.project.trackingEnabled,
                                                                    cellCompleted = cell.completed,
                                                                ),
                                                        ),
                                                )
                                            }.isSuccess
                                        }
                                    } == true
                                if (!rendered) canvas.drawRect(bounds, blankPaint)
                                canvas.drawRect(bounds, gridPaint)
                            }
                        }
                    }
                },
    ) {}
}

internal fun shouldRenderCompletedOverlay(
    trackingEnabled: Boolean,
    cellCompleted: Boolean,
): Boolean = trackingEnabled && cellCompleted
