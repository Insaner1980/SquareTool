package com.finnvek.squaretool.ui.squares

import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.SquareDesignEntity
import com.finnvek.squaretool.ui.moveListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SquaresModelsTest {
    private val now = 123L
    private val olive = color("olive", "Olive", 0xFF6B8A2E)
    private val cream = color("cream", "Cream", 0xFFF3E6C9)
    private val rust = color("rust", "Rust", 0xFFD75A1F)

    @Test
    fun searchMatchesNameNotesAndCategoryCaseInsensitively() {
        val designs =
            listOf(
                item("daisy", "Soft Daisy", "summer gift", "Floral", builtIn = true),
                item("diamond", "Layered Shape", "clean lines", "Geometric", builtIn = false),
            )

        assertEquals(listOf("daisy"), filterSquareDesigns(designs, "dAiSy", SquareFilter.ALL).map { it.id })
        assertEquals(listOf("daisy"), filterSquareDesigns(designs, "SUMMER", SquareFilter.ALL).map { it.id })
        assertEquals(listOf("diamond"), filterSquareDesigns(designs, "geometric", SquareFilter.ALL).map { it.id })
    }

    @Test
    fun filtersFavoritesCategoriesAndCustomDesigns() {
        val designs =
            listOf(
                item("favorite", "Favorite", "", "Floral", favorite = true, builtIn = true),
                item("simple", "Simple", "", "Simple", builtIn = true),
                item("custom", "My square", "", "Geometric", builtIn = false),
            )

        assertEquals(listOf("favorite"), filterSquareDesigns(designs, "", SquareFilter.FAVORITES).map { it.id })
        assertEquals(listOf("simple"), filterSquareDesigns(designs, "", SquareFilter.SIMPLE).map { it.id })
        assertEquals(listOf("custom"), filterSquareDesigns(designs, "", SquareFilter.CUSTOM).map { it.id })
    }

    @Test
    fun templateChangeRequestsConfirmationBeforeTruncatingRounds() {
        val draft =
            validDraft(
                templateId = "classic_granny",
                roundColorIds = listOf("olive", "cream", "rust", "olive", "cream", "rust"),
            )

        val change = draft.planTemplateChange("daisy")

        assertTrue(change.requiresRoundTruncationConfirmation)
        assertEquals(5, change.updatedDraft.roundColorIds.size)
        assertEquals("daisy", change.updatedDraft.templateId)
    }

    @Test
    fun draftAllowsRepeatedColorsAndRejectsUnsupportedRoundCount() {
        val repeated = validDraft(roundColorIds = listOf("olive", "olive", "olive"))
        assertTrue(repeated.validationErrors().isEmpty())

        val tooShort = repeated.copy(roundColorIds = listOf("olive", "olive"))
        assertTrue(SquareDraftError.ROUND_COUNT in tooShort.validationErrors())
    }

    @Test
    fun reorderMovesOneRoundWithoutDroppingDuplicates() {
        val reordered = moveListItem(listOf("olive", "cream", "olive", "rust"), fromIndex = 3, toIndex = 1)

        assertEquals(listOf("olive", "rust", "cream", "olive"), reordered)
    }

    @Test
    fun builtInDesignIsNotEditableButCanBeDuplicated() {
        val item = item("built-in", "Built in", "", "Floral", builtIn = true)

        assertFalse(item.canEdit)
        assertTrue(item.canDuplicate)
    }

    private fun validDraft(
        templateId: String = "classic_granny",
        roundColorIds: List<String> = listOf("olive", "cream", "rust"),
    ) = SquareEditorDraft(
        id = "draft",
        name = "Olive bloom",
        templateId = templateId,
        roundColorIds = roundColorIds,
        notes = "",
        favorite = false,
        sourceBuiltIn = false,
    )

    private fun item(
        id: String,
        name: String,
        note: String,
        category: String,
        favorite: Boolean = false,
        builtIn: Boolean,
    ) = SquareDesignListItem(
        design =
            SquareDesignEntity(
                id = id,
                name = name,
                motifTemplateId = "classic_granny",
                note = note,
                favorite = favorite,
                builtIn = builtIn,
                category = category,
                gramsPerSquareOverride = null,
                createdAt = now,
                updatedAt = now,
            ),
        roundColors = listOf(olive, cream, rust),
    )

    private fun color(
        id: String,
        name: String,
        argb: Long,
    ) = ColorEntity(
        id = id,
        name = name,
        argb = argb,
        builtIn = true,
        createdAt = now,
        updatedAt = now,
    )
}
