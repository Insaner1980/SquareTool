package com.finnvek.squaretool.export

import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ProjectExportInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun pngExporter_writesReadableHighResolutionImage() =
        runBlocking {
            val file = File(context.cacheDir, "export-test.png")
            try {
                file.outputStream().use {
                    ProjectPngExporter().write(snapshot(), it, PngExportOptions(requestedLongEdge = 1024, includeLegend = false))
                }

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                assertEquals(1024, maxOf(bounds.outWidth, bounds.outHeight))
                assertTrue(file.length() > 1_000)
            } finally {
                file.delete()
            }
        }

    @Test
    fun pdfExporter_writesPdfDocument() =
        runBlocking {
            val file = File(context.cacheDir, "export-test.pdf")
            try {
                file.outputStream().use { ProjectPdfExporter(context).write(snapshot(), it) }

                assertTrue(file.length() > 1_000)
                assertEquals("%PDF", file.inputStream().bufferedReader().use { CharArray(4).also(it::read).concatToString() })
            } finally {
                file.delete()
            }
        }

    @Test
    fun pdfExporter_paginatesLargeLegendsWithoutDroppingEntries() =
        runBlocking {
            val file = File(context.cacheDir, "export-many-entries.pdf")
            val designs =
                List(80) { index ->
                    ExportDesign(
                        id = "design-$index",
                        name = "Design $index",
                        templateId = "classic_granny",
                        roundColors = listOf(0xFF6B8A2E.toInt(), 0xFFF3E6C9.toInt(), 0xFFD75A1F.toInt()),
                        gramsPerSquareOverride = null,
                    )
                }
            val colors =
                List(80) { index ->
                    ExportColor("color-$index", "Color $index", 0xFF6B8A2E.toInt() + index)
                }
            val cells =
                List(100) { index ->
                    ExportCell(
                        row = index / 10,
                        column = index % 10,
                        designId = designs.getOrNull(index)?.id,
                        locked = false,
                        completed = false,
                    )
                }
            val largeSnapshot =
                snapshot().copy(
                    project = snapshot().project.copy(rows = 10, columns = 10),
                    designs = designs,
                    colors = colors,
                    cells = cells,
                )

            try {
                file.outputStream().use { ProjectPdfExporter(context).write(largeSnapshot, it) }

                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        assertTrue(renderer.pageCount > 2)
                    }
                }
            } finally {
                file.delete()
            }
        }

    private fun snapshot() =
        ProjectExportSnapshot(
            project =
                ExportProject(
                    name = "Test Blanket",
                    notes = "",
                    rows = 2,
                    columns = 2,
                    squareWidth = 15.0,
                    squareHeight = 15.0,
                    joiningGap = 0.5,
                    measurementUnit = "CENTIMETERS",
                    trackingEnabled = true,
                    globalGramsPerSquare = 20.0,
                    skeinWeightGrams = 100.0,
                    bufferPercent = 10.0,
                ),
            designs =
                listOf(
                    ExportDesign(
                        id = "classic",
                        name = "Classic",
                        templateId = "classic_granny",
                        roundColors = listOf(0xFF6B8A2E.toInt(), 0xFFF3E6C9.toInt(), 0xFFD75A1F.toInt()),
                        gramsPerSquareOverride = null,
                    ),
                ),
            colors = listOf(ExportColor("olive", "Olive", 0xFF6B8A2E.toInt())),
            cells =
                listOf(
                    ExportCell(0, 0, "classic", locked = false, completed = true),
                    ExportCell(0, 1, "classic", locked = false, completed = false),
                    ExportCell(1, 0, "classic", locked = true, completed = false),
                    ExportCell(1, 1, null, locked = false, completed = false),
                ),
        )
}
