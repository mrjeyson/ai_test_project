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
import com.example.test_ai_project.auth.domain.service.AuthService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The login feature's logic, in one place.
 *
 * Validation is pure and lives here rather than in the ViewModel so that the same rules
 * apply whether the input came from the login screen, a deep link, or a test.
 */
@Singleton
class DefaultAuthService @Inject constructor(
    private val dateProvider: DateProvider,
    private val credentialSource: VaultCredentialSource,
) : AuthService {

    override fun validate(
        dateOfBirthDigits: String,
        passportNumber: String,
    ): CredentialsValidation {
        val date = parseDateOfBirth(dateOfBirthDigits)
        val passportError = validatePassportNumber(passportNumber)

        val dateError = (date as? DateParse.Failed)?.error
        if (dateError != null || passportError != null) {
            return CredentialsValidation.Invalid(
                dateOfBirthError = dateError,
                passportNumberError = passportError,
            )
        }

        return CredentialsValidation.Valid(
            VaultCredentials(
                dateOfBirth = (date as DateParse.Parsed).value,
                passportNumber = passportNumber.trim().uppercase(),
            ),
        )
    }

    /**
     * A thrown exception is a vault problem, not a wrong passport number — the two must
     * not reach the user as the same message, so they are mapped to different
     * [LoginError]s here rather than being collapsed into one failure upstream.
     */
    override suspend fun authenticate(credentials: VaultCredentials): LoginResult =
        runCatching { credentialSource.matches(credentials) }
            .fold(
                onSuccess = { matched ->
                    if (matched) {
                        LoginResult.Success
                    } else {
                        LoginResult.Failed(LoginError.CredentialsRejected)
                    }
                },
                onFailure = { LoginResult.Failed(LoginError.VaultUnavailable) },
            )

    private fun parseDateOfBirth(digits: String): DateParse {
        if (digits.length < DATE_DIGITS || digits.any { !it.isDigit() }) {
            return DateParse.Failed(DateOfBirthError.Incomplete)
        }

        val day = digits.substring(0, 2).toInt()
        val month = digits.substring(2, 4).toInt()
        val year = digits.substring(4, 8).toInt()

        if (year < EARLIEST_YEAR || month !in 1..12 || day < 1 || day > daysIn(month, year)) {
            return DateParse.Failed(DateOfBirthError.NotARealDate)
        }

        val date = CalendarDate(year = year, month = month, day = day)
        if (date > dateProvider.today()) {
            return DateParse.Failed(DateOfBirthError.InFuture)
        }

        return DateParse.Parsed(date)
    }

    private fun validatePassportNumber(input: String): PassportNumberError? {
        val trimmed = input.trim()
        return when {
            trimmed.isEmpty() -> PassportNumberError.Empty
            !trimmed.all { it.isLetterOrDigit() } -> PassportNumberError.InvalidCharacters
            trimmed.length < MIN_PASSPORT_LENGTH -> PassportNumberError.TooShort
            else -> null
        }
    }

    private fun daysIn(month: Int, year: Int): Int = when (month) {
        2 -> if (isLeapYear(year)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    private fun isLeapYear(year: Int): Boolean =
        year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

    private sealed interface DateParse {
        data class Parsed(val value: CalendarDate) : DateParse
        data class Failed(val error: DateOfBirthError) : DateParse
    }

    private companion object {
        const val DATE_DIGITS = 8
        const val EARLIEST_YEAR = 1900
        const val MIN_PASSPORT_LENGTH = 6
    }
}
