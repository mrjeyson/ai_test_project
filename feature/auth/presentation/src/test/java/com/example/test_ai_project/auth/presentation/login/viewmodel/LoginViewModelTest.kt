package com.example.test_ai_project.auth.presentation.login.viewmodel

import app.cash.turbine.test
import com.example.test_ai_project.auth.domain.model.CalendarDate
import com.example.test_ai_project.auth.domain.model.CredentialsValidation
import com.example.test_ai_project.auth.domain.model.DateOfBirthError
import com.example.test_ai_project.auth.domain.model.LoginError
import com.example.test_ai_project.auth.domain.model.LoginResult
import com.example.test_ai_project.auth.domain.model.PassportNumberError
import com.example.test_ai_project.auth.domain.model.VaultCredentials
import com.example.test_ai_project.auth.domain.service.AuthService
import com.example.test_ai_project.auth.presentation.login.contract.LoginEffect
import com.example.test_ai_project.auth.presentation.login.contract.LoginErrorMessage
import com.example.test_ai_project.auth.presentation.login.contract.LoginEvent
import com.example.test_ai_project.auth.presentation.testing.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Drives the ViewModel through [LoginEvent]s and asserts on state and effects, which is
 * the whole of its contract.
 *
 * A `StandardTestDispatcher` rather than the rule's default: several of these tests care
 * about the in-flight `isSubmitting` state, which an unconfined dispatcher would run
 * straight past.
 */
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val authService = FakeAuthService()

    private fun viewModel() = LoginViewModel(authService)

    @Test
    fun `keeps only digits from date of birth input`() {
        val viewModel = viewModel()

        viewModel.onEvent(LoginEvent.DateOfBirthChanged("01/02/1990"))

        assertThat(viewModel.uiState.value.dateOfBirthDigits).isEqualTo("01021990")
    }

    @Test
    fun `caps date of birth at eight digits`() {
        val viewModel = viewModel()

        viewModel.onEvent(LoginEvent.DateOfBirthChanged("010219901"))

        assertThat(viewModel.uiState.value.dateOfBirthDigits).isEqualTo("01021990")
    }

    @Test
    fun `uppercases the passport number and caps its length`() {
        val viewModel = viewModel()

        viewModel.onEvent(LoginEvent.PassportNumberChanged("ab1234567890"))

        assertThat(viewModel.uiState.value.passportNumber).isEqualTo("AB1234567")
    }

    @Test
    fun `cannot submit until both fields have content`() {
        val viewModel = viewModel()
        assertThat(viewModel.uiState.value.canSubmit).isFalse()

        viewModel.onEvent(LoginEvent.DateOfBirthChanged("01021990"))
        assertThat(viewModel.uiState.value.canSubmit).isFalse()

        viewModel.onEvent(LoginEvent.PassportNumberChanged("AB123456"))
        assertThat(viewModel.uiState.value.canSubmit).isTrue()
    }

    @Test
    fun `submitting an invalid form reports both fields and never reaches the vault`() = runTest {
        authService.validation = CredentialsValidation.Invalid(
            dateOfBirthError = DateOfBirthError.Incomplete,
            passportNumberError = PassportNumberError.InvalidCharacters,
        )
        val viewModel = viewModel()
        viewModel.onEvent(LoginEvent.DateOfBirthChanged("0102"))
        viewModel.onEvent(LoginEvent.PassportNumberChanged("AB-12"))

        viewModel.onEvent(LoginEvent.Submitted)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.dateOfBirthError).isEqualTo(DateOfBirthError.Incomplete)
        assertThat(state.passportNumberError).isEqualTo(PassportNumberError.InvalidCharacters)
        assertThat(authService.attempts).isEqualTo(0)
    }

    @Test
    fun `editing a field clears its error`() = runTest {
        authService.validation = CredentialsValidation.Invalid(
            dateOfBirthError = DateOfBirthError.Incomplete,
            passportNumberError = null,
        )
        val viewModel = viewModel()
        viewModel.onEvent(LoginEvent.DateOfBirthChanged("0102"))
        viewModel.onEvent(LoginEvent.PassportNumberChanged("AB123456"))
        viewModel.onEvent(LoginEvent.Submitted)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.dateOfBirthError)
            .isEqualTo(DateOfBirthError.Incomplete)

        viewModel.onEvent(LoginEvent.DateOfBirthChanged("01021990"))

        assertThat(viewModel.uiState.value.dateOfBirthError).isNull()
    }

    @Test
    fun `a valid form authenticates and emits the authenticated effect`() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(LoginEvent.DateOfBirthChanged("01021990"))
            viewModel.onEvent(LoginEvent.PassportNumberChanged("AB123456"))
            viewModel.onEvent(LoginEvent.Submitted)
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(LoginEffect.Authenticated)
        }

        assertThat(viewModel.uiState.value.isSubmitting).isFalse()
        assertThat(authService.lastCredentials)
            .isEqualTo(VaultCredentials(CalendarDate(1990, 2, 1), "AB123456"))
    }

    @Test
    fun `a rejection surfaces an error effect and leaves the user on the screen`() = runTest {
        authService.result = LoginResult.Failed(LoginError.CredentialsRejected)
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(LoginEvent.DateOfBirthChanged("01021990"))
            viewModel.onEvent(LoginEvent.PassportNumberChanged("AB123456"))
            viewModel.onEvent(LoginEvent.Submitted)
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(LoginEffect.ShowError::class.java)
            assertThat((effect as LoginEffect.ShowError).message)
                .isInstanceOf(LoginErrorMessage.Resource::class.java)
        }

        assertThat(viewModel.uiState.value.isSubmitting).isFalse()
    }

    @Test
    fun `a message from the vault is preferred over the localized fallback`() = runTest {
        authService.result = LoginResult.Failed(
            error = LoginError.VaultUnavailable,
            message = "Secure element busy",
        )
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(LoginEvent.DateOfBirthChanged("01021990"))
            viewModel.onEvent(LoginEvent.PassportNumberChanged("AB123456"))
            viewModel.onEvent(LoginEvent.Submitted)
            advanceUntilIdle()

            val effect = awaitItem() as LoginEffect.ShowError
            assertThat(effect.message)
                .isEqualTo(LoginErrorMessage.Literal("Secure element busy"))
        }
    }

    @Test
    fun `a second tap while submitting does not authenticate twice`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(LoginEvent.DateOfBirthChanged("01021990"))
        viewModel.onEvent(LoginEvent.PassportNumberChanged("AB123456"))

        viewModel.onEvent(LoginEvent.Submitted)
        viewModel.onEvent(LoginEvent.Submitted)
        advanceUntilIdle()

        assertThat(authService.attempts).isEqualTo(1)
    }

    @Test
    fun `visibility toggle flips`() {
        val viewModel = viewModel()
        assertThat(viewModel.uiState.value.isPassportNumberVisible).isFalse()

        viewModel.onEvent(LoginEvent.PassportNumberVisibilityToggled)

        assertThat(viewModel.uiState.value.isPassportNumberVisible).isTrue()
    }
}

private class FakeAuthService : AuthService {

    var validation: CredentialsValidation = CredentialsValidation.Valid(
        VaultCredentials(CalendarDate(1990, 2, 1), "AB123456"),
    )
    var result: LoginResult = LoginResult.Success
    var attempts = 0
        private set
    var lastCredentials: VaultCredentials? = null
        private set

    override fun validate(
        dateOfBirthDigits: String,
        passportNumber: String,
    ): CredentialsValidation = validation

    override suspend fun authenticate(credentials: VaultCredentials): LoginResult {
        attempts++
        lastCredentials = credentials
        return result
    }
}
