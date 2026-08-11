package com.finnvek.squaretool.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyStateOffersWorkingCreateAndSampleActions() {
        var action = ""
        composeRule.setContent {
            SquareToolTheme {
                HomeScreen(
                    state = HomeUiState(isLoading = false),
                    onNewProject = { action = "new" },
                    onCreateSample = { action = "sample" },
                )
            }
        }

        composeRule.onNodeWithTag("home_empty_state").assertIsDisplayed()
        composeRule.onNodeWithTag("home_create_project").performClick()
        composeRule.runOnIdle { assertEquals("new", action) }
        composeRule.onNodeWithTag("home_create_sample").performClick()
        composeRule.runOnIdle { assertEquals("sample", action) }
    }

    @Test
    fun yarnPaletteWithArgbValueRendersWithoutCrashing() {
        val color =
            ColorEntity(
                id = "moss",
                name = "Moss Green",
                argb = 0xFF6B7A2C,
                builtIn = false,
                createdAt = 0,
                updatedAt = 0,
            )
        composeRule.setContent {
            SquareToolTheme {
                HomeScreen(
                    state =
                        HomeUiState(
                            current = ProjectCardModel(project(), palette = listOf(color)),
                            isLoading = false,
                        ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Moss Green").performScrollTo().assertIsDisplayed()
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
