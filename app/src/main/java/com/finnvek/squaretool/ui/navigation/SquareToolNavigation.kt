package com.finnvek.squaretool.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

@Composable
fun SquareToolNavigationBar(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            val label = stringResource(destination.titleRes)
            NavigationBarItem(
                modifier = Modifier.testTag("top_level_destination"),
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = { DestinationIcon(destination, selected == destination) },
                label = { Text(label) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(),
            )
        }
    }
}

@Composable
fun SquareToolNavigationRail(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            val label = stringResource(destination.titleRes)
            NavigationRailItem(
                modifier = Modifier.testTag("top_level_destination"),
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = { DestinationIcon(destination, selected == destination) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun DestinationIcon(
    destination: TopLevelDestination,
    selected: Boolean,
) {
    Icon(
        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
        contentDescription = null,
    )
}
