package com.finnvek.squaretool.app

import android.view.MotionEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivitySecurityTest {
    @Test
    fun obscuredTouchesAreRejected() {
        assertTrue(isObscuredTouch(MotionEvent.FLAG_WINDOW_IS_OBSCURED))
        assertTrue(isObscuredTouch(MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED))
    }

    @Test
    fun unobscuredTouchesRemainAllowed() {
        assertFalse(isObscuredTouch(0))
    }
}
