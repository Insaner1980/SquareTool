package com.finnvek.squaretool.render

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class SquareDesignVisual(
    val templateId: String,
    val roundColors: List<Int>,
)

enum class MotifRenderDetail {
    AUTO,
    SMALL,
    FULL,
}

enum class MotifSurface {
    LIGHT,
    DARK,
}

data class MotifRenderConfig(
    val detail: MotifRenderDetail = MotifRenderDetail.AUTO,
    val surface: MotifSurface = MotifSurface.LIGHT,
    val selected: Boolean = false,
    val locked: Boolean = false,
    val completed: Boolean = false,
)

data class NormalizedPoint(
    val x: Float,
    val y: Float,
)

sealed interface MotifPrimitive {
    val roundIndex: Int

    data class RoundedSquare(
        override val roundIndex: Int,
        val inset: Float,
        val cornerRadius: Float,
        val outlineWidth: Float,
    ) : MotifPrimitive

    data class Circle(
        override val roundIndex: Int,
        val centerX: Float,
        val centerY: Float,
        val radius: Float,
        val outlineWidth: Float,
    ) : MotifPrimitive

    data class Petal(
        override val roundIndex: Int,
        val centerX: Float,
        val centerY: Float,
        val width: Float,
        val height: Float,
        val rotationDegrees: Float,
        val outlineWidth: Float,
    ) : MotifPrimitive

    data class Diamond(
        override val roundIndex: Int,
        val centerX: Float,
        val centerY: Float,
        val halfExtent: Float,
        val rotationDegrees: Float,
        val cornerRadius: Float,
        val outlineWidth: Float,
    ) : MotifPrimitive

    data class Arc(
        override val roundIndex: Int,
        val centerX: Float,
        val centerY: Float,
        val radius: Float,
        val startAngleDegrees: Float,
        val sweepAngleDegrees: Float,
        val strokeWidth: Float,
        val outlineWidth: Float,
    ) : MotifPrimitive

    data class Polygon(
        override val roundIndex: Int,
        val points: List<NormalizedPoint>,
        val outlineWidth: Float,
    ) : MotifPrimitive
}

sealed interface MotifOverlay {
    data class CompletionWash(
        val alpha: Float,
    ) : MotifOverlay

    data object CompletedCheck : MotifOverlay

    data object LockedBadge : MotifOverlay

    data class SelectionBorder(
        val width: Float,
    ) : MotifOverlay
}

data class MotifRenderPlan(
    val templateId: String,
    val roundColors: List<Int>,
    val detail: MotifRenderDetail,
    val surface: MotifSurface,
    val outlineArgb: Int,
    val primitives: List<MotifPrimitive>,
    val overlays: List<MotifOverlay>,
)

object MotifGeometryPlanner {
    private const val LIGHT_OUTLINE_ARGB = 0x663A4020
    private val DARK_OUTLINE_ARGB = 0x99FFF8E8.toInt()

    fun createPlan(
        visual: SquareDesignVisual,
        config: MotifRenderConfig = MotifRenderConfig(),
    ): MotifRenderPlan {
        val template = MotifTemplateRegistry.require(visual.templateId)
        template.requireSupportedRoundCount(visual.roundColors.size)
        val detail = config.detail.takeUnless { it == MotifRenderDetail.AUTO } ?: MotifRenderDetail.FULL
        val primitives = buildGeometry(template.geometryStyle, visual.roundColors.size, detail)
        val overlays =
            buildList {
                if (config.completed) {
                    add(MotifOverlay.CompletionWash(alpha = 0.14f))
                    add(MotifOverlay.CompletedCheck)
                }
                if (config.locked) add(MotifOverlay.LockedBadge)
                if (config.selected) add(MotifOverlay.SelectionBorder(width = 0.035f))
            }

        return MotifRenderPlan(
            templateId = template.id,
            roundColors = visual.roundColors.toList(),
            detail = detail,
            surface = config.surface,
            outlineArgb =
                when (config.surface) {
                    MotifSurface.LIGHT -> LIGHT_OUTLINE_ARGB
                    MotifSurface.DARK -> DARK_OUTLINE_ARGB
                },
            primitives = primitives,
            overlays = overlays,
        )
    }

