package com.example.test_ai_project.feature.home.map

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
internal data object MapRouteKey

internal fun NavGraphBuilder.mapScreen() {
    composable<MapRouteKey> {
        MapScreen()
    }
}
