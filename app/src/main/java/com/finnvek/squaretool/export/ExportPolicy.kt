package com.finnvek.squaretool.export

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class ExportBitmapSize(
    val width: Int,
    val height: Int,
)

data class GridSection(
    val startRow: Int,
    val startColumn: Int,
    val rowCount: Int,
    val columnCount: Int,
)

object ExportPolicy {
    const val MAX_EXPORT_PIXELS = 16_000_000L

    fun sanitizedBaseName(name: String): String {
        val normalized =
            buildString {
                name.trim().forEach { character ->
                    append(if (character.isLetterOrDigit()) character else '_')
                }
            }
        return normalized.replace(Regex("_+"), "_").trim('_').ifEmpty { "SquareTool_project" }
    }

    fun bitmapSize(
        rows: Int,
        columns: Int,
        requestedLongEdge: Int,
    ): ExportBitmapSize {
        require(rows > 0 && columns > 0)
        require(requestedLongEdge > 0)

        val aspect = columns.toDouble() / rows
        var width: Int
        var height: Int
        if (aspect >= 1.0) {
            width = requestedLongEdge
            height = max(1, (requestedLongEdge / aspect).roundToInt())
        } else {
            height = requestedLongEdge
            width = max(1, (requestedLongEdge * aspect).roundToInt())
        }

        val pixels = width.toLong() * height
        if (pixels > MAX_EXPORT_PIXELS) {
            val scale = sqrt(MAX_EXPORT_PIXELS.toDouble() / pixels)
            width = max(1, (width * scale).toInt())
            height = max(1, (height * scale).toInt())
        }
        return ExportBitmapSize(width, height)
    }

    fun gridSections(
        rows: Int,
        columns: Int,
        maxRows: Int,
        maxColumns: Int,
    ): List<GridSection> {
        require(rows > 0 && columns > 0 && maxRows > 0 && maxColumns > 0)
        return buildList {
            for (row in 0 until rows step maxRows) {
                for (column in 0 until columns step maxColumns) {
                    add(
                        GridSection(
                            startRow = row,
                            startColumn = column,
                            rowCount = minOf(maxRows, rows - row),
                            columnCount = minOf(maxColumns, columns - column),
                        ),
                    )
                }
            }
        }
    }
}
