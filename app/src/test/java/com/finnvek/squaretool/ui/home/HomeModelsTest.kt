package com.finnvek.squaretool.ui.home

import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.ui.projects.ProjectCardModel
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeModelsTest {
    @Test
    fun `most recently opened project is current and only two other recent projects are shown`() {
        val cards =
            listOf(
                card("favorite-old", "Favorite old", openedAt = 10, favorite = true),
                card("current", "Current", openedAt = 50),
                card("recent-two", "Recent two", openedAt = 30),
                card("recent-one", "Recent one", openedAt = 40),
            )

        val selection = selectHomeProjects(cards)

        assertEquals("current", selection.current?.project?.id)
        assertEquals(listOf("recent-one", "recent-two"), selection.recent.map { it.project.id })
    }

    @Test
    fun `home search matches project notes without regard to case`() {
        val cards =
            listOf(
                card("garden", "Garden blanket", openedAt = 20, notes = "A wedding gift"),
                card("baby", "Baby blanket", openedAt = 10, notes = "Soft cotton"),
            )

        assertEquals(
            listOf("garden"),
            searchHomeProjects(cards, "WEDDING").map { it.project.id },
        )
    }

    private fun card(
        id: String,
        name: String,
        openedAt: Long,
        favorite: Boolean = false,
        notes: String = "",
    ) = ProjectCardModel(project(id, name, openedAt, favorite, notes))

    // CPD-OFF
    private fun project(
        id: String,
        name: String,
        openedAt: Long,
        favorite: Boolean,
        notes: String,
    ) = ProjectEntity(
        id = id,
        name = name,
        rowCount = 2,
        columnCount = 3,
        squareWidthValue = null,
        squareHeightValue = null,
        measurementUnit = "centimeters",
        joiningGapValue = null,
        trackingEnabled = false,
        favorite = favorite,
        notes = notes,
        createdAt = 1,
        updatedAt = openedAt,
        lastOpenedAt = openedAt,
        generationSeed = 1,
        defaultSquareDesignId = null,
        globalGramsPerSquare = null,
        skeinWeightGrams = null,
        joiningAndEdgingBufferPercent = 10.0,
        demoProject = false,
    )
    // CPD-ON
}
