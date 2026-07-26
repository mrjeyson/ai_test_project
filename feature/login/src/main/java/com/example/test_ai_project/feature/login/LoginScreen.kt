package com.example.test_ai_project.feature.login

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.test_ai_project.core.model.AuthFailure
import com.example.test_ai_project.core.model.DateOfBirthError
import com.example.test_ai_project.core.model.PassportNumberError
import com.example.test_ai_project.core.ui.R as UiR
import com.example.test_ai_project.core.ui.component.BrandLogo
import com.example.test_ai_project.core.ui.component.BrandWordmark
import com.example.test_ai_project.core.ui.component.VaultPrimaryButton
import com.example.test_ai_project.core.ui.component.VaultTextField
import com.example.test_ai_project.core.ui.theme.AppTheme
import com.example.test_ai_project.core.ui.theme.VaultLabel
import java.util.Calendar
import java.util.TimeZone

/**
 * Stateful entry point: the only place in this file that touches Hilt or the ViewModel.
 * Keeping it separate from [LoginScreen] is what makes the screen previewable and
 * testable without a DI graph.
 */
@Composable
fun LoginRoute(
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isAuthenticated = uiState.isAuthenticated
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) onAuthenticated()
    }

    LoginScreen(
        uiState = uiState,
        onDateOfBirthChange = viewModel::onDateOfBirthChange,
        onPassportNumberChange = viewModel::onPassportNumberChange,
        onPassportNumberVisibilityToggle = viewModel::onPassportNumberVisibilityToggle,
        onSubmit = viewModel::onSubmit,
        onSupportClick = { context.openLink(R.string.login_support_url) },
        onWhitepaperClick = { context.openLink(R.string.login_whitepaper_url) },
        modifier = modifier,
    )
}

/** Stateless and side-effect free — driven entirely by its parameters. */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onDateOfBirthChange: (String) -> Unit,
    onPassportNumberChange: (String) -> Unit,
    onPassportNumberVisibilityToggle: () -> Unit,
    onSubmit: () -> Unit,
    onSupportClick: () -> Unit,
    onWhitepaperClick: () -> Unit,
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
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        BrandLogo(size = 56.dp, tileColor = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(14.dp))
        BrandWordmark()
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(id = R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(22.dp))

        CredentialsCard(
            uiState = uiState,
            onDateOfBirthChange = onDateOfBirthChange,
            onPassportNumberChange = onPassportNumberChange,
            onPassportNumberVisibilityToggle = onPassportNumberVisibilityToggle,
            onSubmit = onSubmit,
        )

        Spacer(modifier = Modifier.height(26.dp))

        TrustChip()

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(id = R.string.login_privacy_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FooterLink(text = stringResource(id = R.string.login_support), onClick = onSupportClick)
            Spacer(modifier = Modifier.width(12.dp))
            FooterLink(
                text = stringResource(id = R.string.login_whitepaper),
                onClick = onWhitepaperClick,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CredentialsCard(
    uiState: LoginUiState,
    onDateOfBirthChange: (String) -> Unit,
    onPassportNumberChange: (String) -> Unit,
    onPassportNumberVisibilityToggle: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // rememberSaveable, not remember: a rotation with the picker open should not drop it.
    var isDatePickerVisible by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            FieldLabel(
                iconRes = R.drawable.ic_calendar,
                label = stringResource(id = R.string.login_date_of_birth_label),
            )
            Spacer(modifier = Modifier.height(8.dp))
            VaultTextField(
                value = uiState.dateOfBirthDigits,
                onValueChange = onDateOfBirthChange,
                placeholder = stringResource(id = R.string.login_date_of_birth_placeholder),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                visualTransformation = remember { DateOfBirthVisualTransformation() },
                isError = uiState.dateOfBirthError != null,
                trailing = {
                    IconButton(
                        onClick = { isDatePickerVisible = true },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_calendar),
                            contentDescription = stringResource(
                                id = R.string.login_date_of_birth_picker,
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
            uiState.dateOfBirthError?.let { FieldError(messageRes = it.messageRes()) }

            Spacer(modifier = Modifier.height(18.dp))

            FieldLabel(
                iconRes = R.drawable.ic_passport,
                label = stringResource(id = R.string.login_passport_number_label),
            )
            Spacer(modifier = Modifier.height(8.dp))
            VaultTextField(
                value = uiState.passportNumber,
                onValueChange = onPassportNumberChange,
                placeholder = stringResource(id = R.string.login_passport_number_placeholder),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                visualTransformation = if (uiState.isPassportNumberVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                isError = uiState.passportNumberError != null,
                trailing = {
                    IconButton(
                        onClick = onPassportNumberVisibilityToggle,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (uiState.isPassportNumberVisible) {
                                    R.drawable.ic_visibility_off
                                } else {
                                    R.drawable.ic_visibility
                                },
                            ),
                            contentDescription = stringResource(
                                id = if (uiState.isPassportNumberVisible) {
                                    R.string.login_passport_number_hide
                                } else {
                                    R.string.login_passport_number_show
                                },
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
            uiState.passportNumberError?.let { FieldError(messageRes = it.messageRes()) }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_lock),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.login_encrypted_at_rest),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // A vault-level failure belongs to the form, not to either field.
            uiState.authFailure?.let { FieldError(messageRes = it.messageRes()) }

            Spacer(modifier = Modifier.height(18.dp))

            VaultPrimaryButton(
                text = stringResource(id = R.string.login_continue),
                onClick = onSubmit,
                enabled = uiState.canSubmit,
                isLoading = uiState.isSubmitting,
                trailing = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_forward),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }

    if (isDatePickerVisible) {
        DateOfBirthPicker(
            onDateSelected = onDateOfBirthChange,
            onDismiss = { isDatePickerVisible = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateOfBirthPicker(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        yearRange = EARLIEST_YEAR..Calendar.getInstance().get(Calendar.YEAR),
        selectableDates = PastAndPresentDates,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onDateSelected(it.toDateOfBirthDigits()) }
                    onDismiss()
                },
            ) {
                Text(text = stringResource(id = R.string.login_date_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.login_date_picker_dismiss))
            }
        },
    ) {
        DatePicker(state = state)
    }
}

/** A future date of birth is never valid, so it is not offered in the first place. */
@OptIn(ExperimentalMaterial3Api::class)
private object PastAndPresentDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis <= System.currentTimeMillis()
}

/**
 * The picker reports UTC midnight, so the calendar fields have to be read back in UTC —
 * doing it in the device zone shifts the date by one day west of Greenwich.
 */
private fun Long.toDateOfBirthDigits(): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).also {
        it.timeInMillis = this
    }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = calendar.get(Calendar.MONTH) + 1
    val year = calendar.get(Calendar.YEAR)
    return "%02d%02d%04d".format(day, month, year)
}

@Composable
private fun FieldLabel(
    @androidx.annotation.DrawableRes iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = iconRes),
            // Decorative: the label next to it already says what the field is.
            contentDescription = null,
            tint = VaultLabel,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = VaultLabel,
        )
    }
}

