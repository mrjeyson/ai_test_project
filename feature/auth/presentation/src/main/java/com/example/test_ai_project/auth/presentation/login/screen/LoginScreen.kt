package com.example.test_ai_project.auth.presentation.login.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.test_ai_project.auth.domain.model.DateOfBirthError
import com.example.test_ai_project.auth.domain.model.PassportNumberError
import com.example.test_ai_project.auth.presentation.login.components.CredentialsCard
import com.example.test_ai_project.auth.presentation.login.components.FooterLinks
import com.example.test_ai_project.auth.presentation.login.components.TrustChip
import com.example.test_ai_project.auth.presentation.login.contract.LoginEffect
import com.example.test_ai_project.auth.presentation.login.contract.LoginErrorMessage
import com.example.test_ai_project.auth.presentation.login.contract.LoginEvent
import com.example.test_ai_project.auth.presentation.login.contract.LoginState
import com.example.test_ai_project.auth.presentation.login.viewmodel.LoginViewModel
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.component.AppText
import com.example.test_ai_project.resource.component.BrandLogo
import com.example.test_ai_project.resource.component.BrandWordmark
import com.example.test_ai_project.resource.component.LocalAppToast
import com.example.test_ai_project.resource.preview.DevicePreview
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.AppTheme
import com.example.test_ai_project.resource.theme.scaled
import com.example.test_ai_project.resource.theme.spacing
import com.example.test_ai_project.resource.util.CollectAsEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Stateful entry point: the only place in this file that touches Hilt, the ViewModel or
 * the effect stream. Keeping it separate from [LoginContent] is what makes the screen
 * previewable and testable without a DI graph.
 */
@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalAppToast.current
    val context = LocalContext.current

    viewModel.effects.CollectAsEffect { effect ->
        when (effect) {
            LoginEffect.Authenticated -> onAuthenticated()
            is LoginEffect.ShowError -> toast.show(
                when (val message = effect.message) {
                    is LoginErrorMessage.Literal -> message.text
                    is LoginErrorMessage.Resource -> context.getString(message.id)
                },
            )
        }
    }

    LoginContent(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

/** Stateless and side-effect free — driven entirely by its parameters. */
@Composable
fun LoginContent(
    state: LoginState,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // safeDrawing covers the IME too, so the scroll below is what keeps the
            // fields reachable once the keyboard is up.
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(spacing.medium))
        BrandLogo(size = 56.scaled, tileColor = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(spacing.medium))
        BrandWordmark()
        Spacer(modifier = Modifier.height(spacing.small))
        AppText(
            text = stringResource(id = ResR.string.login_subtitle),
            style = AppTextStyle.BodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(spacing.large))

        CredentialsCard(state = state, onEvent = onEvent)

        Spacer(modifier = Modifier.height(spacing.large))

        TrustChip()

        Spacer(modifier = Modifier.height(spacing.medium))

        AppText(
            text = stringResource(id = ResR.string.login_privacy_note),
            style = AppTextStyle.BodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = spacing.large),
        )

        Spacer(modifier = Modifier.height(spacing.small))

        FooterLinks()

        Spacer(modifier = Modifier.height(spacing.large))
    }
}

@DevicePreview
@Composable
private fun LoginContentPreview() {
    AppTheme {
        LoginContent(
            state = LoginState(
                dateOfBirthDigits = "3102",
                passportNumber = "AB-12",
                dateOfBirthError = DateOfBirthError.Incomplete,
                passportNumberError = PassportNumberError.InvalidCharacters,
            ),
            onEvent = {},
        )
    }
}
