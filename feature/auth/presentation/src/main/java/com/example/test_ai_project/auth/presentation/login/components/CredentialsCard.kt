package com.example.test_ai_project.auth.presentation.login.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.test_ai_project.auth.presentation.R
import com.example.test_ai_project.auth.presentation.login.contract.LoginEvent
import com.example.test_ai_project.auth.presentation.login.contract.LoginState
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.component.AppButton
import com.example.test_ai_project.resource.component.AppIcon
import com.example.test_ai_project.resource.component.AppText
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.scaled
import com.example.test_ai_project.resource.theme.spacing

/** The white card holding both credential fields and the submit action. */
@Composable
internal fun CredentialsCard(
    state: LoginState,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // rememberSaveable, not remember: a rotation with the picker open should not drop it.
    var isDatePickerVisible by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(spacing.medium),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(spacing.large)) {
            DateOfBirthField(
                digits = state.dateOfBirthDigits,
                error = state.dateOfBirthError,
                onDigitsChange = { onEvent(LoginEvent.DateOfBirthChanged(it)) },
                onPickerClick = { isDatePickerVisible = true },
            )

            Spacer(modifier = Modifier.height(spacing.medium))

            PassportNumberField(
                value = state.passportNumber,
                error = state.passportNumberError,
                isVisible = state.isPassportNumberVisible,
                onValueChange = { onEvent(LoginEvent.PassportNumberChanged(it)) },
                onVisibilityToggle = {
                    onEvent(LoginEvent.PassportNumberVisibilityToggled)
                },
                onSubmit = { onEvent(LoginEvent.Submitted) },
            )

            Spacer(modifier = Modifier.height(spacing.small))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(
                    id = R.drawable.ic_lock,
                    contentDescription = null,
                    size = 13.scaled,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(spacing.small))
                AppText(
                    text = stringResource(id = ResR.string.login_encrypted_at_rest),
                    style = AppTextStyle.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            AppButton(
                text = stringResource(id = ResR.string.login_continue),
                onClick = { onEvent(LoginEvent.Submitted) },
                enabled = state.canSubmit,
                isLoading = state.isSubmitting,
                trailing = {
                    AppIcon(
                        id = R.drawable.ic_arrow_forward,
                        contentDescription = null,
                        size = 18.scaled,
                    )
                },
            )
        }
    }

    if (isDatePickerVisible) {
        DateOfBirthPicker(
            onDateSelected = { onEvent(LoginEvent.DateOfBirthChanged(it)) },
            onDismiss = { isDatePickerVisible = false },
        )
    }
}
