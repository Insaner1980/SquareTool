package com.finnvek.squaretool.ui.navigation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.squaretool.ui.theme.SquareToolTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SquareToolNavigationBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navigationBar_hasAllFiveDestinations() {
        composeRule.setContent {
            SquareToolTheme {
                SquareToolNavigationBar(
                    selected = TopLevelDestination.Home,
                    onSelect = {},
                )
            }
        }

        composeRule.onAllNodesWithTag("top_level_destination").assertCountEquals(5)
        listOf("Home", "Planner", "Squares", "Library", "Settings").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
    }
}