@Composable
private fun FieldError(
    @StringRes messageRes: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(id = messageRes),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier.padding(top = 6.dp),
    )
}

@Composable
private fun TrustChip(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = UiR.drawable.ic_shield),
                contentDescription = null,
                tint = VaultLabel,
                modifier = Modifier.size(13.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.login_trust_chip),
                style = MaterialTheme.typography.labelMedium,
                color = VaultLabel,
            )
        }
    }
}

@Composable
private fun FooterLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A TextButton rather than clickable text: it comes with the 48dp touch target and
    // the pressed state that a bare Text does not.
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun Context.openLink(@StringRes urlRes: Int) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(urlRes)))
    // A device with no browser is unusual but real; crashing over a footer link is not
    // an acceptable outcome.
    runCatching { startActivity(intent) }.onFailure {
        Toast.makeText(this, R.string.login_no_browser, Toast.LENGTH_SHORT).show()
    }
}

@StringRes
private fun DateOfBirthError.messageRes(): Int = when (this) {
    DateOfBirthError.Incomplete -> R.string.login_error_date_incomplete
    DateOfBirthError.NotARealDate -> R.string.login_error_date_not_real
    DateOfBirthError.InFuture -> R.string.login_error_date_in_future
}

@StringRes
private fun PassportNumberError.messageRes(): Int = when (this) {
    PassportNumberError.Empty -> R.string.login_error_passport_empty
    PassportNumberError.TooShort -> R.string.login_error_passport_too_short
    PassportNumberError.InvalidCharacters -> R.string.login_error_passport_invalid_characters
}

@StringRes
private fun AuthFailure.messageRes(): Int = when (this) {
    AuthFailure.CredentialsRejected -> R.string.login_error_credentials_rejected
    AuthFailure.VaultUnavailable -> R.string.login_error_vault_unavailable
}

private const val EARLIEST_YEAR = 1900

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    AppTheme(darkTheme = false) {
        LoginScreen(
            uiState = LoginUiState(
                dateOfBirthDigits = "01021990",
                passportNumber = "AB123456",
            ),
            onDateOfBirthChange = {},
            onPassportNumberChange = {},
            onPassportNumberVisibilityToggle = {},
            onSubmit = {},
            onSupportClick = {},
            onWhitepaperClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenErrorPreview() {
    AppTheme(darkTheme = false) {
        LoginScreen(
            uiState = LoginUiState(
                dateOfBirthDigits = "3102",
                passportNumber = "AB-12",
                dateOfBirthError = DateOfBirthError.Incomplete,
                passportNumberError = PassportNumberError.InvalidCharacters,
            ),
            onDateOfBirthChange = {},
            onPassportNumberChange = {},
            onPassportNumberVisibilityToggle = {},
            onSubmit = {},
            onSupportClick = {},
            onWhitepaperClick = {},
        )
    }
}
