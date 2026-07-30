package com.example.test_ai_project.home.presentation.weather.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import com.example.test_ai_project.home.presentation.weather.contract.DailyRow
import com.example.test_ai_project.home.presentation.weather.contract.HourlyColumn
import com.example.test_ai_project.home.presentation.weather.contract.WeatherEvent
import com.example.test_ai_project.home.presentation.weather.contract.WeatherState
import com.example.test_ai_project.home.presentation.weather.screen.WeatherScreen

@Serializable
internal data object WeatherRouteKey

internal fun NavGraphBuilder.weatherScreen() {
    composable<WeatherRouteKey> {
        WeatherScreen()
    }
}
