package com.example.test_ai_project.auth.presentation.login.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.test_ai_project.auth.domain.model.PassportNumberError
import com.example.test_ai_project.auth.presentation.R
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.component.AppIcon
import com.example.test_ai_project.resource.component.AppTextField
import com.example.test_ai_project.resource.theme.scaled
import com.example.test_ai_project.resource.theme.spacing

/** The passport-number field, with its masking toggle and error line. */
@Composable
internal fun PassportNumberField(
    value: String,
    error: PassportNumberError?,
    isVisible: Boolean,
    onValueChange: (String) -> Unit,
    onVisibilityToggle: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        FieldLabel(
            iconRes = R.drawable.ic_passport,
            label = stringResource(id = ResR.string.login_passport_number_label),
        )
        Spacer(modifier = Modifier.height(spacing.small))
        AppTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(id = ResR.string.login_passport_number_placeholder),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            visualTransformation = if (isVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            isError = error != null,
            trailing = {
                IconButton(onClick = onVisibilityToggle, modifier = Modifier.size(40.scaled)) {
                    AppIcon(
                        id = if (isVisible) {
                            R.drawable.ic_visibility_off
                        } else {
                            R.drawable.ic_visibility
                        },
                        contentDescription = stringResource(
                            id = if (isVisible) {
                                ResR.string.login_passport_number_hide
                            } else {
                                ResR.string.login_passport_number_show
                            },
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
        error?.let { FieldError(messageRes = it.messageRes()) }
    }
}

private fun PassportNumberError.messageRes(): Int = when (this) {
    PassportNumberError.Empty -> ResR.string.login_error_passport_empty
    PassportNumberError.TooShort -> ResR.string.login_error_passport_too_short
    PassportNumberError.InvalidCharacters -> ResR.string.login_error_passport_invalid_characters
}
