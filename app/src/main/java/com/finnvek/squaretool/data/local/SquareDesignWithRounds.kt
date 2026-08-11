package com.finnvek.squaretool.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class SquareDesignWithRounds(
    @Embedded val design: SquareDesignEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "squareDesignId",
    )
    val rounds: List<SquareRoundEntity>,
)
