package com.example.test_ai_project.auth.presentation.login.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.test_ai_project.auth.domain.model.CredentialsValidation
import com.example.test_ai_project.auth.domain.model.LoginError
import com.example.test_ai_project.auth.domain.model.LoginResult
import com.example.test_ai_project.auth.domain.model.VaultCredentials
import com.example.test_ai_project.auth.domain.service.AuthService
import com.example.test_ai_project.auth.presentation.login.contract.LoginEffect
import com.example.test_ai_project.auth.presentation.login.contract.LoginErrorMessage
import com.example.test_ai_project.auth.presentation.login.contract.LoginEvent
import com.example.test_ai_project.auth.presentation.login.contract.LoginState
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Holds the form and runs the submission.
 *
 * The validation rules and the vault check both live behind [AuthService] — this class
 * decides *when* they run and what the screen then shows.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authService: AuthService,
) : BaseViewModel<LoginState, LoginEvent, LoginEffect>(LoginState()) {

    override fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.DateOfBirthChanged -> onDateOfBirthChanged(event.input)
            is LoginEvent.PassportNumberChanged -> onPassportNumberChanged(event.input)
            LoginEvent.PassportNumberVisibilityToggled -> setState {
                copy(isPassportNumberVisible = !isPassportNumberVisible)
            }

            LoginEvent.Submitted -> onSubmit()
        }
    }

    /**
     * Accepts anything and keeps only digits, so a paste of `01/02/1990` behaves the same
     * as typing it.
     */
    private fun onDateOfBirthChanged(input: String) {
        val digits = input.filter(Char::isDigit).take(DATE_DIGITS)
        // Editing clears the complaint about what was edited: an error that outlives the
        // fix reads as the field still being wrong.
        setState { copy(dateOfBirthDigits = digits, dateOfBirthError = null) }
    }

    /**
     * Punctuation is *not* stripped here — the user is told that passport numbers are
     * letters and digits only rather than having characters silently vanish as they type.
     */
    private fun onPassportNumberChanged(input: String) {
        val normalised = input.take(MAX_PASSPORT_LENGTH).uppercase()
        setState { copy(passportNumber = normalised, passportNumberError = null) }
    }

    private fun onSubmit() {
        if (currentState.isSubmitting) return

        when (
            val validation = authService.validate(
                dateOfBirthDigits = currentState.dateOfBirthDigits,
                passportNumber = currentState.passportNumber,
            )
        ) {
            is CredentialsValidation.Invalid -> setState {
                copy(
                    dateOfBirthError = validation.dateOfBirthError,
                    passportNumberError = validation.passportNumberError,
                )
            }

            is CredentialsValidation.Valid -> authenticate(validation.credentials)
        }
    }

    private fun authenticate(credentials: VaultCredentials) {
        // Set before launching, not inside the coroutine: on a dispatcher that does not
        // run eagerly, a second tap would otherwise see `isSubmitting == false` and start
        // a second attempt.
        setState {
            copy(isSubmitting = true, dateOfBirthError = null, passportNumberError = null)
        }

        viewModelScope.launch {
            val result = authService.authenticate(credentials)
            setState { copy(isSubmitting = false) }

            when (result) {
                LoginResult.Success -> sendEffect(LoginEffect.Authenticated)
                is LoginResult.Failed -> sendEffect(
                    LoginEffect.ShowError(result.toMessage()),
                )
            }
        }
    }

    /** A message the vault supplied beats ours, because it is the more specific one. */
    private fun LoginResult.Failed.toMessage(): LoginErrorMessage =
        message?.let(LoginErrorMessage::Literal)
            ?: LoginErrorMessage.Resource(
                when (error) {
                    LoginError.CredentialsRejected -> ResR.string.login_error_credentials_rejected
                    LoginError.VaultUnavailable -> ResR.string.login_error_vault_unavailable
                },
            )

    internal companion object {
        const val DATE_DIGITS = 8

        /** ICAO caps machine-readable passport numbers at nine characters. */
        const val MAX_PASSPORT_LENGTH = 9
    }
}
