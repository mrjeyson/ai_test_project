package com.example.test_ai_project.home.presentation.settings.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.test_ai_project.home.presentation.settings.components.SettingsSection
import com.example.test_ai_project.home.presentation.settings.components.SettingsToggleRow
import com.example.test_ai_project.home.presentation.settings.components.SignOutRow
import com.example.test_ai_project.home.presentation.settings.contract.SettingsEffect
import com.example.test_ai_project.home.presentation.settings.contract.SettingsEvent
import com.example.test_ai_project.home.presentation.settings.contract.SettingsState
import com.example.test_ai_project.home.presentation.settings.viewmodel.SettingsViewModel
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.preview.DevicePreview
import com.example.test_ai_project.resource.theme.AppTheme
import com.example.test_ai_project.resource.theme.spacing
import com.example.test_ai_project.resource.util.CollectAsEffect

/**
 * Stateful entry point: the only place in this file that touches Hilt, the ViewModel or the
 * effect stream.
 *
 * [onSignedOut] is a lambda rather than a route this screen navigates to, for the same
 * reason `onAuthenticated` is: where a finished session lands is the root graph's decision,
 * and a tab that named the login route would tie the home feature to the auth one.
 */
@Composable
internal fun SettingsScreen(
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    viewModel.effects.CollectAsEffect { effect ->
        when (effect) {
            SettingsEffect.SignedOut -> onSignedOut()
        }
    }

    SettingsContent(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

/** Stateless and side-effect free — driven entirely by its parameters. */
@Composable
internal fun SettingsContent(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.medium, vertical = spacing.small),
    ) {
        SettingsSection(titleRes = ResR.string.settings_appearance_title) {
            SettingsToggleRow(
                labelRes = ResR.string.settings_dark_theme_label,
                captionRes = ResR.string.settings_dark_theme_caption,
                checked = state.isDarkTheme,
                onCheckedChange = { isDark -> onEvent(SettingsEvent.DarkThemeToggled(isDark)) },
            )
        }

        Spacer(modifier = Modifier.height(spacing.large))

        SettingsSection(titleRes = ResR.string.settings_session_title) {
            SignOutRow(onSignOut = { onEvent(SettingsEvent.SignOutRequested) })
        }

        Spacer(modifier = Modifier.height(spacing.large))
    }
}

@DevicePreview
@Composable
private fun SettingsContentPreview() {
    AppTheme {
        SettingsContent(state = SettingsState(isDarkTheme = false), onEvent = {})
    }
}
