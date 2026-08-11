package com.finnvek.squaretool.ui.projects

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.ui.theme.SquareToolTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun projectCardOpensPlannerAndInsights() {
        var action = ""
        val project = project()
        composeRule.setContent {
            SquareToolTheme {
                ProjectsScreen(
                    state =
                        ProjectsUiState(
                            projects = listOf(ProjectCardModel(project)),
                            isLoading = false,
                        ),
                    onOpenProject = { action = "open:$it" },
                    onOpenInsights = { action = "insights:$it" },
                )
            }
        }

        composeRule.onNodeWithTag("project_open_p").performClick()
        composeRule.runOnIdle { assertEquals("open:p", action) }
        composeRule.onNodeWithTag("project_insights_p").performClick()
        composeRule.runOnIdle { assertEquals("insights:p", action) }
    }

    @Test
    fun deleteRequiresExplicitConfirmation() {
        var confirmed = false
        val project = project()
        composeRule.setContent {
            SquareToolTheme {
                ProjectsScreen(
                    state =
                        ProjectsUiState(
                            projects = listOf(ProjectCardModel(project)),
                            confirmation = ProjectConfirmation.Delete(project),
                            isLoading = false,
                        ),
                    onConfirmDelete = { confirmed = true },
                )
            }
        }

        composeRule.onNodeWithTag("project_delete_confirmation").assertIsDisplayed()
        composeRule.onNodeWithTag("project_confirm_delete").performClick()
        composeRule.runOnIdle { assertEquals(true, confirmed) }
    }

    private fun project() =
        ProjectEntity(
            id = "p",
            name = "Autumn blanket",
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
