package com.finnvek.squaretool.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.squaretool.ui.theme.SquareToolTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun next_reachesFinalOnboardingChoice() {
        composeRule.setContent {
            SquareToolTheme {
                OnboardingScreen(
                    onCreateProject = {},
                    onExploreSample = {},
                )
            }
        }

        composeRule.onNodeWithText("Design your squares").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_next").performClick()
        composeRule.onNodeWithText("Build your blanket").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_next").performClick()
        composeRule.onNodeWithText("Keep a clear project plan").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_next").performClick()
        composeRule.onNodeWithTag("create_project_choice").assertIsDisplayed()
        composeRule.onNodeWithTag("sample_project_choice").assertIsDisplayed()
    }
}
