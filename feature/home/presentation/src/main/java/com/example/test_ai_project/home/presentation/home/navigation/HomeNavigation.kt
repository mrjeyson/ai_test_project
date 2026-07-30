package com.example.test_ai_project.home.presentation.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import com.example.test_ai_project.home.presentation.home.screen.HomeScreen

/**
 * The feature owns its own route and its own registration function, so `:app`
 * composes the graph without knowing which composable backs the destination.
 *
 * One destination, not four: the tabs live in a nested graph inside [HomeScreen], so the
 * app graph — and back from home — sees the shell as a single stop.
 */
@Serializable
data object HomeRouteKey

fun NavController.navigateToHome(navOptions: NavOptionsBuilder.() -> Unit = {}) {
    navigate(route = HomeRouteKey, builder = navOptions)
}

fun NavGraphBuilder.homeScreen() {
    composable<HomeRouteKey> {
        HomeScreen()
    }
}