package com.finnvek.squaretool.ui.projects

import com.finnvek.squaretool.data.local.ProjectCellEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.repository.AppSettings
import com.finnvek.squaretool.data.repository.MeasurementUnitPreference
import com.finnvek.squaretool.data.repository.ProjectCardData
import com.finnvek.squaretool.domain.model.MeasurementUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ProjectModelsTest {
    @Test
    fun `card snapshot ignores cells outside current resized bounds`() {
        val resizedProject =
            project("p", "Resized", updatedAt = 1).copy(
                rowCount = 1,
                columnCount = 1,
                trackingEnabled = true,
            )

        val card =
            buildProjectCardModels(
                ProjectCardData(
                    projects = listOf(resizedProject),
                    cells = listOf(cell(0, 0, null), cell(1, 0, null)),
                    designs = emptyList(),
                    colors = emptyList(),
                    projectPaletteRefs = emptyList(),
                ),
            ).single()

        assertEquals(listOf(0 to 0), card.cells.map { it.rowIndex to it.columnIndex })
    }

    @Test
    fun `preview hides stale completed state when tracking is disabled`() {
        assertTrue(shouldRenderCompletedOverlay(trackingEnabled = true, cellCompleted = true))
        assertTrue(!shouldRenderCompletedOverlay(trackingEnabled = false, cellCompleted = true))
        assertTrue(!shouldRenderCompletedOverlay(trackingEnabled = true, cellCompleted = false))
    }

    @Test
    fun `new project draft uses explicit AppSettings defaults`() {
        val draft =
            initialProjectEditorDraft(
                project = null,
                selectedColorIds = emptySet(),
                settings =
                    AppSettings(
                        preferredMeasurementUnit = MeasurementUnitPreference.INCHES,
                        defaultJoiningAndEdgingBufferPercent = 17.5,
                        defaultSkeinWeightGrams = 125.0,
                    ),
                newProjectId = "new",
                locale = Locale.FRANCE,
            )

        assertEquals(MeasurementUnit.INCHES, draft.measurementUnit)
        assertEquals("17.5", draft.bufferPercent)
        assertEquals("125.0", draft.skeinWeightGrams)
    }

    @Test
    fun `automatic measurement unit follows US locale and otherwise defaults to centimeters`() {
        val automatic = AppSettings(preferredMeasurementUnit = MeasurementUnitPreference.AUTOMATIC)

        assertEquals(
            MeasurementUnit.INCHES,
            initialProjectEditorDraft(null, emptySet(), automatic, "us", Locale.US).measurementUnit,
        )
        assertEquals(
            MeasurementUnit.CENTIMETERS,
            initialProjectEditorDraft(
                null,
                emptySet(),
                automatic,
                "fi",
                Locale.forLanguageTag("fi-FI"),
            ).measurementUnit,
        )
    }

    @Test
    fun `existing project values override AppSettings defaults`() {
        val existing =
            project("saved", "Saved", updatedAt = 1).copy(
                measurementUnit = "inches",
                skeinWeightGrams = 50.0,
                joiningAndEdgingBufferPercent = 4.0,
            )
        val settings =
            AppSettings(
                preferredMeasurementUnit = MeasurementUnitPreference.CENTIMETERS,
                defaultJoiningAndEdgingBufferPercent = 20.0,
                defaultSkeinWeightGrams = 200.0,
            )

        val draft = initialProjectEditorDraft(existing, setOf("cream"), settings, "ignored", Locale.FRANCE)

        assertEquals(MeasurementUnit.INCHES, draft.measurementUnit)
        assertEquals("4.0", draft.bufferPercent)
        assertEquals("50.0", draft.skeinWeightGrams)
        assertEquals("saved", draft.id)
    }

    @Test
    fun `project list filters favorites and sorts alphabetically`() {
        val cards =
            listOf(
                ProjectCardModel(project("z", "Zinnia", favorite = true, updatedAt = 30)),
                ProjectCardModel(project("a", "Autumn", favorite = true, updatedAt = 10)),
                ProjectCardModel(project("b", "Baby", favorite = false, updatedAt = 40)),
            )

        val result =
            filterAndSortProjectCards(
                cards = cards,
                query = "",
                favoriteOnly = true,
                sort = ProjectSort.ALPHABETICAL,
            )

        assertEquals(listOf("a", "z"), result.map { it.project.id })
    }

    @Test
    fun `project list search includes notes and recent sort uses updated time`() {
        val cards =
            listOf(
                ProjectCardModel(project("old", "Old", notes = "gift", updatedAt = 10)),
                ProjectCardModel(project("new", "Gift blanket", updatedAt = 40)),
                ProjectCardModel(project("other", "Other", updatedAt = 50)),
            )

        val result = filterAndSortProjectCards(cards, "GIFT", false, ProjectSort.RECENT)

        assertEquals(listOf("new", "old"), result.map { it.project.id })
    }

    @Test
    fun `editor validates required name grid and positive optional measurements`() {
        val errors =
            ProjectEditorDraft(
                id = "new",
                name = " ",
                rows = 0,
                columns = 51,
                measurementUnit = MeasurementUnit.CENTIMETERS,
                squareWidth = "-1",
                squareHeight = "zero",
                joiningGap = "-0.1",
                trackingEnabled = true,
                selectedColorIds = emptySet(),
                initialFill = InitialProjectFill.BLANK,
                selectedDesignIds = emptySet(),
                globalGramsPerSquare = "0",
                skeinWeightGrams = "-50",
                bufferPercent = "101",
                notes = "",
            ).validationErrors()

        assertEquals(
            setOf(
                ProjectDraftError.NAME,
                ProjectDraftError.ROWS,
                ProjectDraftError.COLUMNS,
                ProjectDraftError.SQUARE_WIDTH,
                ProjectDraftError.SQUARE_HEIGHT,
                ProjectDraftError.JOINING_GAP,
                ProjectDraftError.GRAMS_PER_SQUARE,
                ProjectDraftError.SKEIN_WEIGHT,
                ProjectDraftError.BUFFER_PERCENT,
            ),
            errors,
        )
    }

    @Test
    fun `fill one requires one design and balanced assignment differs by at most one`() {
        val fillOne = validDraft().copy(initialFill = InitialProjectFill.FILL_ONE)
        assertTrue(ProjectDraftError.DESIGNS in fillOne.validationErrors())

        val balanced =
            validDraft().copy(
                rows = 2,
                columns = 4,
                initialFill = InitialProjectFill.BALANCED,
                selectedDesignIds = linkedSetOf("sun", "daisy", "star"),
            )

        val assignments = balanced.initialAssignments()
        val counts =
            assignments
                .filterNotNull()
                .groupingBy { it }
                .eachCount()
                .values
        assertEquals(8, assignments.size)
        assertTrue(counts.max() - counts.min() <= 1)
    }

    @Test
    fun `shrinking reports every lost cell and assigned lost cells separately`() {
        val cells =
            listOf(
                cell(0, 0, "a"),
                cell(0, 1, null),
                cell(1, 0, "b"),
                cell(1, 1, null),
                cell(2, 0, "c"),
                cell(2, 1, null),
            )

        val impact = calculateShrinkImpact(cells, newRows = 2, newColumns = 1)

        assertEquals(4, impact.lostCellCount)
        assertEquals(1, impact.lostAssignedCellCount)
    }

    private fun validDraft() =
        ProjectEditorDraft(
            id = "new",
            name = "Blanket",
            rows = 2,
            columns = 3,
            measurementUnit = MeasurementUnit.INCHES,
            squareWidth = "8",
            squareHeight = "",
            joiningGap = "0",
            trackingEnabled = true,
            selectedColorIds = setOf("cream"),
            initialFill = InitialProjectFill.BLANK,
            selectedDesignIds = emptySet(),
            globalGramsPerSquare = "20",
            skeinWeightGrams = "100",
            bufferPercent = "10",
            notes = "",
        )

    private fun cell(
        row: Int,
        column: Int,
        designId: String?,
    ) = ProjectCellEntity(
        projectId = "p",
        rowIndex = row,
        columnIndex = column,
        squareDesignId = designId,
        locked = false,
        completed = false,
    )

    private fun project(
        id: String,
        name: String,
        favorite: Boolean = false,
        notes: String = "",
        updatedAt: Long,
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
        updatedAt = updatedAt,
        lastOpenedAt = updatedAt,
        generationSeed = 1,
        defaultSquareDesignId = null,
        globalGramsPerSquare = null,
        skeinWeightGrams = null,
        joiningAndEdgingBufferPercent = 10.0,
        demoProject = false,
    )
}
