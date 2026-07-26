package com.example.test_ai_project.feature.home.weather

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
internal data object WeatherRouteKey

internal fun NavGraphBuilder.weatherScreen() {
    composable<WeatherRouteKey> {
        WeatherScreen()
    }
}
