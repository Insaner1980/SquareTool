package com.finnvek.squaretool.ui.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.squaretool.data.repository.AppSettings
import com.finnvek.squaretool.data.repository.ThemePreference
import com.finnvek.squaretool.ui.theme.SquareToolTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun choosingDarkTheme_dispatchesPreference() {
        var selected: ThemePreference? = null
        composeRule.setContent {
            SquareToolTheme {
                SettingsScreen(
                    state = SettingsUiState(settings = AppSettings()),
                    onThemeChange = { selected = it },
                )
            }
        }

        composeRule.onNodeWithTag("theme_dark").performClick()
        composeRule.runOnIdle { assertEquals(ThemePreference.DARK, selected) }
    }

    @Test
    fun privacySection_explainsOfflineStorage() {
        composeRule.setContent {
            SquareToolTheme { SettingsScreen(state = SettingsUiState(settings = AppSettings())) }
        }

        composeRule.onNodeWithTag("settings_list").performScrollToIndex(16)
        composeRule.onNodeWithText("SquareTool works completely offline.").assertExists()
        composeRule.onNodeWithText("Data stays on this device unless you explicitly export or share it.").assertExists()
    }
}
