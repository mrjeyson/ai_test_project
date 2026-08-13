package com.example.test_ai_project.home.presentation.settings.contract

import com.example.test_ai_project.resource.base.UiEffect
import com.example.test_ai_project.resource.base.UiEvent
import com.example.test_ai_project.resource.base.UiState

/**
 * Everything the settings tab renders.
 *
 * The theme is held as a boolean rather than a `ThemeMode`, because that is what the switch
 * on the row binds to — the mapping to the two-valued enum belongs in the ViewModel, where
 * a third mode would land if one is ever added.
 */
data class SettingsState(
    val isDarkTheme: Boolean = false,
) : UiState

sealed interface SettingsEvent : UiEvent {

    data class DarkThemeToggled(val isDark: Boolean) : SettingsEvent

    data object SignOutRequested : SettingsEvent
}

sealed interface SettingsEffect : UiEffect {

    /**
     * The session is over; where that lands is the root graph's decision.
     *
     * An effect, not state: the screen must navigate exactly once, and a rotation mid-exit
     * would replay a `signedOut` flag held in state.
     */
    data object SignedOut : SettingsEffect
}
