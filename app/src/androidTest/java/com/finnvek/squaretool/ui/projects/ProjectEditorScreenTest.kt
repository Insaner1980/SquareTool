package com.finnvek.squaretool.ui.projects

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.ui.theme.SquareToolTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectEditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shrinkWarningExplainsLossAndRequiresConfirmation() {
        var confirmed = false
        composeRule.setContent {
            SquareToolTheme {
                ProjectEditorScreen(
                    state =
                        ProjectEditorUiState(
                            pendingShrink = ShrinkImpact(lostCellCount = 8, lostAssignedCellCount = 5),
                            isLoading = false,
                        ),
                    isEditing = true,
                    onConfirmShrink = { confirmed = true },
                )
            }
        }

        composeRule.onNodeWithTag("project_shrink_confirmation").assertIsDisplayed()
        composeRule.onNodeWithTag("project_confirm_shrink").performClick()
        composeRule.runOnIdle { assertTrue(confirmed) }
    }

    @Test
    fun paletteColorWithArgbValueRendersWithoutCrashing() {
        composeRule.setContent {
            SquareToolTheme {
                ProjectEditorScreen(
                    state =
                        ProjectEditorUiState(
                            colors =
                                listOf(
                                    ColorEntity(
                                        id = "moss",
                                        name = "Moss Green",
                                        argb = 0xFF6B7A2C,
                                        builtIn = false,
                                        createdAt = 0,
                                        updatedAt = 0,
                                    ),
                                ),
                            isLoading = false,
                        ),
                    isEditing = false,
                )
            }
        }

        composeRule.onNodeWithText("Moss Green").assertExists()
    }
}
