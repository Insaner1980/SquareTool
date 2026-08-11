package com.finnvek.squaretool.ui.export

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.squaretool.ui.theme.SquareToolTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportProjectScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun exportActions_dispatchRealCallbacks() {
        val actions = mutableListOf<String>()
        composeRule.setContent {
            SquareToolTheme {
                ExportProjectScreen(
                    projectName = "Autumn Garden Blanket",
                    onSavePdf = { actions += "pdf" },
                    onSavePng = { actions += "png" },
                    onSharePdf = { actions += "share-pdf" },
                    onSharePng = { actions += "share-png" },
                    onExportBackup = { actions += "backup" },
                )
            }
        }

        listOf("save_pdf", "save_png", "share_pdf", "share_png", "export_backup").forEach {
            composeRule.onNodeWithTag(it).performClick()
        }
        composeRule.runOnIdle { assertEquals(listOf("pdf", "png", "share-pdf", "share-png", "backup"), actions) }
    }
}
