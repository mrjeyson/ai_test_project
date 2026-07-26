package com.example.test_ai_project.feature.splash

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * The feature owns its route and its registration function; `:app` decides *where*
 * bootstrap leads by supplying [onFinished], so the splash never names another feature.
 */
@Serializable
data object SplashRouteKey

fun NavGraphBuilder.splashScreen(onFinished: () -> Unit) {
    composable<SplashRouteKey> {
        SplashRoute(onFinished = onFinished)
    }
}
