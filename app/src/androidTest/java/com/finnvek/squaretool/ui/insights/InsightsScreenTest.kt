package com.finnvek.squaretool.ui.insights

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.ui.projects.ProjectCardModel
import com.finnvek.squaretool.ui.theme.SquareToolTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InsightsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun donutHasTextEquivalentAndEveryExportActionUsesItsCallback() {
        var action = ""
        val project = project()
        val model =
            InsightsModel(
                project = project,
                preview = ProjectCardModel(project),
                totalSquares = 96,
                designCount = 1,
                colorCount = 0,
                progress = null,
                distribution =
                    listOf(
                        DesignUsageItem("classic", "Classic", 96, 100.0, 0xFF6B8A2E.toInt()),
                    ),
                colorUsage = emptyList(),
                dimensions = null,
                yarnEstimate = null,
            )
        composeRule.setContent {
            SquareToolTheme {
                InsightsScreen(
                    state = InsightsUiState(model = model, isLoading = false),
                    onExportPdf = { action = "pdf" },
                    onSaveImage = { action = "image" },
                    onSharePdf = { action = "share-pdf" },
                    onShareImage = { action = "share-image" },
                    onExportBackup = { action = "backup" },
                )
            }
        }

        composeRule
            .onNodeWithTag("insights_distribution_chart")
            .assertContentDescriptionEquals("Square distribution. Classic: 96 squares, 100 percent.")

        listOf(
            "insights_export_pdf" to "pdf",
            "insights_save_image" to "image",
            "insights_share_pdf" to "share-pdf",
            "insights_share_image" to "share-image",
            "insights_export_backup" to "backup",
        ).forEach { (tag, expected) ->
            composeRule.onNodeWithTag(tag).performScrollTo().performClick()
            composeRule.runOnIdle { assertEquals(expected, action) }
        }
    }

    @Test
    fun colorUsageWithArgbValueRendersWithoutCrashing() {
        val project = project()
        val color =
            ColorEntity(
                id = "moss",
                name = "Moss Green",
                argb = 0xFF6B7A2C,
                builtIn = false,
                createdAt = 0,
                updatedAt = 0,
            )
        val model =
            InsightsModel(
                project = project,
                preview = ProjectCardModel(project),
                totalSquares = 1,
                designCount = 0,
                colorCount = 1,
                progress = null,
                distribution = emptyList(),
                colorUsage = listOf(ColorUsageItem(color, 100.0, null, null)),
                dimensions = null,
                yarnEstimate = null,
            )
        composeRule.setContent {
            SquareToolTheme {
                InsightsScreen(state = InsightsUiState(model = model, isLoading = false))
            }
        }

        composeRule.onNodeWithText("Moss Green").performScrollTo().assertIsDisplayed()
    }

    private fun project() =
        ProjectEntity(
            id = "p",
            name = "Autumn Garden Blanket",
            rowCount = 8,
            columnCount = 12,
            squareWidthValue = null,
            squareHeightValue = null,
            measurementUnit = "centimeters",
            joiningGapValue = null,
            trackingEnabled = false,
            favorite = false,
            notes = "",
            createdAt = 1,
            updatedAt = 1,
            lastOpenedAt = 1,
            generationSeed = 1,
            defaultSquareDesignId = null,
            globalGramsPerSquare = null,
            skeinWeightGrams = null,
            joiningAndEdgingBufferPercent = 10.0,
            demoProject = false,
        )
}
