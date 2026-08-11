package com.finnvek.squaretool.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        primary = OlivePrimary,
        onPrimary = WarmSurface,
        primaryContainer = OliveContainer,
        onPrimaryContainer = Color(0xFF1D260D),
        secondary = BurntOrange,
        onSecondary = WarmSurface,
        secondaryContainer = OrangeContainer,
        onSecondaryContainer = Color(0xFF351006),
        background = WarmBackground,
        onBackground = WarmOnSurface,
        surface = WarmSurface,
        onSurface = WarmOnSurface,
        surfaceVariant = WarmSurfaceContainer,
        onSurfaceVariant = WarmOnSurfaceVariant,
        outline = Color(0xFF747668),
        error = Color(0xFF9F2D22),
        onError = Color.White,
    )

private val DarkColors =
    darkColorScheme(
        primary = OlivePrimaryDark,
        onPrimary = Color(0xFF172000),
        primaryContainer = OliveContainerDark,
        onPrimaryContainer = Color(0xFFDDE9AD),
        secondary = BurntOrangeDark,
        onSecondary = Color(0xFF4B1907),
        secondaryContainer = OrangeContainerDark,
        onSecondaryContainer = Color(0xFFFFDCC8),
        background = WarmBackgroundDark,
        onBackground = WarmOnSurfaceDark,
        surface = WarmSurfaceDark,
        onSurface = WarmOnSurfaceDark,
        surfaceVariant = WarmSurfaceContainerDark,
        onSurfaceVariant = WarmOnSurfaceVariantDark,
        outline = Color(0xFF959784),
        error = Color(0xFFFFB4A9),
        onError = Color(0xFF680003),
    )

@Composable
fun SquareToolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SquareToolTypography,
        shapes = SquareToolShapes,
        content = content,
    )
}
