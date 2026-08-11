package com.finnvek.squaretool.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.finnvek.squaretool.R

enum class TopLevelDestination(
    val route: String,
    @StringRes val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home("home", R.string.home, Icons.Filled.Home, Icons.Outlined.Home),
    Planner("planner", R.string.planner, Icons.Filled.GridView, Icons.Outlined.GridView),
    Squares("squares", R.string.squares, Icons.Filled.Apps, Icons.Outlined.Apps),
    Library("library", R.string.library, Icons.Filled.Palette, Icons.Outlined.Palette),
    Settings("settings", R.string.settings, Icons.Filled.Settings, Icons.Outlined.Settings),
}
