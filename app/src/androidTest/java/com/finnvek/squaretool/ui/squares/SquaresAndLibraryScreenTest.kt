package com.finnvek.squaretool.ui.squares

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.PaletteEntity
import com.finnvek.squaretool.data.local.SquareDesignEntity
import com.finnvek.squaretool.ui.library.LibraryScreen
import com.finnvek.squaretool.ui.library.LibraryTab
import com.finnvek.squaretool.ui.library.LibraryUiState
import com.finnvek.squaretool.ui.library.PaletteListItem
import com.finnvek.squaretool.ui.theme.SquareToolTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class SquaresAndLibraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val color =
        ColorEntity(
            id = "olive",
            name = "Olive",
            argb = 0xFF6B8A2E,
            builtIn = true,
            createdAt = 1,
            updatedAt = 1,
        )
    private val design =
        SquareDesignListItem(
            design =
                SquareDesignEntity(
                    id = "olive-bloom",
                    name = "Olive Bloom",
                    motifTemplateId = "classic_granny",
                    note = "Garden",
                    favorite = true,
                    builtIn = false,
                    category = "Floral",
                    gramsPerSquareOverride = null,
                    createdAt = 1,
                    updatedAt = 1,
                ),
            roundColors = listOf(color, color, color),
        )

    @Test
    fun squaresSearchFiltersAndShowsEmptyResult() {
        var query by mutableStateOf("")
        composeRule.setContent {
            SquareToolTheme {
                SquaresScreen(
                    state =
                        SquaresUiState(
                            query = query,
                            designs = filterSquareDesigns(listOf(design), query, SquareFilter.ALL),
                            isLoading = false,
                        ),
                    onQueryChange = { query = it },
                    onFilterChange = {},
                    onSelectDesign = {},
                    onFavorite = {},
                    onCreateDesign = {},
                    onEditDesign = { _, _ -> },
                    onUseInProject = {},
                    onDeleteDesign = {},
                    onNoticeShown = {},
                )
            }
        }

        composeRule.onNodeWithTag("square_card_olive-bloom").assertIsDisplayed()
        composeRule.onNodeWithTag("squares_search").performTextInput("missing")
        composeRule.onAllNodesWithTag("square_card_olive-bloom").assertCountEquals(0)
        composeRule.onNodeWithText("No matching squares").assertIsDisplayed()
    }

    @Test
    fun squareEditorExposesSaveAction() {
        val saved = AtomicBoolean(false)
        composeRule.setContent {
            SquareToolTheme {
                SquareEditorScreen(
                    state =
                        SquareEditorUiState(
                            draft =
                                SquareEditorDraft(
                                    id = "new",
                                    name = "New square",
                                    templateId = "classic_granny",
                                    roundColorIds = listOf("olive", "olive", "olive"),
                                    notes = "",
                                    favorite = false,
                                    sourceBuiltIn = false,
                                ),
                            colors = listOf(color),
                            isLoading = false,
                        ),
                    isNew = true,
                    isDuplicate = false,
                    onNameChange = {},
                    onNotesChange = {},
                    onFavoriteChange = {},
                    onTemplateSelected = {},
                    onAssignColor = { _, _ -> },
                    onAddRound = {},
                    onRemoveRound = {},
                    onMoveRound = { _, _ -> },
                    onCreateColor = { _, _, _ -> },
                    onSave = { saved.set(true) },
                    onCancel = {},
                    onConfirmTemplate = {},
                    onCancelTemplate = {},
                )
            }
        }

        composeRule.onNodeWithTag("save_square").performScrollTo().performClick()
        assertTrue(saved.get())
    }

    @Test
    fun libraryTabsSwitchBetweenRealColorAndPaletteContent() {
        val palette =
            PaletteListItem(
                PaletteEntity("garden", "Garden", false, 1, 1),
                listOf(color),
            )
        var selectedTab by mutableStateOf(LibraryTab.COLORS)
        composeRule.setContent {
            SquareToolTheme {
                LibraryScreen(
                    state =
                        LibraryUiState(
                            selectedTab = selectedTab,
                            colors = listOf(color),
                            palettes = listOf(palette),
                            isLoading = false,
                        ),
                    projectId = null,
                    onTabSelected = { selectedTab = it },
                    onQueryChange = {},
                    onCreateColor = {},
                    onEditColor = { _, _ -> },
                    onDeleteColor = {},
                    onCreatePalette = {},
                    onEditPalette = { _, _ -> },
                    onDeletePalette = {},
                    onApplyPalette = { _, _ -> },
                    onSaveProjectPalette = { _, _ -> },
                    onNoticeShown = {},
                )
            }
        }

        composeRule.onNodeWithTag("color_card_olive").assertIsDisplayed()
        composeRule.onNodeWithTag("library_tab_palettes").performClick()
        composeRule.onNodeWithTag("palette_card_garden").assertIsDisplayed()
    }
}
