package com.finnvek.squaretool.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.decorView.setFilterTouchesWhenObscured(true)
        enableEdgeToEdge()

        val container = (application as SquareToolApplication).container
        setContent { SquareToolApp(container) }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (isObscuredTouch(event.flags)) return false
        return super.dispatchTouchEvent(event)
    }
}

@SuppressLint("InlinedApi")
internal fun isObscuredTouch(flags: Int): Boolean =
    flags and (MotionEvent.FLAG_WINDOW_IS_OBSCURED or MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED) != 0
