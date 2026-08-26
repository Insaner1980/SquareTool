package com.finnvek.squaretool.ui.library

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class LibraryTab { COLORS, PALETTES }

data class HslColor(
    val hue: Float,
    val saturation: Float,
    val lightness: Float,
    val alpha: Float = 1f,
)

fun parseHexColor(value: String): Long? {
    val digits = value.trim().removePrefix("#")
    if (digits.length != 6 && digits.length != 8) return null
    if (!digits.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    val parsed = digits.toLongOrNull(16) ?: return null
    return if (digits.length == 6) 0xFF00_0000L or parsed else parsed
}

fun formatHexColor(argb: Long): String =
    if (((argb shr 24) and 0xFF) == 0xFFL) {
        "#%06X".format(argb and 0x00FF_FFFFL)
    } else {
        "#%08X".format(argb and 0xFFFF_FFFFL)
    }

fun argbToHsl(argb: Long): HslColor {
    val alpha = ((argb shr 24) and 0xFF) / 255f
    val red = ((argb shr 16) and 0xFF) / 255f
    val green = ((argb shr 8) and 0xFF) / 255f
    val blue = (argb and 0xFF) / 255f
    val maximum = max(red, max(green, blue))
    val minimum = min(red, min(green, blue))
    val lightness = (maximum + minimum) / 2f
    val delta = maximum - minimum
    if (delta == 0f) return HslColor(0f, 0f, lightness, alpha)
    val saturation = delta / (1f - abs(2f * lightness - 1f))
    val hueSection =
        when (maximum) {
            red -> ((green - blue) / delta) % 6f
            green -> (blue - red) / delta + 2f
            else -> (red - green) / delta + 4f
        }
    val hue = ((hueSection * 60f) + 360f) % 360f
    return HslColor(hue, saturation.coerceIn(0f, 1f), lightness, alpha)
}

fun hslToArgb(hsl: HslColor): Long {
    val hue = ((hsl.hue % 360f) + 360f) % 360f
    val saturation = hsl.saturation.coerceIn(0f, 1f)
    val lightness = hsl.lightness.coerceIn(0f, 1f)
    val chroma = (1f - abs(2f * lightness - 1f)) * saturation
    val section = hue / 60f
    val secondary = chroma * (1f - abs(section % 2f - 1f))
    val (redBase, greenBase, blueBase) =
        when (section.toInt()) {
            0 -> Triple(chroma, secondary, 0f)
            1 -> Triple(secondary, chroma, 0f)
            2 -> Triple(0f, chroma, secondary)
            3 -> Triple(0f, secondary, chroma)
            4 -> Triple(secondary, 0f, chroma)
            else -> Triple(chroma, 0f, secondary)
        }
    val match = lightness - chroma / 2f
    val alpha = (hsl.alpha.coerceIn(0f, 1f) * 255f).roundToInt().toLong()
    val red = ((redBase + match) * 255f).roundToInt().toLong()
    val green = ((greenBase + match) * 255f).roundToInt().toLong()
    val blue = ((blueBase + match) * 255f).roundToInt().toLong()
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}

enum class ColorDraftError { NAME, HEX, SKEIN_WEIGHT, YARN_LENGTH }

data class ColorEditorDraft(
    val id: String = "",
    val name: String = "",
    val hex: String = "#6B8A2E",
    val yarnBrand: String = "",
    val yarnLine: String = "",
    val shadeName: String = "",
    val shadeCode: String = "",
    val skeinWeightGrams: String = "",
    val yarnLength: String = "",
    val yarnLengthUnit: String = "m",
    val notes: String = "",
    val sourceBuiltIn: Boolean = false,
) {
    fun validationErrors(): Set<ColorDraftError> =
        buildSet {
            if (name.isBlank()) add(ColorDraftError.NAME)
            if (parseHexColor(hex) == null) add(ColorDraftError.HEX)
            if (skeinWeightGrams.isNotBlank() && skeinWeightGrams.toDoubleOrNull()?.takeIf { it > 0.0 } == null) {
                add(ColorDraftError.SKEIN_WEIGHT)
            }
            if (yarnLength.isNotBlank() && yarnLength.toDoubleOrNull()?.takeIf { it > 0.0 } == null) {
                add(ColorDraftError.YARN_LENGTH)
            }
        }
}

data class PaletteEditorDraft(
    val id: String = "",
    val name: String = "",
    val colorIds: List<String> = emptyList(),
    val sourceBuiltIn: Boolean = false,
) {
    val isValid: Boolean get() = name.isNotBlank() && colorIds.isNotEmpty() && colorIds.distinct().size == colorIds.size
}
