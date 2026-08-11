package com.finnvek.squaretool.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

object SquareToolSpacing {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Standard = 16.dp
    val Large = 20.dp
    val Section = 24.dp
    val ExtraLarge = 32.dp
}

val SquareToolShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(26.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

object SquareToolMotion {
    const val Quick = 150
    const val Standard = 220
    const val Deliberate = 300
}

val LocalReduceMotion = staticCompositionLocalOf { false }
