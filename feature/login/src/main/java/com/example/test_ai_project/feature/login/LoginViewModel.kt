package com.example.test_ai_project.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test_ai_project.core.domain.usecase.AuthenticateLocallyUseCase
import com.example.test_ai_project.core.domain.usecase.CredentialsValidation
import com.example.test_ai_project.core.domain.usecase.ValidateCredentialsUseCase
import com.example.test_ai_project.core.model.AuthFailure
import com.example.test_ai_project.core.model.AuthOutcome
import com.example.test_ai_project.core.model.VaultCredentials
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the form and runs the submission. Validation rules and the vault check both live
 * in `:core:domain` — this class decides *when* they run and what the screen then shows.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val validateCredentials: ValidateCredentialsUseCase,
    private val authenticateLocally: AuthenticateLocallyUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Accepts anything and keeps only digits, so a paste of `01/02/1990` behaves the same
     * as typing it.
     */
    fun onDateOfBirthChange(input: String) {
        val digits = input.filter(Char::isDigit).take(DATE_DIGITS)
        _uiState.update {
            // Editing clears the complaint about what was edited: an error that outlives
            // the fix reads as the field still being wrong.
            it.copy(dateOfBirthDigits = digits, dateOfBirthError = null, authFailure = null)
        }
    }

    /**
     * Punctuation is *not* stripped here — the user is told that passport numbers are
     * letters and digits only rather than having characters silently vanish as they type.
     */
    fun onPassportNumberChange(input: String) {
        val normalised = input.take(MAX_PASSPORT_LENGTH).uppercase()
        _uiState.update {
            it.copy(passportNumber = normalised, passportNumberError = null, authFailure = null)
        }
    }

    fun onPassportNumberVisibilityToggle() {
        _uiState.update { it.copy(isPassportNumberVisible = !it.isPassportNumberVisible) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        when (
            val validation = validateCredentials(
                dateOfBirthDigits = state.dateOfBirthDigits,
                passportNumber = state.passportNumber,
            )
        ) {
            is CredentialsValidation.Invalid -> _uiState.update {
                it.copy(
                    dateOfBirthError = validation.dateOfBirthError,
                    passportNumberError = validation.passportNumberError,
                    authFailure = null,
                )
            }

            is CredentialsValidation.Valid -> authenticate(validation.credentials)
        }
    }

    private fun authenticate(credentials: VaultCredentials) {
        // Set before launching, not inside the coroutine: on a dispatcher that does not
        // run eagerly, a second tap would otherwise see `isSubmitting == false` and start
        // a second attempt.
        _uiState.update {
            it.copy(
                isSubmitting = true,
                dateOfBirthError = null,
                passportNumberError = null,
                authFailure = null,
            )
        }

        viewModelScope.launch {
            // A thrown exception is a vault problem, not a wrong passport number — the two
            // must not be reported to the user with the same message.
            val outcome = runCatching { authenticateLocally(credentials) }
                .getOrElse { AuthOutcome.Failed(AuthFailure.VaultUnavailable) }

            _uiState.update { state ->
                when (outcome) {
                    AuthOutcome.Success -> state.copy(
                        isSubmitting = false,
                        isAuthenticated = true,
                    )

                    is AuthOutcome.Failed -> state.copy(
                        isSubmitting = false,
                        authFailure = outcome.reason,
                    )
                }
            }
        }
    }

    internal companion object {
        const val DATE_DIGITS = 8

        /** ICAO caps machine-readable passport numbers at nine characters. */
        const val MAX_PASSPORT_LENGTH = 9
    }
}
