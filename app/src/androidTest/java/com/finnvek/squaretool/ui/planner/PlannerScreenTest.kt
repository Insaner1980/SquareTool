package com.finnvek.squaretool.ui.planner

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.local.SquareDesignEntity
import com.finnvek.squaretool.data.local.SquareRoundEntity
import com.finnvek.squaretool.data.local.SquareToolDatabase
import com.finnvek.squaretool.data.repository.SquareToolRepository
import com.finnvek.squaretool.domain.model.CellCoordinate
import com.finnvek.squaretool.render.MotifRenderDetail
import com.finnvek.squaretool.render.SquareDesignVisual
import com.finnvek.squaretool.ui.theme.SquareToolTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun accessibleGrid_announcesStateAndSelectsCell() {
        var selected: CellCoordinate? = null
        composeRule.setContent {
            SquareToolTheme {
                AccessiblePlannerGrid(
                    rows = 1,
                    columns = 1,
                    cells =
                        listOf(
                            PlannerUiCell(
                                coordinate = CellCoordinate(0, 0),
                                designId = "olive",
                                designName = "Olive Bloom",
                                visual =
                                    SquareDesignVisual(
                                        templateId = "classic_granny",
                                        roundColors =
                                            listOf(
                                                0xFF526A1D.toInt(),
                                                0xFFF3E6C9.toInt(),
                                                0xFFD75A1F.toInt(),
                                            ),
                                    ),
                                locked = true,
                                completed = false,
                            ),
                        ),
                    selectedCoordinate = null,
                    onSelectCell = { selected = it },
                )
            }
        }

        composeRule
            .onNodeWithTag("planner_cell_0_0")
            .assertContentDescriptionEquals(
                "Row 1, column 1. Olive Bloom. Locked. Not completed.",
            ).performClick()

        composeRule.runOnIdle {
            assertEquals(CellCoordinate(0, 0), selected)
        }
    }

    @Test
    fun progressRoundsToNearestWholePercent() {
        val cells =
            List(96) { index ->
                PlannerUiCell(
                    coordinate = CellCoordinate(index / 12, index % 12),
                    designId = null,
                    designName = null,
                    visual = null,
                    locked = false,
                    completed = index < 69,
                )
            }

        val state =
            PlannerUiState(
                isLoading = false,
                rows = 8,
                columns = 12,
                trackingEnabled = true,
                cells = cells,
            )

        assertEquals(72, state.completionPercent)
    }

    @Test
    fun canvasUsesSimplifiedMotifsBelowSmallPreviewThreshold() {
        assertEquals(
            MotifRenderDetail.SMALL,
            resolvePlannerMotifDetail(
                renderedCellSizePx = 23.9f,
                smallPreviewThresholdPx = 24f,
            ),
        )
        assertEquals(
            MotifRenderDetail.FULL,
            resolvePlannerMotifDetail(
                renderedCellSizePx = 24f,
                smallPreviewThresholdPx = 24f,
            ),
        )
    }

    @Test
    fun dragPaintPersistsAndUndoRevertsWholeGesture() =
        runBlocking {
            val database =
                Room
                    .inMemoryDatabaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        SquareToolDatabase::class.java,
                    ).build()
            val repository = SquareToolRepository(database)
            val now = 1_000L
            listOf(
                "cream" to 0xFFF3E6C9,
                "olive" to 0xFF526A1D,
                "orange" to 0xFFD75A1F,
            ).forEach { (id, argb) ->
                repository.saveColor(
                    ColorEntity(
                        id = id,
                        name = id,
                        argb = argb,
                        builtIn = false,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
            repository.saveDesign(
                design =
                    SquareDesignEntity(
                        id = "olive-bloom",
                        name = "Olive Bloom",
                        motifTemplateId = "classic_granny",
                        note = "",
                        favorite = false,
                        builtIn = false,
                        category = "floral",
                        gramsPerSquareOverride = null,
                        createdAt = now,
                        updatedAt = now,
                    ),
                rounds =
                    listOf("cream", "orange", "olive").mapIndexed { index, colorId ->
                        SquareRoundEntity("olive-bloom", index, colorId)
                    },
            )
            repository.createProject(
                ProjectEntity(
                    id = "project",
                    name = "Test blanket",
                    rowCount = 1,
                    columnCount = 2,
                    squareWidthValue = null,
                    squareHeightValue = null,
                    measurementUnit = "cm",
                    joiningGapValue = null,
                    trackingEnabled = true,
                    favorite = false,
                    notes = "",
                    createdAt = now,
                    updatedAt = now,
                    lastOpenedAt = now,
                    generationSeed = 42L,
                    defaultSquareDesignId = null,
                    globalGramsPerSquare = null,
                    skeinWeightGrams = null,
                    joiningAndEdgingBufferPercent = 10.0,
                    demoProject = false,
                ),
            )
            val viewModel =
                PlannerViewModel(
                    repository = repository,
                    savedStateHandle = SavedStateHandle(mapOf(PlannerViewModel.PROJECT_ID_KEY to "project")),
                    generationDispatcher = Dispatchers.Default,
                )
            val viewModelStore = ViewModelStore().apply { put("planner", viewModel) }

            withTimeout(5_000) { viewModel.state.first { !it.isLoading } }
            viewModel.selectDesign("olive-bloom")
            viewModel.beginPaintDrag()
            viewModel.paintDuringDrag(CellCoordinate(0, 0))
            viewModel.paintDuringDrag(CellCoordinate(0, 1))
            viewModel.endPaintDrag()

            withTimeout(5_000) {
                repository.observeGrid("project").filterNotNull().first { snapshot ->
                    snapshot.cells.all { it.designId == "olive-bloom" }
                }
            }
            assertEquals(true, viewModel.state.value.canUndo)

            viewModel.undo()
            withTimeout(5_000) {
                repository.observeGrid("project").filterNotNull().first { snapshot ->
                    snapshot.cells.all { it.designId == null }
                }
            }
            viewModelStore.clear()
            database.close()
        }
}
