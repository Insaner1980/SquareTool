package com.finnvek.squaretool.domain.algorithm

import android.annotation.SuppressLint
import com.finnvek.squaretool.domain.model.CellCoordinate
import com.finnvek.squaretool.domain.model.CellState
import com.finnvek.squaretool.domain.model.GradientDirection
import com.finnvek.squaretool.domain.model.GridSize
import com.finnvek.squaretool.domain.model.GridSnapshot
import com.finnvek.squaretool.domain.model.MirrorDirection
import java.util.Random
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

data class WeightedDesign(
    val designId: String,
    val weight: Double = 1.0,
)

sealed interface LayoutMode {
    data object Random : LayoutMode

    data object BalancedRandom : LayoutMode

    data object Checker : LayoutMode

    data object AlternatingRows : LayoutMode

    data object AlternatingColumns : LayoutMode

    data object Diagonal : LayoutMode

    data class HorizontalStripes(
        val bandWidth: Int = 1,
    ) : LayoutMode

    data class VerticalStripes(
        val bandWidth: Int = 1,
    ) : LayoutMode

    data class Mirror(
        val direction: MirrorDirection,
    ) : LayoutMode

    data class Gradient(
        val direction: GradientDirection,
    ) : LayoutMode

    data object Radial : LayoutMode
}

data class GenerationRequest(
    val snapshot: GridSnapshot,
    val designs: List<WeightedDesign>,
    val mode: LayoutMode,
    val seed: Long,
    val avoidOrthogonalNeighbors: Boolean = false,
    val overwriteCompleted: Boolean = false,
    val optimizationPasses: Int = DEFAULT_OPTIMIZATION_PASSES,
) {
    companion object {
        const val DEFAULT_OPTIMIZATION_PASSES = 4
    }
}

data class GenerationResult(
    val snapshot: GridSnapshot,
    val designCounts: Map<String, Int>,
    val orthogonalConflictCount: Int,
    val changedCellCount: Int,
)

