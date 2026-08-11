package com.finnvek.squaretool.domain

import com.finnvek.squaretool.domain.algorithm.GenerationRequest
import com.finnvek.squaretool.domain.algorithm.LayoutGenerator
import com.finnvek.squaretool.domain.algorithm.LayoutMode
import com.finnvek.squaretool.domain.algorithm.WeightedDesign
import com.finnvek.squaretool.domain.model.CellCoordinate
import com.finnvek.squaretool.domain.model.CellState
import com.finnvek.squaretool.domain.model.GradientDirection
import com.finnvek.squaretool.domain.model.MirrorDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutGeneratorTest {
    private val designs = listOf(WeightedDesign("A"), WeightedDesign("B"), WeightedDesign("C"))

    @Test
    fun `same random seed and configuration produce identical arrangement`() {
        val request =
            GenerationRequest(
                snapshot = grid(8, 12),
                designs = designs,
                mode = LayoutMode.Random,
                seed = 4_291L,
            )

        val first = LayoutGenerator.generate(request)
        val second = LayoutGenerator.generate(request)

        assertEquals(first.snapshot, second.snapshot)
        assertEquals(first.designCounts, second.designCounts)
    }

    @Test
    fun `different random seed can produce a different arrangement`() {
        val snapshot = grid(8, 12)

        val first =
            LayoutGenerator.generate(
                GenerationRequest(snapshot, designs, LayoutMode.Random, seed = 1L),
            )
        val second =
            LayoutGenerator.generate(
                GenerationRequest(snapshot, designs, LayoutMode.Random, seed = 2L),
            )

        assertNotEquals(first.snapshot.designsInRowMajorOrder(), second.snapshot.designsInRowMajorOrder())
    }

    @Test
    fun `equal balanced counts differ by no more than one`() {
        val result =
            LayoutGenerator.generate(
                GenerationRequest(
                    snapshot = grid(5, 5),
                    designs = designs,
                    mode = LayoutMode.BalancedRandom,
                    seed = 42L,
                ),
            )

        val counts = designs.map { result.designCounts.getValue(it.designId) }
        assertEquals(25, counts.sum())
        assertTrue(counts.max() - counts.min() <= 1)
    }

    @Test
    fun `weighted balanced counts match requested proportions`() {
        val weighted = listOf(WeightedDesign("A", 1.0), WeightedDesign("B", 3.0))

        val result =
            LayoutGenerator.generate(
                GenerationRequest(
                    snapshot = grid(10, 10),
                    designs = weighted,
                    mode = LayoutMode.BalancedRandom,
                    seed = 99L,
                ),
            )

        assertEquals(25, result.designCounts.getValue("A"))
        assertEquals(75, result.designCounts.getValue("B"))
    }

    @Test
    fun `locked cells remain unchanged in every filling generator`() {
        val original =
            grid(4, 4) { row, column ->
                val locked = row == 1 && column == 2
                CellState(
                    CellCoordinate(row, column),
                    designId = if (locked) "LOCKED" else null,
                    locked = locked,
                )
            }
        val modes =
            listOf(
                LayoutMode.Random,
                LayoutMode.BalancedRandom,
                LayoutMode.Checker,
                LayoutMode.AlternatingRows,
                LayoutMode.AlternatingColumns,
                LayoutMode.Diagonal,
                LayoutMode.HorizontalStripes(2),
                LayoutMode.VerticalStripes(2),
                LayoutMode.Gradient(GradientDirection.LEFT_TO_RIGHT),
                LayoutMode.Radial,
            )

        modes.forEach { mode ->
            val result =
                LayoutGenerator.generate(
                    GenerationRequest(original, designs.take(2), mode, seed = 7L),
                )
            assertEquals("$mode changed locked cell", "LOCKED", result.snapshot[CellCoordinate(1, 2)].designId)
        }
    }

    @Test
    fun `completed cells are preserved by default`() {
        val original =
            grid(2, 2) { row, column ->
                val protected = row == 0 && column == 1
                CellState(
                    CellCoordinate(row, column),
                    designId = if (protected) "DONE" else null,
                    completed = protected,
                )
            }

        val result =
            LayoutGenerator.generate(
                GenerationRequest(original, designs, LayoutMode.Random, seed = 1L),
            )

        assertEquals("DONE", result.snapshot[CellCoordinate(0, 1)].designId)
        assertTrue(result.snapshot[CellCoordinate(0, 1)].completed)
    }

    @Test
    fun `completed cells may be overwritten only when explicitly enabled`() {
        val original =
            grid(1, 1) { _, _ ->
                CellState(CellCoordinate(0, 0), designId = "DONE", completed = true)
            }

        val result =
            LayoutGenerator.generate(
                GenerationRequest(
                    original,
                    listOf(WeightedDesign("A")),
                    LayoutMode.Random,
                    seed = 1L,
                    overwriteCompleted = true,
                ),
            )

        assertEquals("A", result.snapshot[CellCoordinate(0, 0)].designId)
        assertTrue(result.snapshot[CellCoordinate(0, 0)].completed)
    }

    @Test
    fun `checker alternates exactly two designs by row plus column parity`() {
        val result =
            LayoutGenerator.generate(
                GenerationRequest(grid(3, 4), designs.take(2), LayoutMode.Checker, seed = 5L),
            )

        assertEquals(
            listOf(
                "A",
                "B",
                "A",
                "B",
                "B",
                "A",
                "B",
                "A",
                "A",
                "B",
                "A",
                "B",
            ),
            result.snapshot.designsInRowMajorOrder(),
        )
    }

    @Test
    fun `alternating rows cycles ordered designs by row`() {
        val result =
            LayoutGenerator.generate(
                GenerationRequest(grid(4, 2), designs, LayoutMode.AlternatingRows, seed = 5L),
            )

        assertEquals(
            listOf("A", "A", "B", "B", "C", "C", "A", "A"),
            result.snapshot.designsInRowMajorOrder(),
        )
    }

    @Test
    fun `alternating columns cycles ordered designs by column`() {
        val result =
            LayoutGenerator.generate(
                GenerationRequest(grid(2, 4), designs, LayoutMode.AlternatingColumns, seed = 5L),
            )

        assertEquals(
            listOf("A", "B", "C", "A", "A", "B", "C", "A"),
            result.snapshot.designsInRowMajorOrder(),
        )
    }

    @Test
    fun `diagonal cycles ordered designs by row plus column`() {
        val result =
            LayoutGenerator.generate(
                GenerationRequest(grid(3, 3), designs, LayoutMode.Diagonal, seed = 5L),
            )

        assertEquals(
            listOf("A", "B", "C", "B", "C", "A", "C", "A", "B"),
            result.snapshot.designsInRowMajorOrder(),
        )
    }

    @Test
    fun `horizontal stripes honor band width`() {
        val result =
            LayoutGenerator.generate(
                GenerationRequest(grid(5, 2), designs.take(2), LayoutMode.HorizontalStripes(2), seed = 5L),
            )

        assertEquals(
            listOf("A", "A", "A", "A", "B", "B", "B", "B", "A", "A"),
            result.snapshot.designsInRowMajorOrder(),
        )
    }

    @Test
    fun `vertical stripes honor band width`() {
        val result =
            LayoutGenerator.generate(
                GenerationRequest(grid(2, 5), designs.take(2), LayoutMode.VerticalStripes(2), seed = 5L),
            )

        assertEquals(
            listOf("A", "A", "B", "B", "A", "A", "A", "B", "B", "A"),
            result.snapshot.designsInRowMajorOrder(),
        )
    }

    @Test
    fun `mirror left to right copies source half and preserves locked target`() {
        val original = rowOf("A", "B", "C", "LOCKED", lockedColumn = 3)

        val result =
            LayoutGenerator.generate(
                GenerationRequest(
                    original,
                    emptyList(),
                    LayoutMode.Mirror(MirrorDirection.LEFT_TO_RIGHT),
                    seed = 0L,
                ),
            )

        assertEquals(listOf("A", "B", "B", "LOCKED"), result.snapshot.designsInRowMajorOrder())
    }

    @Test
    fun `mirror right to left copies right source half`() {
        val original = rowOf("A", "B", "C", "D")

        val result =
            LayoutGenerator.generate(
                GenerationRequest(
                    original,
                    emptyList(),
                    LayoutMode.Mirror(MirrorDirection.RIGHT_TO_LEFT),
                    seed = 0L,
                ),
            )

        assertEquals(listOf("D", "C", "C", "D"), result.snapshot.designsInRowMajorOrder())
    }

    @Test
    fun `mirror top to bottom copies top source half`() {
        val original = columnOf("A", "B", "C", "D")

        val result =
            LayoutGenerator.generate(
                GenerationRequest(
                    original,
                    emptyList(),
                    LayoutMode.Mirror(MirrorDirection.TOP_TO_BOTTOM),
                    seed = 0L,
                ),
            )

        assertEquals(listOf("A", "B", "B", "A"), result.snapshot.designsInRowMajorOrder())
    }

    @Test
    fun `mirror bottom to top copies bottom source half`() {
        val original = columnOf("A", "B", "C", "D")

        val result =
            LayoutGenerator.generate(
                GenerationRequest(
                    original,
                    emptyList(),
                    LayoutMode.Mirror(MirrorDirection.BOTTOM_TO_TOP),
                    seed = 0L,
                ),
            )

        assertEquals(listOf("D", "C", "C", "D"), result.snapshot.designsInRowMajorOrder())
    }

    @Test
    fun `gradient left to right maps ordered endpoint and middle designs`() {
        val result =
            LayoutGenerator.generate(
                GenerationRequest(
                    grid(1, 3),
                    designs,
                    LayoutMode.Gradient(GradientDirection.LEFT_TO_RIGHT),
                    seed = 0L,
                ),
            )

        assertEquals(listOf("A", "B", "C"), result.snapshot.designsInRowMajorOrder())
    }

    @Test
    fun `gradient right to left reverses ordered endpoint and middle designs`() {
        val result =
            LayoutGenerator.generate(
                GenerationRequest(
                    grid(1, 3),
                    designs,
                    LayoutMode.Gradient(GradientDirection.RIGHT_TO_LEFT),
                    seed = 0L,
                ),
            )

        assertEquals(listOf("C", "B", "A"), result.snapshot.designsInRowMajorOrder())
    }

    @Test
    fun `gradient top to bottom maps ordered endpoint and middle designs`() {
        val result =
            LayoutGenerator.generate(
                GenerationRequest(
                    grid(3, 1),
                    designs,
                    LayoutMode.Gradient(GradientDirection.TOP_TO_BOTTOM),
                    seed = 0L,
                ),
            )

        assertEquals(listOf("A", "B", "C"), result.snapshot.designsInRowMajorOrder())
    }

    @Test
    fun `gradient bottom to top reverses ordered endpoint and middle designs`() {
        val result =
            LayoutGenerator.generate(
                GenerationRequest(
                    grid(3, 1),
                    designs,
                    LayoutMode.Gradient(GradientDirection.BOTTOM_TO_TOP),
                    seed = 0L,
                ),
            )

        assertEquals(listOf("C", "B", "A"), result.snapshot.designsInRowMajorOrder())
    }

    @Test
    fun `diagonal gradient maps opposite corners and center`() {
        val result =
            LayoutGenerator.generate(
                GenerationRequest(
                    grid(3, 3),
                    designs,
                    LayoutMode.Gradient(GradientDirection.DIAGONAL),
                    seed = 0L,
                ),
            )

        assertEquals("A", result.snapshot[CellCoordinate(0, 0)].designId)
        assertEquals("B", result.snapshot[CellCoordinate(1, 1)].designId)
        assertEquals("C", result.snapshot[CellCoordinate(2, 2)].designId)
    }

    @Test
    fun `radial maps center edge and corners into ordered distance bands`() {
        val result =
            LayoutGenerator.generate(
                GenerationRequest(grid(3, 3), designs, LayoutMode.Radial, seed = 0L),
            )

        assertEquals("A", result.snapshot[CellCoordinate(1, 1)].designId)
        assertEquals("B", result.snapshot[CellCoordinate(0, 1)].designId)
        assertEquals("C", result.snapshot[CellCoordinate(0, 0)].designId)
    }

    @Test(timeout = 1_000L)
    fun `all locked cells finish without changes or unbounded search`() {
        val original =
            grid(50, 50) { row, column ->
                CellState(CellCoordinate(row, column), designId = "LOCKED", locked = true)
            }

        val result =
            LayoutGenerator.generate(
                GenerationRequest(
                    original,
                    designs,
                    LayoutMode.BalancedRandom,
                    seed = 55L,
                    avoidOrthogonalNeighbors = true,
                ),
            )

        assertEquals(original, result.snapshot)
        assertEquals(0, result.changedCellCount)
    }

    @Test
    fun `insufficient designs leave layout unchanged instead of crashing`() {
        val original = grid(2, 2)

        val emptyRandom =
            LayoutGenerator.generate(
                GenerationRequest(original, emptyList(), LayoutMode.Random, seed = 1L),
            )
        val oneDesignChecker =
            LayoutGenerator.generate(
                GenerationRequest(original, listOf(WeightedDesign("A")), LayoutMode.Checker, seed = 1L),
            )

        assertEquals(original, emptyRandom.snapshot)
        assertEquals(original, oneDesignChecker.snapshot)
    }

    @Test
    fun `neighbor avoidance reduces identical orthogonal conflicts`() {
        val snapshot = grid(12, 12)
        val twoDesigns = designs.take(2)

        val unrestricted =
            LayoutGenerator.generate(
                GenerationRequest(
                    snapshot,
                    twoDesigns,
                    LayoutMode.Random,
                    seed = 12L,
                    avoidOrthogonalNeighbors = false,
                ),
            )
        val optimized =
            LayoutGenerator.generate(
                GenerationRequest(
                    snapshot,
                    twoDesigns,
                    LayoutMode.Random,
                    seed = 12L,
                    avoidOrthogonalNeighbors = true,
                    optimizationPasses = 4,
                ),
            )

        assertTrue(
            "expected ${optimized.orthogonalConflictCount} < ${unrestricted.orthogonalConflictCount}",
            optimized.orthogonalConflictCount < unrestricted.orthogonalConflictCount,
        )
    }

    @Test(timeout = 1_500L)
    fun `largest supported grid generation is bounded`() {
        val result =
            LayoutGenerator.generate(
                GenerationRequest(
                    grid(50, 50),
                    designs,
                    LayoutMode.BalancedRandom,
                    seed = Long.MAX_VALUE,
                    avoidOrthogonalNeighbors = true,
                    optimizationPasses = 4,
                ),
            )

        assertEquals(2_500, result.designCounts.values.sum())
    }

    private fun rowOf(
        vararg ids: String,
        lockedColumn: Int? = null,
    ) = grid(1, ids.size) { _, column ->
        CellState(
            CellCoordinate(0, column),
            designId = ids[column],
            locked = column == lockedColumn,
        )
    }

    private fun columnOf(vararg ids: String) =
        grid(ids.size, 1) { row, _ ->
            CellState(CellCoordinate(row, 0), designId = ids[row])
        }
}