    private fun buildGeometry(
        style: MotifGeometryStyle,
        roundCount: Int,
        detail: MotifRenderDetail,
    ): List<MotifPrimitive> =
        when (style) {
            MotifGeometryStyle.CLASSIC_GRANNY -> classicGranny(roundCount, detail)
            MotifGeometryStyle.SUNBURST -> sunburst(roundCount, detail)
            MotifGeometryStyle.DAISY -> daisy(roundCount, detail)
            MotifGeometryStyle.FLOWER_MEDALLION -> flowerMedallion(roundCount, detail)
            MotifGeometryStyle.SOLID_CENTER -> solidCenter(roundCount, detail)
            MotifGeometryStyle.STAR_BLOOM -> starBloom(roundCount, detail)
            MotifGeometryStyle.DIAMOND_LAYERS -> diamondLayers(roundCount, detail)
            MotifGeometryStyle.PINWHEEL -> pinwheel(roundCount, detail)
            MotifGeometryStyle.CORNER_ACCENT -> cornerAccent(roundCount, detail)
            MotifGeometryStyle.SIMPLE_ROUNDS -> roundedLayers(roundCount)
        }

    private fun classicGranny(
        roundCount: Int,
        detail: MotifRenderDetail,
    ): List<MotifPrimitive> {
        if (detail == MotifRenderDetail.SMALL) return roundedLayers(roundCount)
        return buildList {
            add(outerSquare(roundCount))
            for (roundIndex in roundCount - 2 downTo 1) {
                addAll(
                    petalRing(
                        roundIndex = roundIndex,
                        roundCount = roundCount,
                        petalCount = 8,
                        radialOffsetDegrees = if (roundIndex % 2 == 0) 22.5f else 0f,
                        width = 0.105f,
                        height = 0.16f,
                    ),
                )
            }
            add(MotifPrimitive.Circle(0, 0.5f, 0.5f, 0.105f, 0.009f))
        }
    }

    private fun sunburst(
        roundCount: Int,
        detail: MotifRenderDetail,
    ): List<MotifPrimitive> {
        if (detail == MotifRenderDetail.SMALL) return concentricCircles(roundCount)
        return buildList {
            add(outerSquare(roundCount))
            for (roundIndex in roundCount - 2 downTo 1) {
                addAll(
                    petalRing(
                        roundIndex = roundIndex,
                        roundCount = roundCount,
                        petalCount = 10 + roundIndex * 2,
                        radialOffsetDegrees = roundIndex * 9f,
                        width = 0.075f,
                        height = 0.19f,
                    ),
                )
            }
            add(MotifPrimitive.Circle(0, 0.5f, 0.5f, 0.09f, 0.008f))
        }
    }

    private fun daisy(
        roundCount: Int,
        detail: MotifRenderDetail,
    ): List<MotifPrimitive> {
        if (detail == MotifRenderDetail.SMALL) return concentricCircles(roundCount)
        return buildList {
            addAll(roundedLayers(roundCount))
            addAll(petalRing(2, roundCount, 8, 22.5f, 0.11f, 0.17f))
            addAll(petalRing(1, roundCount, 8, 0f, 0.13f, 0.21f))
            add(MotifPrimitive.Circle(0, 0.5f, 0.5f, 0.08f, 0.008f))
        }
    }

    private fun flowerMedallion(
        roundCount: Int,
        detail: MotifRenderDetail,
    ): List<MotifPrimitive> {
        if (detail == MotifRenderDetail.SMALL) return concentricCircles(roundCount)
        return buildList {
            add(outerSquare(roundCount))
            for (roundIndex in roundCount - 2 downTo 1) {
                addAll(
                    petalRing(
                        roundIndex = roundIndex,
                        roundCount = roundCount,
                        petalCount = if (roundIndex % 2 == 0) 12 else 8,
                        radialOffsetDegrees = roundIndex * 11.25f,
                        width = 0.10f,
                        height = 0.18f,
                    ),
                )
            }
            add(MotifPrimitive.Circle(0, 0.5f, 0.5f, 0.085f, 0.008f))
        }
    }