// The seeded PRNG produces reproducible visual layouts and is never used for security decisions.
@SuppressLint("WeakPrng")
object LayoutGenerator {
    fun generate(request: GenerationRequest): GenerationResult {
        val original = request.snapshot
        val assignments = original.cells.associate { it.coordinate to it.designId }.toMutableMap()
        val eligible =
            original.cells
                .filter { cell ->
                    !cell.locked && (request.overwriteCompleted || !cell.completed)
                }.map(CellState::coordinate)

        if (request.mode is LayoutMode.Mirror) {
            applyMirror(original, assignments, request.mode.direction, request.overwriteCompleted)
            return result(original, assignments)
        }

        val designs = validDesigns(request.designs)
        if (designs.isEmpty() || (request.mode == LayoutMode.Checker && designs.size != 2)) {
            return result(original, assignments)
        }
        eligible.forEach { assignments[it] = null }

        when (val mode = request.mode) {
            LayoutMode.Random -> {
                fillRandom(
                    coordinates = eligible,
                    assignments = assignments,
                    size = original.size,
                    designs = designs,
                    seed = request.seed,
                    avoidNeighbors = request.avoidOrthogonalNeighbors,
                )
            }

            LayoutMode.BalancedRandom -> {
                fillBalanced(
                    coordinates = eligible,
                    assignments = assignments,
                    size = original.size,
                    designs = designs,
                    seed = request.seed,
                    avoidNeighbors = request.avoidOrthogonalNeighbors,
                    preservedCounts = preservedCounts(original, eligible.toHashSet(), designs),
                )
            }

            LayoutMode.Checker -> {
                fillByCoordinate(eligible, assignments) { coordinate ->
                    designs[(coordinate.row + coordinate.column) % 2].designId
                }
            }

            LayoutMode.AlternatingRows -> {
                fillByCoordinate(eligible, assignments) { coordinate ->
                    designs[coordinate.row % designs.size].designId
                }
            }

            LayoutMode.AlternatingColumns -> {
                fillByCoordinate(eligible, assignments) { coordinate ->
                    designs[coordinate.column % designs.size].designId
                }
            }

            LayoutMode.Diagonal -> {
                fillByCoordinate(eligible, assignments) { coordinate ->
                    designs[(coordinate.row + coordinate.column) % designs.size].designId
                }
            }

            is LayoutMode.HorizontalStripes -> {
                val bandWidth = mode.bandWidth.coerceAtLeast(1)
                fillByCoordinate(eligible, assignments) { coordinate ->
                    designs[(coordinate.row / bandWidth) % designs.size].designId
                }
            }

            is LayoutMode.VerticalStripes -> {
                val bandWidth = mode.bandWidth.coerceAtLeast(1)
                fillByCoordinate(eligible, assignments) { coordinate ->
                    designs[(coordinate.column / bandWidth) % designs.size].designId
                }
            }

            is LayoutMode.Gradient -> {
                fillByCoordinate(eligible, assignments) { coordinate ->
                    val position = gradientPosition(coordinate, original.size, mode.direction)
                    designs[(position * (designs.size - 1)).roundToInt()].designId
                }
            }

            LayoutMode.Radial -> {
                fillByCoordinate(eligible, assignments) { coordinate ->
                    val position = radialPosition(coordinate, original.size)
                    designs[(position * (designs.size - 1)).roundToInt()].designId
                }
            }

            is LayoutMode.Mirror -> {
                error("Mirror is handled before design validation")
            }
        }

        if (
            request.avoidOrthogonalNeighbors &&
            (request.mode == LayoutMode.Random || request.mode == LayoutMode.BalancedRandom)
        ) {
            optimizeBySwapping(
                assignments = assignments,
                size = original.size,
                mutableCoordinates = eligible,
                seed = request.seed xor OPTIMIZATION_SEED_SALT,
                passes = request.optimizationPasses.coerceIn(0, MAX_OPTIMIZATION_PASSES),
            )
        }
        return result(original, assignments)
    }

    private fun validDesigns(designs: List<WeightedDesign>): List<WeightedDesign> {
        val combined = linkedMapOf<String, Double>()
        designs.forEach { design ->
            if (design.designId.isNotBlank() && design.weight.isFinite() && design.weight > 0.0) {
                combined[design.designId] = combined.getOrDefault(design.designId, 0.0) + design.weight
            }
        }
        return combined.map { (id, weight) -> WeightedDesign(id, weight) }
    }

    private fun fillRandom(
        coordinates: List<CellCoordinate>,
        assignments: MutableMap<CellCoordinate, String?>,
        size: GridSize,
        designs: List<WeightedDesign>,
        seed: Long,
        avoidNeighbors: Boolean,
    ) {
        val random = Random(seed)
        val order = shuffled(coordinates, random)
        order.forEach { coordinate ->
            val candidates =
                if (avoidNeighbors) {
                    val neighborIds = neighbors(coordinate, size).mapNotNull(assignments::get).toSet()
                    designs.filterNot { it.designId in neighborIds }.ifEmpty { designs }
                } else {
                    designs
                }
            assignments[coordinate] = weightedChoice(candidates, random).designId
        }
    }

    private fun fillBalanced(
        coordinates: List<CellCoordinate>,
        assignments: MutableMap<CellCoordinate, String?>,
        size: GridSize,
        designs: List<WeightedDesign>,
        seed: Long,
        avoidNeighbors: Boolean,
        preservedCounts: Map<String, Int>,
    ) {
        val random = Random(seed)
        val remaining = balancedEligibleTargets(coordinates.size, designs, preservedCounts).toMutableMap()
        val order = shuffled(coordinates, random)
        order.forEach { coordinate ->
            val available = designs.filter { remaining.getOrDefault(it.designId, 0) > 0 }
            if (available.isEmpty()) return@forEach
            val neighborIds =
                if (avoidNeighbors) {
                    neighbors(coordinate, size).mapNotNull(assignments::get).toSet()
                } else {
                    emptySet()
                }
            val conflictFree = available.filterNot { it.designId in neighborIds }
            val candidates = conflictFree.ifEmpty { available }
            val highestRemaining = candidates.maxOf { remaining.getValue(it.designId) }
            val mostNeeded = candidates.filter { remaining.getValue(it.designId) == highestRemaining }
            val chosen = mostNeeded[random.nextInt(mostNeeded.size)]
            assignments[coordinate] = chosen.designId
            remaining[chosen.designId] = remaining.getValue(chosen.designId) - 1
        }
    }

