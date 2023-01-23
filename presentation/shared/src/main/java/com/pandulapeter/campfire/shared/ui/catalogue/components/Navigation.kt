package com.pandulapeter.campfire.shared.ui.catalogue.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.BottomAppBar
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.campfire.shared.ui.CampfireViewModel
import com.pandulapeter.campfire.shared.ui.catalogue.theme.CampfireColors
import com.pandulapeter.campfire.shared.ui.utilities.UiSize

@Composable
fun CampfireScaffold(
    modifier: Modifier = Modifier,
    statusBarModifier: Modifier = Modifier,
    navigationDestinations: List<CampfireViewModel.NavigationDestinationWrapper>,
    uiSize: UiSize,
    bottomNavigationBar: @Composable () -> Unit,
    navigationRail: @Composable (scaffoldPadding: PaddingValues, content: @Composable () -> Unit) -> Unit,
    content: @Composable (scaffoldPadding: PaddingValues?) -> Unit
) = Scaffold(
    modifier = modifier,
    topBar = {
        CampfireAppBar(
            statusBarModifier = statusBarModifier,
            selectedNavigationDestination = navigationDestinations.firstOrNull { it.isSelected }?.destination
        )
    },
    bottomBar = bottomNavigationBar
) { scaffoldPadding ->
    when (uiSize) {
        UiSize.COMPACT -> content(scaffoldPadding)
        UiSize.EXPANDED -> navigationRail(scaffoldPadding) { content(null) }
    }
}

@Composable
fun CampfireAppBar(
    modifier: Modifier = Modifier,
    statusBarModifier: Modifier = Modifier,
    selectedNavigationDestination: CampfireViewModel.NavigationDestination?
) = TopAppBar(
    modifier = modifier,
    backgroundColor = MaterialTheme.colors.surface,
    title = {
        Text(
            modifier = statusBarModifier,
            text = selectedNavigationDestination?.displayName ?: "Campfire"
        )
    }
)

@Composable
fun CampfireNavigationRail(
    modifier: Modifier = Modifier,
    navigationDestinations: List<CampfireViewModel.NavigationDestinationWrapper>,
    onNavigationDestinationSelected: (CampfireViewModel.NavigationDestination) -> Unit
) = NavigationRail(
    modifier = modifier
) {
    navigationDestinations.forEach { navigationDestination ->
        NavigationRailItem(
            selected = navigationDestination.isSelected,
            onClick = { onNavigationDestinationSelected(navigationDestination.destination) },
            selectedContentColor = MaterialTheme.colors.primary,
            unselectedContentColor = CampfireColors.getUnselectedContentColor(),
            icon = {
                Icon(
                    imageVector = navigationDestination.destination.icon,
                    contentDescription = navigationDestination.destination.displayName,
                )
            }
        )
    }
}


@Composable
fun CampfireBottomNavigationBar(
    modifier: Modifier = Modifier,
    navigationDestinations: List<CampfireViewModel.NavigationDestinationWrapper>,
    onNavigationDestinationSelected: (CampfireViewModel.NavigationDestination) -> Unit
) = BottomAppBar(
    modifier = modifier,
    backgroundColor = MaterialTheme.colors.surface
) {
    navigationDestinations.forEach { navigationDestination ->
        BottomNavigationItem(
            selected = navigationDestination.isSelected,
            onClick = { onNavigationDestinationSelected(navigationDestination.destination) },
            selectedContentColor = MaterialTheme.colors.primary,
            unselectedContentColor = CampfireColors.getUnselectedContentColor(),
            icon = {
                Icon(
                    imageVector = navigationDestination.destination.icon,
                    contentDescription = navigationDestination.destination.displayName,
                )
            }
        )
    }
}