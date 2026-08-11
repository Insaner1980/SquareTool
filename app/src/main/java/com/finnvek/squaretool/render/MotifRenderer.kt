package com.finnvek.squaretool.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.min

object MotifRenderer {
    private const val DEFAULT_SMALL_PREVIEW_THRESHOLD_PX = 24f

    fun createPlan(
        visual: SquareDesignVisual,
        config: MotifRenderConfig = MotifRenderConfig(),
    ): MotifRenderPlan = MotifGeometryPlanner.createPlan(visual, config)

    /** Draws the same normalized plan to screen, Bitmap, PDF, or print canvases. */
    fun draw(
        canvas: Canvas,
        bounds: RectF,
        visual: SquareDesignVisual,
        config: MotifRenderConfig = MotifRenderConfig(),
        smallPreviewThresholdPx: Float = DEFAULT_SMALL_PREVIEW_THRESHOLD_PX,
    ): MotifRenderPlan {
        require(bounds.width() > 0f && bounds.height() > 0f) { "Motif bounds must be non-empty" }
        val resolvedConfig =
            if (config.detail == MotifRenderDetail.AUTO) {
                config.copy(
                    detail =
                        if (min(bounds.width(), bounds.height()) < smallPreviewThresholdPx) {
                            MotifRenderDetail.SMALL
                        } else {
                            MotifRenderDetail.FULL
                        },
                )
            } else {
                config
            }
        val plan = createPlan(visual, resolvedConfig)
        drawPlan(canvas, bounds, plan)
        return plan
    }

    fun drawPlan(
        canvas: Canvas,
        bounds: RectF,
        plan: MotifRenderPlan,
    ) {
        require(bounds.width() > 0f && bounds.height() > 0f) { "Motif bounds must be non-empty" }
        val side = min(bounds.width(), bounds.height())
        val square =
            RectF(
                bounds.centerX() - side / 2f,
                bounds.centerY() - side / 2f,
                bounds.centerX() + side / 2f,
                bounds.centerY() + side / 2f,
            )
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
        val saveCount = canvas.save()
        try {
            canvas.clipRect(square)
            plan.primitives.forEach { primitive ->
                drawPrimitive(
                    canvas = canvas,
                    square = square,
                    primitive = primitive,
                    fillArgb = plan.roundColors[primitive.roundIndex],
                    outlineArgb = plan.outlineArgb,
                    paint = paint,
                )
            }
            plan.overlays.forEach { overlay ->
                drawOverlay(canvas, square, overlay, plan.surface, paint)
            }
        } finally {
            canvas.restoreToCount(saveCount)
        }
    }

