package com.example.test_ai_project.feature.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.test_ai_project.feature.home.map.mapScreen
import com.example.test_ai_project.feature.home.movies.moviesScreen
import com.example.test_ai_project.feature.home.prayertimes.prayerTimesScreen
import com.example.test_ai_project.feature.home.weather.weatherScreen

/**
 * The graph *inside* the home shell.
 *
 * Deliberately a second, nested [NavHost] rather than four destinations in the app graph:
 * each tab has to keep its own back stack and scroll position across switches, and the
 * navigation bar has to stay put while the content changes. Both fall out of nesting.
 *
 * Every tab registers itself through its own `...Screen()` extension, exactly as a feature
 * module does in the app graph — so a tab that outgrows this module can be lifted into
 * `:feature:<tab>` by moving its package, with this file unchanged.
 */
@Composable
internal fun HomeNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeTab.Start.routeKey,
        modifier = modifier,
    ) {
        moviesScreen()
        mapScreen()
        prayerTimesScreen()
        weatherScreen()
    }
}
