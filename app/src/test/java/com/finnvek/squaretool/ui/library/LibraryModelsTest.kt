package com.finnvek.squaretool.ui.library

import com.finnvek.squaretool.ui.moveListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryModelsTest {
    @Test
    fun acceptsRgbAndArgbHexAndNormalizesCase() {
        assertEquals(0xFF6B8A2EL, parseHexColor("#6b8a2e"))
        assertEquals(0x806B8A2EL, parseHexColor("806B8A2E"))
        assertEquals("#6B8A2E", formatHexColor(0xFF6B8A2E))
        assertEquals("#806B8A2E", formatHexColor(0x806B8A2E))
    }

    @Test
    fun rejectsShorthandNonHexAndWrongLengths() {
        assertNull(parseHexColor("#abc"))
        assertNull(parseHexColor("#GG6B8A"))
        assertNull(parseHexColor("#1234567"))
    }

    @Test
    fun hslRoundTripKeepsRepresentativeColorsWithinOneChannelStep() {
        listOf(0xFFD75A1FL, 0xFF6B8A2EL, 0xFFF3E6C9L, 0x80123456L).forEach { argb ->
            val roundTrip = hslToArgb(argbToHsl(argb))
            assertArgbClose(argb, roundTrip)
        }
    }

    @Test
    fun colorDraftRequiresNameValidHexAndPositiveOptionalNumbers() {
        val valid = ColorEditorDraft(name = "Olive", hex = "#6B8A2E")
        assertTrue(valid.validationErrors().isEmpty())

        val invalid = valid.copy(name = " ", hex = "olive", skeinWeightGrams = "0", yarnLength = "-2")
        assertEquals(
            setOf(ColorDraftError.NAME, ColorDraftError.HEX, ColorDraftError.SKEIN_WEIGHT, ColorDraftError.YARN_LENGTH),
            invalid.validationErrors(),
        )
    }

    @Test
    fun paletteReorderRetainsEveryColorExactlyOnce() {
        val reordered = moveListItem(listOf("olive", "cream", "rust"), 0, 2)

        assertEquals(listOf("cream", "rust", "olive"), reordered)
        assertFalse(reordered.toSet().size != reordered.size)
    }

    private fun assertArgbClose(
        expected: Long,
        actual: Long,
    ) {
        for (shift in listOf(24, 16, 8, 0)) {
            val expectedChannel = ((expected shr shift) and 0xFF).toInt()
            val actualChannel = ((actual shr shift) and 0xFF).toInt()
            assertTrue("channel $shift: $expectedChannel != $actualChannel", kotlin.math.abs(expectedChannel - actualChannel) <= 1)
        }
    }
}
