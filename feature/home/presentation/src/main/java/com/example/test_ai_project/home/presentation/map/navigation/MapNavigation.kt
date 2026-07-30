package com.example.test_ai_project.home.presentation.map.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import com.example.test_ai_project.home.presentation.map.contract.LocationPermission
import com.example.test_ai_project.home.presentation.map.contract.MapEvent
import com.example.test_ai_project.home.presentation.map.contract.MapState
import com.example.test_ai_project.home.presentation.map.screen.MapScreen

@Serializable
internal data object MapRouteKey

internal fun NavGraphBuilder.mapScreen() {
    composable<MapRouteKey> {
        MapScreen()
    }
}
