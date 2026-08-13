package com.example.test_ai_project.home.presentation.settings.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.test_ai_project.home.presentation.settings.contract.SettingsEffect
import com.example.test_ai_project.home.presentation.settings.contract.SettingsEvent
import com.example.test_ai_project.home.presentation.settings.contract.SettingsState
import com.example.test_ai_project.resource.base.BaseViewModel
import com.example.test_ai_project.resource.theme.ThemeMode
import com.example.test_ai_project.resource.theme.ThemeService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeService: ThemeService,
) : BaseViewModel<SettingsState, SettingsEvent, SettingsEffect>(SettingsState()) {

    /**
     * Derived, not reduced: the stored theme is the single source of truth and it is
     * already a flow, so mirroring it into `setState` would create a second copy that could
     * disagree with the one the app is actually painted with.
     *
     * The initial value comes from the same flow's current value rather than the default,
     * so the switch is drawn already in the right position instead of flicking across on
     * the first emission.
     */
    override val uiState: StateFlow<SettingsState> = themeService.mode
        .map { mode -> SettingsState(isDarkTheme = mode == ThemeMode.Dark) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SettingsState(isDarkTheme = themeService.mode.value == ThemeMode.Dark),
        )

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.DarkThemeToggled -> setDarkTheme(event.isDark)
            SettingsEvent.SignOutRequested -> signOut()
        }
    }

    private fun setDarkTheme(isDark: Boolean) {
        themeService.setMode(if (isDark) ThemeMode.Dark else ThemeMode.Light)
    }

    /**
     * Nothing is torn down here, and that is not an omission: authentication holds no token
     * and writes no session — the signed-in state *is* the back stack — so ending the
     * session is entirely the navigation this effect asks for. The day a session store
     * arrives, clearing it belongs on this line, before the effect.
     */
    private fun signOut() {
        sendEffect(SettingsEffect.SignedOut)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