    private fun drawPrimitive(
        canvas: Canvas,
        square: RectF,
        primitive: MotifPrimitive,
        fillArgb: Int,
        outlineArgb: Int,
        paint: Paint,
    ) {
        when (primitive) {
            is MotifPrimitive.RoundedSquare -> {
                val rect = insetSquare(square, primitive.inset)
                val radius = primitive.cornerRadius * square.width()
                drawFilled(
                    paint,
                    fillArgb,
                    outlineArgb,
                    primitive.outlineWidth * square.width(),
                ) { shapePaint ->
                    canvas.drawRoundRect(rect, radius, radius, shapePaint)
                }
            }

            is MotifPrimitive.Circle -> {
                drawFilled(
                    paint,
                    fillArgb,
                    outlineArgb,
                    primitive.outlineWidth * square.width(),
                ) { shapePaint ->
                    canvas.drawCircle(
                        square.left + primitive.centerX * square.width(),
                        square.top + primitive.centerY * square.height(),
                        primitive.radius * square.width(),
                        shapePaint,
                    )
                }
            }

            is MotifPrimitive.Petal -> {
                val centerX = square.left + primitive.centerX * square.width()
                val centerY = square.top + primitive.centerY * square.height()
                val width = primitive.width * square.width()
                val height = primitive.height * square.height()
                val rect =
                    RectF(
                        centerX - width / 2f,
                        centerY - height / 2f,
                        centerX + width / 2f,
                        centerY + height / 2f,
                    )
                val radius = min(width, height) * 0.46f
                val saveCount = canvas.save()
                try {
                    canvas.rotate(primitive.rotationDegrees, centerX, centerY)
                    drawFilled(
                        paint,
                        fillArgb,
                        outlineArgb,
                        primitive.outlineWidth * square.width(),
                    ) { shapePaint ->
                        canvas.drawRoundRect(rect, radius, radius, shapePaint)
                    }
                } finally {
                    canvas.restoreToCount(saveCount)
                }
            }

            is MotifPrimitive.Diamond -> {
                val centerX = square.left + primitive.centerX * square.width()
                val centerY = square.top + primitive.centerY * square.height()
                val halfExtent = primitive.halfExtent * square.width()
                val rect =
                    RectF(
                        centerX - halfExtent,
                        centerY - halfExtent,
                        centerX + halfExtent,
                        centerY + halfExtent,
                    )
                val saveCount = canvas.save()
                try {
                    canvas.rotate(primitive.rotationDegrees, centerX, centerY)
                    drawFilled(
                        paint,
                        fillArgb,
                        outlineArgb,
                        primitive.outlineWidth * square.width(),
                    ) { shapePaint ->
                        val radius = primitive.cornerRadius * square.width()
                        canvas.drawRoundRect(rect, radius, radius, shapePaint)
                    }
                } finally {
                    canvas.restoreToCount(saveCount)
                }
            }

            is MotifPrimitive.Arc -> {
                val centerX = square.left + primitive.centerX * square.width()
                val centerY = square.top + primitive.centerY * square.height()
                val radius = primitive.radius * square.width()
                val rect = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
                paint.style = Paint.Style.STROKE
                paint.color = outlineArgb
                paint.strokeWidth = (primitive.strokeWidth + primitive.outlineWidth * 2f) * square.width()
                canvas.drawArc(rect, primitive.startAngleDegrees, primitive.sweepAngleDegrees, false, paint)
                paint.color = fillArgb
                paint.strokeWidth = primitive.strokeWidth * square.width()
                canvas.drawArc(rect, primitive.startAngleDegrees, primitive.sweepAngleDegrees, false, paint)
            }

            is MotifPrimitive.Polygon -> {
                val path = Path()
                primitive.points.forEachIndexed { index, point ->
                    val x = square.left + point.x * square.width()
                    val y = square.top + point.y * square.height()
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawFilled(
                    paint,
                    fillArgb,
                    outlineArgb,
                    primitive.outlineWidth * square.width(),
                ) { shapePaint ->
                    canvas.drawPath(path, shapePaint)
                }
            }
        }
    }

    private inline fun drawFilled(
        paint: Paint,
        fillArgb: Int,
        outlineArgb: Int,
        outlineWidth: Float,
        drawShape: (Paint) -> Unit,
    ) {
        paint.style = Paint.Style.FILL
        paint.color = fillArgb
        drawShape(paint)
        if (outlineWidth > 0f) {
            paint.style = Paint.Style.STROKE
            paint.color = outlineArgb
            paint.strokeWidth = outlineWidth
            drawShape(paint)
        }
    }

    private fun drawOverlay(
        canvas: Canvas,
        square: RectF,
        overlay: MotifOverlay,
        surface: MotifSurface,
        paint: Paint,
    ) {
        when (overlay) {
            is MotifOverlay.CompletionWash -> {
                paint.style = Paint.Style.FILL
                paint.color = if (surface == MotifSurface.LIGHT) Color.WHITE else Color.BLACK
                paint.alpha = (overlay.alpha * 255f).toInt()
                canvas.drawRoundRect(square, square.width() * 0.12f, square.width() * 0.12f, paint)
                paint.alpha = 255
            }

            MotifOverlay.CompletedCheck -> {
                drawCompletedCheck(canvas, square, paint)
            }

            MotifOverlay.LockedBadge -> {
                drawLockedBadge(canvas, square, paint)
            }

            is MotifOverlay.SelectionBorder -> {
                val halfStroke = overlay.width * square.width() / 2f
                val border = RectF(square).apply { inset(halfStroke, halfStroke) }
                paint.style = Paint.Style.STROKE
                paint.color = 0xFFD75A1F.toInt()
                paint.strokeWidth = overlay.width * square.width()
                canvas.drawRoundRect(border, square.width() * 0.12f, square.width() * 0.12f, paint)
            }
        }
    }

    private fun drawCompletedCheck(
        canvas: Canvas,
        square: RectF,
        paint: Paint,
    ) {
        val centerX = square.left + square.width() * 0.79f
        val centerY = square.top + square.height() * 0.79f
        val radius = square.width() * 0.13f
        paint.style = Paint.Style.FILL
        paint.color = 0xFF6B8A2E.toInt()
        canvas.drawCircle(centerX, centerY, radius, paint)

        val check =
            Path().apply {
                moveTo(centerX - radius * 0.52f, centerY)
                lineTo(centerX - radius * 0.12f, centerY + radius * 0.38f)
                lineTo(centerX + radius * 0.58f, centerY - radius * 0.42f)
            }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = square.width() * 0.035f
        paint.color = Color.WHITE
        canvas.drawPath(check, paint)
    }

    private fun drawLockedBadge(
        canvas: Canvas,
        square: RectF,
        paint: Paint,
    ) {
        val centerX = square.left + square.width() * 0.79f
        val centerY = square.top + square.height() * 0.21f
        val radius = square.width() * 0.13f
        paint.style = Paint.Style.FILL
        paint.color = 0xE63A4020.toInt()
        canvas.drawCircle(centerX, centerY, radius, paint)

        val bodyWidth = radius * 0.88f
        val bodyHeight = radius * 0.70f
        val body =
            RectF(
                centerX - bodyWidth / 2f,
                centerY - bodyHeight * 0.03f,
                centerX + bodyWidth / 2f,
                centerY + bodyHeight,
            )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = square.width() * 0.025f
        paint.color = Color.WHITE
        canvas.drawRoundRect(body, radius * 0.14f, radius * 0.14f, paint)
        val shackle =
            RectF(
                centerX - bodyWidth * 0.34f,
                centerY - bodyHeight * 0.62f,
                centerX + bodyWidth * 0.34f,
                centerY + bodyHeight * 0.22f,
            )
        canvas.drawArc(shackle, 180f, 180f, false, paint)
    }

    private fun insetSquare(
        square: RectF,
        normalizedInset: Float,
    ): RectF {
        val inset = normalizedInset * square.width()
        return RectF(square).apply { inset(inset, inset) }
    }
}

fun DrawScope.drawMotif(
    visual: SquareDesignVisual,
    config: MotifRenderConfig = MotifRenderConfig(),
): MotifRenderPlan =
    MotifRenderer.draw(
        canvas = drawContext.canvas.nativeCanvas,
        bounds = RectF(0f, 0f, size.width, size.height),
        visual = visual,
        config = config,
        smallPreviewThresholdPx = 24f * density,
    )
