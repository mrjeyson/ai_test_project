package com.example.test_ai_project.home.presentation.settings.viewmodel

import app.cash.turbine.test
import com.example.test_ai_project.home.presentation.settings.contract.SettingsEffect
import com.example.test_ai_project.home.presentation.settings.contract.SettingsEvent
import com.example.test_ai_project.home.presentation.testing.MainDispatcherRule
import com.example.test_ai_project.resource.theme.ThemeMode
import com.example.test_ai_project.resource.theme.ThemeService
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * The theme service is faked, which is the whole point of it being an interface: the real
 * one needs a `Context` and would drag Robolectric into a JVM test to answer one enum.
 */
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val themeService = FakeThemeService()

    private fun viewModel() = SettingsViewModel(themeService = themeService)

    @Test
    fun `starts on the stored theme rather than the default`() = runTest {
        themeService.setMode(ThemeMode.Dark)

        // Read before anything collects: the initial value has to be right on its own, or
        // the switch is drawn off and flicks across a frame later.
        assertThat(viewModel().uiState.value.isDarkTheme).isTrue()
    }

    @Test
    fun `toggling dark on stores it and republishes`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertThat(awaitItem().isDarkTheme).isFalse()

            viewModel.onEvent(SettingsEvent.DarkThemeToggled(isDark = true))

            assertThat(awaitItem().isDarkTheme).isTrue()
            assertThat(themeService.mode.value).isEqualTo(ThemeMode.Dark)
        }
    }

    @Test
    fun `toggling dark off returns to light`() = runTest {
        themeService.setMode(ThemeMode.Dark)
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertThat(awaitItem().isDarkTheme).isTrue()

            viewModel.onEvent(SettingsEvent.DarkThemeToggled(isDark = false))

            assertThat(awaitItem().isDarkTheme).isFalse()
            assertThat(themeService.mode.value).isEqualTo(ThemeMode.Light)
        }
    }

    @Test
    fun `signing out emits the effect once and changes no state`() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.SignOutRequested)

            assertThat(awaitItem()).isEqualTo(SettingsEffect.SignedOut)
            expectNoEvents()
        }

        // Signing out must not disturb the appearance the user chose — they will meet it
        // again on the login screen a moment later.
        assertThat(themeService.mode.value).isEqualTo(ThemeMode.Light)
    }
}

private class FakeThemeService : ThemeService {

    private val _mode = MutableStateFlow(ThemeMode.Default)
    override val mode: StateFlow<ThemeMode> = _mode

    override fun setMode(mode: ThemeMode) {
        _mode.value = mode
    }
}
