package com.example.test_ai_project.feature.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * The feature owns its own route and its own registration function, so `:app`
 * composes the graph without knowing which composable backs the destination.
 */
@Serializable
data object HomeRouteKey

fun NavController.navigateToHome(navOptions: NavOptionsBuilder.() -> Unit = {}) {
    navigate(route = HomeRouteKey, builder = navOptions)
}

fun NavGraphBuilder.homeScreen() {
    composable<HomeRouteKey> {
        HomeRoute()
    }
}
