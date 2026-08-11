package com.finnvek.squaretool.domain

import com.finnvek.squaretool.domain.model.CellCoordinate
import com.finnvek.squaretool.domain.model.CellState
import com.finnvek.squaretool.domain.model.GridSize
import com.finnvek.squaretool.domain.model.GridSnapshot

internal fun grid(
    rows: Int,
    columns: Int,
    cell: (row: Int, column: Int) -> CellState = { row, column ->
        CellState(CellCoordinate(row, column))
    },
): GridSnapshot =
    GridSnapshot.of(
        size = GridSize(rows, columns),
        cells =
            buildList {
                repeat(rows) { row ->
                    repeat(columns) { column ->
                        add(cell(row, column))
                    }
                }
            },
    )

internal fun GridSnapshot.designsInRowMajorOrder(): List<String?> = cells.map(CellState::designId)
