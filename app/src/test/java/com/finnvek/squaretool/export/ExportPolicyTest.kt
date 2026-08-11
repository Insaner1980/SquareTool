package com.finnvek.squaretool.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportPolicyTest {
    @Test
    fun sanitizedBaseName_removesUnsafeCharactersAndCollapsesWhitespace() {
        assertEquals("Autumn_Garden_Blanket", ExportPolicy.sanitizedBaseName("  Autumn / Garden: Blanket  "))
    }

    @Test
    fun bitmapSize_preservesGridAspectRatio() {
        val size = ExportPolicy.bitmapSize(rows = 12, columns = 8, requestedLongEdge = 2048)

        assertEquals(1365, size.width)
        assertEquals(2048, size.height)
    }

    @Test
    fun bitmapSize_limitsTotalPixels() {
        val size = ExportPolicy.bitmapSize(rows = 1, columns = 50, requestedLongEdge = 16_384)

        assertTrue(size.width.toLong() * size.height <= ExportPolicy.MAX_EXPORT_PIXELS)
        assertTrue(size.width >= size.height)
        assertTrue(ExportPolicy.MAX_EXPORT_PIXELS <= 16_000_000L)
    }

    @Test
    fun gridSections_coverLargeGridWithoutOversizedTiles() {
        val sections = ExportPolicy.gridSections(rows = 50, columns = 50, maxRows = 16, maxColumns = 12)

        assertEquals(20, sections.size)
        assertEquals(GridSection(0, 0, 16, 12), sections.first())
        assertEquals(GridSection(48, 48, 2, 2), sections.last())
    }
}
