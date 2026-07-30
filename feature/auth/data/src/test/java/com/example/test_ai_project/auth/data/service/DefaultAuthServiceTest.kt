package com.example.test_ai_project.auth.data.service

import com.example.test_ai_project.auth.domain.model.CalendarDate
import com.example.test_ai_project.auth.domain.service.DateProvider
import com.example.test_ai_project.auth.data.local.VaultCredentialSource
import com.example.test_ai_project.auth.domain.model.CredentialsValidation
import com.example.test_ai_project.auth.domain.model.DateOfBirthError
import com.example.test_ai_project.auth.domain.model.LoginError
import com.example.test_ai_project.auth.domain.model.LoginResult
import com.example.test_ai_project.auth.domain.model.PassportNumberError
import com.example.test_ai_project.auth.domain.model.VaultCredentials
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The validation half is pure, so it is tested directly. The vault half is faked, because
 * that is the part with I/O.
 */
class DefaultAuthServiceTest {

    private val credentialSource = mockk<VaultCredentialSource>()

    private val service = DefaultAuthService(
        dateProvider = object : DateProvider {
            override fun today(): CalendarDate = CalendarDate(2026, 7, 25)
        },
        credentialSource = credentialSource,
    )

    @Test
    fun `a well-formed form validates and normalizes the passport number`() {
        val result = service.validate(dateOfBirthDigits = "01021990", passportNumber = " ab123456 ")

        assertThat(result).isEqualTo(
            CredentialsValidation.Valid(
                VaultCredentials(CalendarDate(1990, 2, 1), "AB123456"),
            ),
        )
    }

    @Test
    fun `both fields are reported at once`() {
        val result = service.validate(dateOfBirthDigits = "0102", passportNumber = "AB-12")

        assertThat(result).isEqualTo(
            CredentialsValidation.Invalid(
                dateOfBirthError = DateOfBirthError.Incomplete,
                passportNumberError = PassportNumberError.InvalidCharacters,
            ),
        )
    }

    @Test
    fun `a date that does not exist is rejected as not real`() {
        val result = service.validate(dateOfBirthDigits = "31021990", passportNumber = "AB123456")

        assertThat((result as CredentialsValidation.Invalid).dateOfBirthError)
            .isEqualTo(DateOfBirthError.NotARealDate)
    }

    @Test
    fun `29 February is accepted in a leap year`() {
        val result = service.validate(dateOfBirthDigits = "29022000", passportNumber = "AB123456")

        assertThat(result).isInstanceOf(CredentialsValidation.Valid::class.java)
    }

    @Test
    fun `a date after today is rejected as in the future`() {
        val result = service.validate(dateOfBirthDigits = "26072026", passportNumber = "AB123456")

        assertThat((result as CredentialsValidation.Invalid).dateOfBirthError)
            .isEqualTo(DateOfBirthError.InFuture)
    }

    @Test
    fun `a passport number shorter than the ICAO minimum is rejected`() {
        val result = service.validate(dateOfBirthDigits = "01021990", passportNumber = "AB123")

        assertThat((result as CredentialsValidation.Invalid).passportNumberError)
            .isEqualTo(PassportNumberError.TooShort)
    }

    @Test
    fun `a matching credential authenticates`() = runTest {
        coEvery { credentialSource.matches(any()) } returns true

        val result = service.authenticate(VaultCredentials(CalendarDate(1990, 2, 1), "AB123456"))

        assertThat(result).isEqualTo(LoginResult.Success)
    }

    @Test
    fun `a non-matching credential is reported as rejected`() = runTest {
        coEvery { credentialSource.matches(any()) } returns false

        val result = service.authenticate(VaultCredentials(CalendarDate(1990, 2, 1), "AB123456"))

        assertThat(result).isEqualTo(LoginResult.Failed(LoginError.CredentialsRejected))
    }

    @Test
    fun `a thrown failure is reported as the vault being unavailable, not a bad password`() =
        runTest {
            coEvery { credentialSource.matches(any()) } throws IllegalStateException("keystore")

            val result =
                service.authenticate(VaultCredentials(CalendarDate(1990, 2, 1), "AB123456"))

            assertThat(result).isEqualTo(LoginResult.Failed(LoginError.VaultUnavailable))
        }
}