    private fun balancedEligibleTargets(
        eligibleCount: Int,
        designs: List<WeightedDesign>,
        preservedCounts: Map<String, Int>,
    ): Map<String, Int> {
        val preservedTotal = designs.sumOf { preservedCounts.getOrDefault(it.designId, 0) }
        val idealTotals = apportionedCounts(eligibleCount + preservedTotal, designs)
        val eligibleTargets =
            designs
                .associate { design ->
                    design.designId to
                        (idealTotals.getValue(design.designId) - preservedCounts.getOrDefault(design.designId, 0))
                            .coerceAtLeast(0)
                }.toMutableMap()
        var unallocated = eligibleCount - eligibleTargets.values.sum()
        while (unallocated > 0) {
            val chosen =
                designs.minWith(
                    compareBy<WeightedDesign> {
                        (preservedCounts.getOrDefault(it.designId, 0) + eligibleTargets.getValue(it.designId)) / it.weight
                    }.thenBy { designs.indexOf(it) },
                )
            eligibleTargets[chosen.designId] = eligibleTargets.getValue(chosen.designId) + 1
            unallocated -= 1
        }
        return eligibleTargets
    }

    private fun apportionedCounts(
        total: Int,
        designs: List<WeightedDesign>,
    ): Map<String, Int> {
        if (total == 0) return designs.associate { it.designId to 0 }
        val totalWeight = designs.sumOf(WeightedDesign::weight)
        val raw = designs.map { total * it.weight / totalWeight }
        val counts = raw.map { floor(it).toInt() }.toIntArray()
        var remainder = total - counts.sum()
        val remainderOrder =
            designs.indices.sortedWith(
                compareByDescending<Int> { raw[it] - counts[it] }.thenBy { it },
            )
        var index = 0
        while (remainder > 0) {
            counts[remainderOrder[index % remainderOrder.size]] += 1
            remainder -= 1
            index += 1
        }
        return designs.indices.associate { designs[it].designId to counts[it] }
    }

    private fun preservedCounts(
        snapshot: GridSnapshot,
        eligible: Set<CellCoordinate>,
        designs: List<WeightedDesign>,
    ): Map<String, Int> {
        val selectedIds = designs.mapTo(hashSetOf(), WeightedDesign::designId)
        return snapshot.cells
            .asSequence()
            .filterNot { it.coordinate in eligible }
            .mapNotNull { it.designId }
            .filter { it in selectedIds }
            .groupingBy { it }
            .eachCount()
    }

    private fun fillByCoordinate(
        coordinates: List<CellCoordinate>,
        assignments: MutableMap<CellCoordinate, String?>,
        designAt: (CellCoordinate) -> String,
    ) {
        coordinates.forEach { assignments[it] = designAt(it) }
    }