    private fun solidCenter(
        roundCount: Int,
        detail: MotifRenderDetail,
    ): List<MotifPrimitive> =
        buildList {
            addAll(roundedLayers(roundCount))
            if (detail == MotifRenderDetail.FULL) {
                val accentRound = (roundCount - 2).coerceAtLeast(0)
                val positions = listOf(0.20f, 0.38f, 0.62f, 0.80f)
                positions.forEach { position ->
                    add(MotifPrimitive.Petal(accentRound, position, 0.09f, 0.10f, 0.055f, 0f, 0.006f))
                    add(MotifPrimitive.Petal(accentRound, position, 0.91f, 0.10f, 0.055f, 0f, 0.006f))
                    add(MotifPrimitive.Petal(accentRound, 0.09f, position, 0.055f, 0.10f, 0f, 0.006f))
                    add(MotifPrimitive.Petal(accentRound, 0.91f, position, 0.055f, 0.10f, 0f, 0.006f))
                }
            }
        }

    private fun starBloom(
        roundCount: Int,
        detail: MotifRenderDetail,
    ): List<MotifPrimitive> =
        buildList {
            add(outerSquare(roundCount))
            val innerLayerCount = roundCount - 1
            for (roundIndex in roundCount - 2 downTo 0) {
                val depth = (roundCount - 2) - roundIndex
                val fraction = fraction(depth, innerLayerCount - 1)
                val radius = lerp(0.41f, 0.11f, fraction)
                add(
                    MotifPrimitive.Polygon(
                        roundIndex = roundIndex,
                        points =
                            starPoints(
                                pointCount = if (detail == MotifRenderDetail.SMALL) 6 else 8,
                                outerRadius = radius,
                                innerRadius = radius * if (detail == MotifRenderDetail.SMALL) 0.72f else 0.58f,
                                rotationDegrees = if (roundIndex % 2 == 0) -90f else -67.5f,
                            ),
                        outlineWidth = lerp(0.011f, 0.007f, fraction),
                    ),
                )
            }
            if (detail == MotifRenderDetail.FULL) {
                addAll(petalRing(1, roundCount, 8, 22.5f, 0.08f, 0.14f))
            }
        }

    private fun diamondLayers(
        roundCount: Int,
        detail: MotifRenderDetail,
    ): List<MotifPrimitive> =
        buildList {
            add(outerSquare(roundCount))
            val innerLayerCount = roundCount - 1
            for (roundIndex in roundCount - 2 downTo 0) {
                val depth = (roundCount - 2) - roundIndex
                val fraction = fraction(depth, innerLayerCount - 1)
                add(
                    MotifPrimitive.Diamond(
                        roundIndex = roundIndex,
                        centerX = 0.5f,
                        centerY = 0.5f,
                        halfExtent = lerp(0.36f, 0.09f, fraction),
                        rotationDegrees = if (roundIndex % 2 == 0) 45f else 0f,
                        cornerRadius = lerp(0.045f, 0.025f, fraction),
                        outlineWidth = lerp(0.011f, 0.007f, fraction),
                    ),
                )
            }
            if (detail == MotifRenderDetail.FULL) {
                val cornerRound = (roundCount - 2).coerceAtLeast(0)
                listOf(0.14f to 0.14f, 0.86f to 0.14f, 0.14f to 0.86f, 0.86f to 0.86f).forEach { (x, y) ->
                    add(MotifPrimitive.Circle(cornerRound, x, y, 0.065f, 0.007f))
                }
            }
        }

    private fun pinwheel(
        roundCount: Int,
        detail: MotifRenderDetail,
    ): List<MotifPrimitive> =
        buildList {
            add(outerSquare(roundCount))
            for (roundIndex in roundCount - 2 downTo 0) {
                val radius = 0.12f + 0.25f * fraction(roundIndex, roundCount - 2)
                val bladeCount = if (detail == MotifRenderDetail.SMALL) 1 else 4
                repeat(bladeCount) { blade ->
                    add(
                        MotifPrimitive.Arc(
                            roundIndex = roundIndex,
                            centerX = 0.5f,
                            centerY = 0.5f,
                            radius = radius,
                            startAngleDegrees = q(roundIndex * 37f + blade * (360f / bladeCount)),
                            sweepAngleDegrees = if (detail == MotifRenderDetail.SMALL) 255f else 58f,
                            strokeWidth = if (detail == MotifRenderDetail.SMALL) 0.11f else 0.095f,
                            outlineWidth = 0.009f,
                        ),
                    )
                }
            }
        }

