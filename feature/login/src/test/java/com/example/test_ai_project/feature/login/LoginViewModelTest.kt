package com.example.test_ai_project.feature.login

import com.example.test_ai_project.core.domain.repository.AuthRepository
import com.example.test_ai_project.core.domain.time.DateProvider
import com.example.test_ai_project.core.domain.usecase.AuthenticateLocallyUseCase
import com.example.test_ai_project.core.domain.usecase.ValidateCredentialsUseCase
import com.example.test_ai_project.core.model.AuthFailure
import com.example.test_ai_project.core.model.AuthOutcome
import com.example.test_ai_project.core.model.CalendarDate
import com.example.test_ai_project.core.model.DateOfBirthError
import com.example.test_ai_project.core.model.PassportNumberError
import com.example.test_ai_project.core.model.VaultCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The real [ValidateCredentialsUseCase] is used rather than a fake — it is pure, and
 * doubling it would only test that the double agrees with itself. The repository, which
 * is the part with I/O, is the thing that gets faked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val authRepository = FakeAuthRepository()
    private val validate = ValidateCredentialsUseCase(
        dateProvider = object : DateProvider {
            override fun today(): CalendarDate = CalendarDate(2026, 7, 25)
        },
    )

    private fun viewModel() = LoginViewModel(
        validateCredentials = validate,
        authenticateLocally = AuthenticateLocallyUseCase(authRepository),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `keeps only digits from date of birth input`() {
        val viewModel = viewModel()

        viewModel.onDateOfBirthChange("01/02/1990")

        assertEquals("01021990", viewModel.uiState.value.dateOfBirthDigits)
    }

    @Test
    fun `caps date of birth at eight digits`() {
        val viewModel = viewModel()

        viewModel.onDateOfBirthChange("010219901")

        assertEquals("01021990", viewModel.uiState.value.dateOfBirthDigits)
    }

    @Test
    fun `uppercases the passport number and caps its length`() {
        val viewModel = viewModel()

        viewModel.onPassportNumberChange("ab1234567890")

        assertEquals("AB1234567", viewModel.uiState.value.passportNumber)
    }

    @Test
    fun `cannot submit until both fields have content`() {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onDateOfBirthChange("01021990")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onPassportNumberChange("AB123456")
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `submitting an invalid form reports both fields and never reaches the vault`() = runTest {
        val viewModel = viewModel()
        viewModel.onDateOfBirthChange("0102")
        viewModel.onPassportNumberChange("AB-12")

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DateOfBirthError.Incomplete, state.dateOfBirthError)
        assertEquals(PassportNumberError.InvalidCharacters, state.passportNumberError)
        assertFalse(state.isAuthenticated)
        assertEquals(0, authRepository.attempts)
    }

    @Test
    fun `editing a field clears its error`() = runTest {
        val viewModel = viewModel()
        viewModel.onDateOfBirthChange("0102")
        viewModel.onPassportNumberChange("AB123456")
        viewModel.onSubmit()
        advanceUntilIdle()
        assertEquals(DateOfBirthError.Incomplete, viewModel.uiState.value.dateOfBirthError)

        viewModel.onDateOfBirthChange("01021990")

        assertNull(viewModel.uiState.value.dateOfBirthError)
    }

    @Test
    fun `a valid form authenticates and reports success`() = runTest {
        val viewModel = viewModel()
        viewModel.onDateOfBirthChange("01021990")
        viewModel.onPassportNumberChange("AB123456")

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAuthenticated)
        assertFalse(state.isSubmitting)
        assertEquals(
            VaultCredentials(CalendarDate(1990, 2, 1), "AB123456"),
            authRepository.lastCredentials,
        )
    }

    @Test
    fun `a rejection surfaces a form error and leaves the user on the screen`() = runTest {
        authRepository.outcome = AuthOutcome.Failed(AuthFailure.CredentialsRejected)
        val viewModel = viewModel()
        viewModel.onDateOfBirthChange("01021990")
        viewModel.onPassportNumberChange("AB123456")

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AuthFailure.CredentialsRejected, state.authFailure)
        assertFalse(state.isAuthenticated)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `a thrown failure is reported as the vault being unavailable`() = runTest {
        authRepository.throwOnAuthenticate = true
        val viewModel = viewModel()
        viewModel.onDateOfBirthChange("01021990")
        viewModel.onPassportNumberChange("AB123456")

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthFailure.VaultUnavailable, viewModel.uiState.value.authFailure)
    }

    @Test
    fun `a second tap while submitting does not authenticate twice`() = runTest {
        val viewModel = viewModel()
        viewModel.onDateOfBirthChange("01021990")
        viewModel.onPassportNumberChange("AB123456")

        viewModel.onSubmit()
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(1, authRepository.attempts)
    }

    @Test
    fun `visibility toggle flips`() {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.isPassportNumberVisible)

        viewModel.onPassportNumberVisibilityToggle()

        assertTrue(viewModel.uiState.value.isPassportNumberVisible)
    }
}

private class FakeAuthRepository : AuthRepository {

    var outcome: AuthOutcome = AuthOutcome.Success
    var throwOnAuthenticate = false
    var attempts = 0
        private set
    var lastCredentials: VaultCredentials? = null
        private set

    override suspend fun authenticate(credentials: VaultCredentials): AuthOutcome {
        attempts++
        lastCredentials = credentials
        if (throwOnAuthenticate) error("keystore unavailable")
        return outcome
    }
}
