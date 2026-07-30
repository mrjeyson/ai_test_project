package com.example.test_ai_project.home.presentation.home.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.test_ai_project.resource.component.BrandWordmark
import com.example.test_ai_project.resource.theme.AppTheme
import com.example.test_ai_project.resource.theme.VaultHairline
import com.example.test_ai_project.home.presentation.home.navigation.HomeNavHost
import com.example.test_ai_project.home.presentation.home.navigation.HomeTab
import com.example.test_ai_project.resource.R as CoreUiR
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.home.presentation.R

/**
 * The home shell: brand bar, navigation bar, and the frame the four tabs are drawn into.
 *
 * It owns no data of its own — the tabs behind [HomeNavHost] fetch what they render — so
 * there is no `HomeRoute`/`HomeScreen` pair here. That split exists to keep Hilt out of a
 * previewable composable, and with no ViewModel to inject there is nothing to split.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    HomeShell(
        selectedTab = HomeTab.entries.firstOrNull { currentDestination.isOn(it) },
        onTabSelected = navController::switchTo,
        modifier = modifier,
    ) { contentModifier ->
        HomeNavHost(navController = navController, modifier = contentModifier)
    }
}

/**
 * The chrome on its own, with the content area supplied by the caller.
 *
 * Splitting it out is what lets a `@Preview` — and later a screenshot test — exercise the
 * real bar states without standing up a NavHost.
 */
@Composable
private fun HomeShell(
    selectedTab: HomeTab?,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = { HomeTopBar() },
        bottomBar = { HomeBottomBar(selectedTab = selectedTab, onTabSelected = onTabSelected) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        // The content gets the insets as padding rather than the shell clipping to them, so
        // a full-bleed page (the map) can consume them differently without fighting a parent.
        content(Modifier.padding(innerPadding))
    }
}

/**
 * Brand and security status. Lives in the shell, not in a tab: it states a property of the
 * app — everything here is on-device — which stays true whichever page is open.
 */
@Composable
private fun HomeTopBar(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = CoreUiR.drawable.ic_shield),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    BrandWordmark(modifier = Modifier.padding(start = 8.dp))
                }

                LocalSecureBadge()
            }

            HorizontalDivider(color = VaultHairline)
        }
    }
}

/** The "LOCAL SECURE" status: the caption over the teal tick from the design. */
@Composable
private fun LocalSecureBadge(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = stringResource(id = ResR.string.home_local_secure),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(18.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(5.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_small),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun HomeBottomBar(
    selectedTab: HomeTab?,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        HomeTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        painter = painterResource(id = tab.iconRes),
                        // The label sits right below and is announced with the item, so
                        // describing the icon as well would only repeat it.
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = { Text(text = stringResource(id = tab.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/**
 * Matches against the whole hierarchy, not just the leaf: once a tab grows sub-pages, its
 * bar item has to stay lit while the user is inside one of them.
 */
private fun NavDestination?.isOn(tab: HomeTab): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.routeKey::class) } == true

/**
 * Re-tapping a tab must not stack a second copy of it, and returning to a tab must restore
 * where the user left off — which is what the three options below buy, in that order.
 */
private fun NavHostController.switchTo(tab: HomeTab) {
    navigate(tab.routeKey) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeShellPreview() {
    AppTheme {
        HomeShell(selectedTab = HomeTab.Movies, onTabSelected = {}) { modifier ->
            Box(modifier = modifier)
        }
    }
}
