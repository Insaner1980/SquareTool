package com.finnvek.squaretool.app

import android.app.Application

class SquareToolApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
