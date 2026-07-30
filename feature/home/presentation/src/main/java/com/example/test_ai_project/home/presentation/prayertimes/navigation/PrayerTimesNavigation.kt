package com.example.test_ai_project.home.presentation.prayertimes.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import com.example.test_ai_project.home.presentation.prayertimes.contract.NextPrayer
import com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerEntry
import com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerStatus
import com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerTimesEvent
import com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerTimesState
import com.example.test_ai_project.home.presentation.prayertimes.screen.PrayerTimesScreen

@Serializable
internal data object PrayerTimesRouteKey

internal fun NavGraphBuilder.prayerTimesScreen() {
    composable<PrayerTimesRouteKey> {
        PrayerTimesScreen()
    }
}