    private fun cornerAccent(
        roundCount: Int,
        detail: MotifRenderDetail,
    ): List<MotifPrimitive> =
        buildList {
            addAll(roundedLayers(roundCount))
            val repeatedRounds =
                if (detail == MotifRenderDetail.SMALL) {
                    listOf((roundCount - 2).coerceAtLeast(0))
                } else {
                    (roundCount - 2 downTo 0).toList()
                }
            repeatedRounds.forEachIndexed { depth, roundIndex ->
                val centerInset = 0.14f + depth * 0.06f
                val radius = (0.075f - depth * 0.008f).coerceAtLeast(0.04f)
                listOf(
                    centerInset to centerInset,
                    (1f - centerInset) to centerInset,
                    centerInset to (1f - centerInset),
                    (1f - centerInset) to (1f - centerInset),
                ).forEach { (x, y) ->
                    add(MotifPrimitive.Circle(roundIndex, q(x), q(y), q(radius), 0.006f))
                }
            }
        }

    private fun roundedLayers(roundCount: Int): List<MotifPrimitive> =
        (0 until roundCount).map { depth ->
            val fraction = fraction(depth, roundCount - 1)
            MotifPrimitive.RoundedSquare(
                roundIndex = roundCount - 1 - depth,
                inset = lerp(0.04f, 0.36f, fraction),
                cornerRadius = lerp(0.12f, 0.08f, fraction),
                outlineWidth = lerp(0.012f, 0.008f, fraction),
            )
        }

    private fun concentricCircles(roundCount: Int): List<MotifPrimitive> =
        buildList {
            add(outerSquare(roundCount))
            val innerLayerCount = roundCount - 1
            for (roundIndex in roundCount - 2 downTo 0) {
                val depth = (roundCount - 2) - roundIndex
                val fraction = fraction(depth, innerLayerCount - 1)
                add(
                    MotifPrimitive.Circle(
                        roundIndex = roundIndex,
                        centerX = 0.5f,
                        centerY = 0.5f,
                        radius = lerp(0.40f, 0.10f, fraction),
                        outlineWidth = lerp(0.011f, 0.007f, fraction),
                    ),
                )
            }
        }

    private fun outerSquare(roundCount: Int) =
        MotifPrimitive.RoundedSquare(
            roundIndex = roundCount - 1,
            inset = 0.04f,
            cornerRadius = 0.12f,
            outlineWidth = 0.012f,
        )

    private fun petalRing(
        roundIndex: Int,
        roundCount: Int,
        petalCount: Int,
        radialOffsetDegrees: Float,
        width: Float,
        height: Float,
    ): List<MotifPrimitive.Petal> {
        val ringRadius = 0.12f + 0.25f * fraction(roundIndex, roundCount - 2)
        return List(petalCount) { petalIndex ->
            val angleDegrees = -90f + radialOffsetDegrees + petalIndex * (360f / petalCount)
            val angleRadians = angleDegrees * PI / 180.0
            MotifPrimitive.Petal(
                roundIndex = roundIndex,
                centerX = q(0.5f + cos(angleRadians).toFloat() * ringRadius),
                centerY = q(0.5f + sin(angleRadians).toFloat() * ringRadius),
                width = q(width),
                height = q(height),
                rotationDegrees = q(angleDegrees + 90f),
                outlineWidth = 0.007f,
            )
        }
    }

    private fun starPoints(
        pointCount: Int,
        outerRadius: Float,
        innerRadius: Float,
        rotationDegrees: Float,
    ): List<NormalizedPoint> =
        List(pointCount * 2) { pointIndex ->
            val radius = if (pointIndex % 2 == 0) outerRadius else innerRadius
            val angleDegrees = rotationDegrees + pointIndex * (180f / pointCount)
            val angleRadians = angleDegrees * PI / 180.0
            NormalizedPoint(
                x = q(0.5f + cos(angleRadians).toFloat() * radius),
                y = q(0.5f + sin(angleRadians).toFloat() * radius),
            )
        }

    private fun fraction(
        value: Int,
        maximum: Int,
    ): Float = if (maximum <= 0) 0f else value.toFloat() / maximum

    private fun lerp(
        start: Float,
        end: Float,
        fraction: Float,
    ): Float = q(start + (end - start) * fraction)

    private fun q(value: Float): Float = (value * 10_000f).roundToInt() / 10_000f
}
