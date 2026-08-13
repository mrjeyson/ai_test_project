package com.example.test_ai_project.home.presentation.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import com.example.test_ai_project.home.presentation.settings.screen.SettingsScreen

@Serializable
internal data object SettingsRouteKey

internal fun NavGraphBuilder.settingsScreen(onSignedOut: () -> Unit) {
    composable<SettingsRouteKey> {
        SettingsScreen(onSignedOut = onSignedOut)
    }
}
