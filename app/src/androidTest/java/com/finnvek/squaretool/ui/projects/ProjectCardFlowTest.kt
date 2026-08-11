package com.finnvek.squaretool.ui.projects

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.ProjectCellEntity
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.local.SquareDesignEntity
import com.finnvek.squaretool.data.local.SquareRoundEntity
import com.finnvek.squaretool.data.local.SquareToolDatabase
import com.finnvek.squaretool.data.repository.SquareToolRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectCardFlowTest {
    @Test
    fun cardFlowReemitsForEveryTableThatChangesVisibleCardData() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val database =
                Room
                    .inMemoryDatabaseBuilder(context, SquareToolDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
            val repository = SquareToolRepository(database)
            val emissions = Channel<List<ProjectCardModel>>(Channel.UNLIMITED)
            var collector: Job? = null

            try {
                repository.saveColor(color("cream", 0xFFF3E6C9))
                repository.saveColor(color("moss", 0xFF6B7A2C))
                repository.saveDesign(design("Original"), rounds("cream"))
                repository.createProject(project())
                repository.saveCell(ProjectCellEntity("project", 0, 0, "design", false, false))
                repository.setProjectPalette("project", listOf("cream"))

                collector =
                    launch(Dispatchers.IO) {
                        repository
                            .observeProjectCardData()
                            .map(::buildProjectCardModels)
                            .collect(emissions::send)
                    }

                receiveUntil(emissions) { cards -> cards.singleOrNull()?.project?.id == "project" }

                repository.updateProject(project().copy(name = "Updated project"))
                assertEquals(
                    "Updated project",
                    receiveUntil(emissions) { it.single().project.name == "Updated project" }
                        .single()
                        .project.name,
                )

                repository.saveCell(ProjectCellEntity("project", 0, 0, "design", false, true))
                assertEquals(100, receiveUntil(emissions) { it.single().progress?.percentage == 100 }.single().progress?.percentage)

                repository.saveDesign(design("Renamed"), rounds("cream"))
                assertEquals(
                    "Renamed",
                    receiveUntil(emissions) { it.single().designs["design"]?.name == "Renamed" }
                        .single()
                        .designs
                        .getValue("design")
                        .name,
                )

                database.squareDesignDao().deleteRounds("design")
                database.squareDesignDao().insertRounds(rounds("moss"))
                assertEquals(
                    0xFF6B7A2C.toInt(),
                    receiveUntil(emissions) {
                        it
                            .single()
                            .designs["design"]
                            ?.roundColors
                            ?.firstOrNull() == 0xFF6B7A2C.toInt()
                    }.single().designs.getValue("design").roundColors.first(),
                )

                repository.saveColor(color("moss", 0xFF123456))
                assertEquals(
                    0xFF123456.toInt(),
                    receiveUntil(emissions) {
                        it
                            .single()
                            .designs["design"]
                            ?.roundColors
                            ?.firstOrNull() == 0xFF123456.toInt()
                    }.single().designs.getValue("design").roundColors.first(),
                )

                repository.setProjectPalette("project", listOf("moss"))
                assertEquals(
                    listOf("moss"),
                    receiveUntil(emissions) { it.single().palette.map(ColorEntity::id) == listOf("moss") }
                        .single()
                        .palette
                        .map(ColorEntity::id),
                )

                repository.updateProject(project().copy(rowCount = 2, columnCount = 2, name = "Expanded"))
                receiveUntil(emissions) { it.single().totalSquares == 4 && it.single().cells.size == 4 }
                repository.updateProject(project().copy(name = "Shrunk"))
                val shrunk =
                    receiveUntil(emissions) {
                        it.single().project.name == "Shrunk" &&
                            it.single().totalSquares == 1 &&
                            it.single().cells.size == 1
                    }.single()
                assertEquals(listOf(0 to 0), shrunk.cells.map { it.rowIndex to it.columnIndex })
            } finally {
                collector?.cancelAndJoin()
                database.close()
            }
        }

    private suspend fun receiveUntil(
        channel: Channel<List<ProjectCardModel>>,
        predicate: (List<ProjectCardModel>) -> Boolean,
    ): List<ProjectCardModel> =
        withTimeout(5_000) {
            var value = channel.receive()
            while (!predicate(value)) value = channel.receive()
            value
        }

    private fun project() =
        ProjectEntity(
            id = "project",
            name = "Blanket",
            rowCount = 1,
            columnCount = 1,
            squareWidthValue = null,
            squareHeightValue = null,
            measurementUnit = "centimeters",
            joiningGapValue = null,
            trackingEnabled = true,
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

    private fun design(name: String) =
        SquareDesignEntity(
            id = "design",
            name = name,
            motifTemplateId = "classic_granny",
            note = "",
            favorite = false,
            builtIn = false,
            category = "Classic",
            gramsPerSquareOverride = null,
            createdAt = 1,
            updatedAt = 1,
        )

    private fun rounds(colorId: String) =
        List(3) { index ->
            SquareRoundEntity("design", index, colorId)
        }

    private fun color(
        id: String,
        argb: Long,
    ) = ColorEntity(
        id = id,
        name = id,
        argb = argb,
        builtIn = false,
        createdAt = 1,
        updatedAt = 1,
    )
}
