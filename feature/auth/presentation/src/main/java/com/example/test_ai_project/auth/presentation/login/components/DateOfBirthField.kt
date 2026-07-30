package com.example.test_ai_project.auth.presentation.login.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.test_ai_project.auth.domain.model.DateOfBirthError
import com.example.test_ai_project.auth.presentation.R
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.component.AppIcon
import com.example.test_ai_project.resource.component.AppTextField
import com.example.test_ai_project.resource.theme.scaled
import com.example.test_ai_project.resource.theme.spacing

/** The date-of-birth field, its label, its picker affordance and its error line. */
@Composable
internal fun DateOfBirthField(
    digits: String,
    error: DateOfBirthError?,
    onDigitsChange: (String) -> Unit,
    onPickerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        FieldLabel(
            iconRes = R.drawable.ic_calendar,
            label = stringResource(id = ResR.string.login_date_of_birth_label),
        )
        Spacer(modifier = Modifier.height(spacing.small))
        AppTextField(
            value = digits,
            onValueChange = onDigitsChange,
            placeholder = stringResource(id = ResR.string.login_date_of_birth_placeholder),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
            visualTransformation = remember { DateOfBirthVisualTransformation() },
            isError = error != null,
            trailing = {
                IconButton(onClick = onPickerClick, modifier = Modifier.size(40.scaled)) {
                    AppIcon(
                        id = R.drawable.ic_calendar,
                        contentDescription = stringResource(
                            id = ResR.string.login_date_of_birth_picker,
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
        error?.let { FieldError(messageRes = it.messageRes()) }
    }
}

private fun DateOfBirthError.messageRes(): Int = when (this) {
    DateOfBirthError.Incomplete -> ResR.string.login_error_date_incomplete
    DateOfBirthError.NotARealDate -> ResR.string.login_error_date_not_real
    DateOfBirthError.InFuture -> ResR.string.login_error_date_in_future
}
