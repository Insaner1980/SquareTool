package com.finnvek.squaretool.domain.model

data class GridSize(
    val rows: Int,
    val columns: Int,
) {
    init {
        require(rows in MIN_DIMENSION..MAX_DIMENSION) {
            "Rows must be between $MIN_DIMENSION and $MAX_DIMENSION"
        }
        require(columns in MIN_DIMENSION..MAX_DIMENSION) {
            "Columns must be between $MIN_DIMENSION and $MAX_DIMENSION"
        }
        require(rows * columns <= MAX_CELLS) { "A grid may contain at most $MAX_CELLS cells" }
    }

    val cellCount: Int = rows * columns

    fun contains(coordinate: CellCoordinate): Boolean = coordinate.row in 0 until rows && coordinate.column in 0 until columns

    fun coordinates(): Sequence<CellCoordinate> =
        sequence {
            repeat(rows) { row ->
                repeat(columns) { column ->
                    yield(CellCoordinate(row, column))
                }
            }
        }

    companion object {
        const val MIN_DIMENSION = 1
        const val MAX_DIMENSION = 50
        const val MAX_CELLS = 2_500
    }
}

data class CellCoordinate(
    val row: Int,
    val column: Int,
) {
    init {
        require(row >= 0) { "Row cannot be negative" }
        require(column >= 0) { "Column cannot be negative" }
    }
}

data class CellState(
    val coordinate: CellCoordinate,
    val designId: String? = null,
    val locked: Boolean = false,
    val completed: Boolean = false,
    val gramsPerSquareOverride: Double? = null,
)

@ConsistentCopyVisibility
data class GridSnapshot private constructor(
    val size: GridSize,
    val cells: List<CellState>,
) {
    operator fun get(coordinate: CellCoordinate): CellState {
        require(size.contains(coordinate)) { "Coordinate $coordinate is outside $size" }
        return cells[coordinate.row * size.columns + coordinate.column]
    }

    fun updated(cell: CellState): GridSnapshot = updated(listOf(cell))

    fun updated(changes: Iterable<CellState>): GridSnapshot {
        val replacements = changes.associateBy(CellState::coordinate)
        replacements.keys.forEach { coordinate ->
            require(size.contains(coordinate)) { "Coordinate $coordinate is outside $size" }
        }
        if (replacements.isEmpty()) return this
        return of(size, cells.map { replacements[it.coordinate] ?: it })
    }

    companion object {
        fun blank(size: GridSize): GridSnapshot = of(size, emptyList())

        fun of(
            size: GridSize,
            cells: Iterable<CellState>,
        ): GridSnapshot {
            val supplied = linkedMapOf<CellCoordinate, CellState>()
            cells.forEach { cell ->
                require(size.contains(cell.coordinate)) {
                    "Coordinate ${cell.coordinate} is outside $size"
                }
                require(supplied.put(cell.coordinate, cell) == null) {
                    "Duplicate cell coordinate ${cell.coordinate}"
                }
            }
            val completeCells =
                size
                    .coordinates()
                    .map { coordinate ->
                        supplied[coordinate] ?: CellState(coordinate)
                    }.toList()
            return GridSnapshot(size, completeCells)
        }
    }
}
