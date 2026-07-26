package com.example.test_ai_project.feature.login

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * The feature owns its route and its registration function; `:app` decides *where*
 * a successful authentication leads by supplying [onAuthenticated], so login never names
 * another feature.
 */
@Serializable
data object LoginRouteKey

fun NavController.navigateToLogin(navOptions: NavOptionsBuilder.() -> Unit = {}) {
    navigate(route = LoginRouteKey, builder = navOptions)
}

fun NavGraphBuilder.loginScreen(onAuthenticated: () -> Unit) {
    composable<LoginRouteKey> {
        LoginRoute(onAuthenticated = onAuthenticated)
    }
}