    private fun applyMirror(
        snapshot: GridSnapshot,
        assignments: MutableMap<CellCoordinate, String?>,
        direction: MirrorDirection,
        overwriteCompleted: Boolean,
    ) {
        fun copy(
            source: CellCoordinate,
            target: CellCoordinate,
        ) {
            val targetCell = snapshot[target]
            if (targetCell.locked || (targetCell.completed && !overwriteCompleted)) return
            assignments[target] = assignments[source]
        }

        when (direction) {
            MirrorDirection.LEFT_TO_RIGHT -> {
                repeat(snapshot.size.rows) { row ->
                    repeat(snapshot.size.columns / 2) { column ->
                        copy(
                            CellCoordinate(row, column),
                            CellCoordinate(row, snapshot.size.columns - 1 - column),
                        )
                    }
                }
            }

            MirrorDirection.RIGHT_TO_LEFT -> {
                repeat(snapshot.size.rows) { row ->
                    repeat(snapshot.size.columns / 2) { offset ->
                        val sourceColumn = snapshot.size.columns - 1 - offset
                        copy(CellCoordinate(row, sourceColumn), CellCoordinate(row, offset))
                    }
                }
            }

            MirrorDirection.TOP_TO_BOTTOM -> {
                repeat(snapshot.size.rows / 2) { row ->
                    repeat(snapshot.size.columns) { column ->
                        copy(
                            CellCoordinate(row, column),
                            CellCoordinate(snapshot.size.rows - 1 - row, column),
                        )
                    }
                }
            }

            MirrorDirection.BOTTOM_TO_TOP -> {
                repeat(snapshot.size.rows / 2) { offset ->
                    val sourceRow = snapshot.size.rows - 1 - offset
                    repeat(snapshot.size.columns) { column ->
                        copy(CellCoordinate(sourceRow, column), CellCoordinate(offset, column))
                    }
                }
            }
        }
    }

    private fun gradientPosition(
        coordinate: CellCoordinate,
        size: GridSize,
        direction: GradientDirection,
    ): Double {
        val horizontal = normalized(coordinate.column, size.columns)
        val vertical = normalized(coordinate.row, size.rows)
        return when (direction) {
            GradientDirection.LEFT_TO_RIGHT -> horizontal
            GradientDirection.RIGHT_TO_LEFT -> 1.0 - horizontal
            GradientDirection.TOP_TO_BOTTOM -> vertical
            GradientDirection.BOTTOM_TO_TOP -> 1.0 - vertical
            GradientDirection.DIAGONAL -> (horizontal + vertical) / 2.0
        }.coerceIn(0.0, 1.0)
    }

    private fun radialPosition(
        coordinate: CellCoordinate,
        size: GridSize,
    ): Double {
        val centerRow = (size.rows - 1) / 2.0
        val centerColumn = (size.columns - 1) / 2.0
        val maxDistance = hypot(centerRow, centerColumn)
        if (maxDistance == 0.0) return 0.0
        return (hypot(coordinate.row - centerRow, coordinate.column - centerColumn) / maxDistance)
            .coerceIn(0.0, 1.0)
    }

    private fun normalized(
        index: Int,
        count: Int,
    ): Double = if (count <= 1) 0.0 else index.toDouble() / (count - 1)

    private fun weightedChoice(
        designs: List<WeightedDesign>,
        random: Random,
    ): WeightedDesign {
        val totalWeight = designs.sumOf(WeightedDesign::weight)
        var threshold = random.nextDouble() * totalWeight
        designs.forEach { design ->
            threshold -= design.weight
            if (threshold < 0.0) return design
        }
        return designs.last()
    }

    private fun optimizeBySwapping(
        assignments: MutableMap<CellCoordinate, String?>,
        size: GridSize,
        mutableCoordinates: List<CellCoordinate>,
        seed: Long,
        passes: Int,
    ) {
        if (mutableCoordinates.size < 2 || passes == 0) return
        val random = Random(seed)
        val attempts = (mutableCoordinates.size * passes * 2).coerceAtMost(MAX_SWAP_ATTEMPTS)
        repeat(attempts) {
            val first = mutableCoordinates[random.nextInt(mutableCoordinates.size)]
            val second = mutableCoordinates[random.nextInt(mutableCoordinates.size)]
            if (first == second || assignments[first] == assignments[second]) return@repeat
            val affectedEdges = affectedEdges(first, second, size)
            val before = conflictsOnEdges(assignments, affectedEdges)
            val firstValue = assignments[first]
            assignments[first] = assignments[second]
            assignments[second] = firstValue
            val after = conflictsOnEdges(assignments, affectedEdges)
            if (after >= before) {
                val secondValue = assignments[second]
                assignments[second] = assignments[first]
                assignments[first] = secondValue
            }
        }
    }

