package com.finnvek.squaretool.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

internal inline fun <reified T : ViewModel> simpleViewModelFactory(crossinline create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
            require(modelClass.isAssignableFrom(T::class.java))
            @Suppress("UNCHECKED_CAST")
            return create() as VM
        }
    }
