package com.finnvek.squaretool.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class SettingsNumberParserTest {
    @Test
    fun groupingAndDecimalSeparatorsUseTheSelectedLocale() {
        assertEquals(1_000.5, parseLocalizedNumber("1,000.5", Locale.US) ?: 0.0, 0.0)
        assertEquals(
            1_000.5,
            parseLocalizedNumber("1\u00A0000,5", Locale.forLanguageTag("fi-FI")) ?: 0.0,
            0.0,
        )
    }

    @Test
    fun incompleteInputIsRejected() {
        assertNull(parseLocalizedNumber("12 kg", Locale.US))
        assertNull(parseLocalizedNumber("", Locale.US))
    }
}