    private fun affectedEdges(
        first: CellCoordinate,
        second: CellCoordinate,
        size: GridSize,
    ): Set<Pair<CellCoordinate, CellCoordinate>> =
        buildSet {
            listOf(first, second).forEach { coordinate ->
                neighbors(coordinate, size).forEach { neighbor ->
                    add(canonicalEdge(coordinate, neighbor))
                }
            }
        }

    private fun canonicalEdge(
        first: CellCoordinate,
        second: CellCoordinate,
    ): Pair<CellCoordinate, CellCoordinate> {
        val firstIndex = first.row * GridSize.MAX_DIMENSION + first.column
        val secondIndex = second.row * GridSize.MAX_DIMENSION + second.column
        return if (firstIndex <= secondIndex) first to second else second to first
    }

    private fun conflictsOnEdges(
        assignments: Map<CellCoordinate, String?>,
        edges: Set<Pair<CellCoordinate, CellCoordinate>>,
    ): Int =
        edges.count { (first, second) ->
            val firstDesign = assignments[first]
            firstDesign != null && firstDesign == assignments[second]
        }

    private fun neighbors(
        coordinate: CellCoordinate,
        size: GridSize,
    ): List<CellCoordinate> =
        buildList(4) {
            if (coordinate.row > 0) add(CellCoordinate(coordinate.row - 1, coordinate.column))
            if (coordinate.row + 1 < size.rows) add(CellCoordinate(coordinate.row + 1, coordinate.column))
            if (coordinate.column > 0) add(CellCoordinate(coordinate.row, coordinate.column - 1))
            if (coordinate.column + 1 < size.columns) add(CellCoordinate(coordinate.row, coordinate.column + 1))
        }

    private fun countOrthogonalConflicts(
        assignments: Map<CellCoordinate, String?>,
        size: GridSize,
    ): Int {
        var conflicts = 0
        repeat(size.rows) { row ->
            repeat(size.columns) { column ->
                val coordinate = CellCoordinate(row, column)
                val design = assignments[coordinate] ?: return@repeat
                if (column + 1 < size.columns && design == assignments[CellCoordinate(row, column + 1)]) {
                    conflicts += 1
                }
                if (row + 1 < size.rows && design == assignments[CellCoordinate(row + 1, column)]) {
                    conflicts += 1
                }
            }
        }
        return conflicts
    }

    private fun result(
        original: GridSnapshot,
        assignments: Map<CellCoordinate, String?>,
    ): GenerationResult {
        val changedCells =
            original.cells.map { cell ->
                val designId = assignments[cell.coordinate]
                if (designId == cell.designId) cell else cell.copy(designId = designId)
            }
        val snapshot = GridSnapshot.of(original.size, changedCells)
        return GenerationResult(
            snapshot = snapshot,
            designCounts =
                snapshot.cells
                    .mapNotNull(CellState::designId)
                    .groupingBy { it }
                    .eachCount(),
            orthogonalConflictCount = countOrthogonalConflicts(assignments, original.size),
            changedCellCount = original.cells.indices.count { original.cells[it] != changedCells[it] },
        )
    }

    private fun <T> shuffled(
        source: List<T>,
        random: Random,
    ): List<T> {
        val values = source.toMutableList()
        for (index in values.lastIndex downTo 1) {
            val swapIndex = random.nextInt(index + 1)
            val value = values[index]
            values[index] = values[swapIndex]
            values[swapIndex] = value
        }
        return values
    }

    private const val OPTIMIZATION_SEED_SALT = -7_046_029_254_386_353_131L
    private const val MAX_OPTIMIZATION_PASSES = 20
    private const val MAX_SWAP_ATTEMPTS = 20_000
}
