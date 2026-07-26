package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.time.DateProvider
import com.example.test_ai_project.core.model.CalendarDate
import com.example.test_ai_project.core.model.DateOfBirthError
import com.example.test_ai_project.core.model.PassportNumberError
import com.example.test_ai_project.core.model.VaultCredentials
import javax.inject.Inject

/** The outcome of checking a filled-in login form. */
sealed interface CredentialsValidation {

    data class Valid(val credentials: VaultCredentials) : CredentialsValidation

    /**
     * Both fields are reported at once — validating one at a time would make the user
     * submit twice to discover two problems.
     */
    data class Invalid(
        val dateOfBirthError: DateOfBirthError?,
        val passportNumberError: PassportNumberError?,
    ) : CredentialsValidation
}

/**
 * Turns raw form input into [VaultCredentials], or into the reasons it cannot be.
 *
 * Lives in `:core:domain` and is pure: the same rules apply whether the input came from
 * the login screen, a deep link, or a test.
 */
class ValidateCredentialsUseCase @Inject constructor(
    private val dateProvider: DateProvider,
) {

    /**
     * @param dateOfBirthDigits the eight digits `ddMMyyyy`, with no separators — the UI
     *   inserts the slashes visually, so the domain never has to strip them.
     */
    operator fun invoke(
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
