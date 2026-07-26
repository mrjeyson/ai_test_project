package com.example.test_ai_project.feature.home.prayertimes

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
internal data object PrayerTimesRouteKey

internal fun NavGraphBuilder.prayerTimesScreen() {
    composable<PrayerTimesRouteKey> {
        PrayerTimesScreen()
    }
}
