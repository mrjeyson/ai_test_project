package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.time.DateProvider
import com.example.test_ai_project.core.model.CalendarDate
import com.example.test_ai_project.core.model.DateOfBirthError
import com.example.test_ai_project.core.model.PassportNumberError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure JVM — no Robolectric, no instrumentation. The fixed [DateProvider] is what makes
 * the "in the future" rule assertable at all.
 */
class ValidateCredentialsUseCaseTest {

    private val today = CalendarDate(year = 2026, month = 7, day = 25)
    private val validate = ValidateCredentialsUseCase(
        dateProvider = object : DateProvider {
            override fun today(): CalendarDate = today
        },
    )

    @Test
    fun `accepts a complete form and normalises the passport number`() {
        val result = validate(dateOfBirthDigits = "01021990", passportNumber = " ab123456 ")

        val valid = result as CredentialsValidation.Valid
        assertEquals(CalendarDate(1990, 2, 1), valid.credentials.dateOfBirth)
        assertEquals("AB123456", valid.credentials.passportNumber)
    }

    @Test
    fun `reports both fields at once`() {
        val result = validate(dateOfBirthDigits = "0102", passportNumber = "")

        val invalid = result as CredentialsValidation.Invalid
        assertEquals(DateOfBirthError.Incomplete, invalid.dateOfBirthError)
        assertEquals(PassportNumberError.Empty, invalid.passportNumberError)
    }

    @Test
    fun `rejects a day that does not exist in that month`() {
        assertEquals(
            DateOfBirthError.NotARealDate,
            invalidDate(digits = "29021990"),
        )
    }

    @Test
    fun `accepts the 29th of February in a leap year`() {
        val result = validate(dateOfBirthDigits = "29022024", passportNumber = "AB123456")

        assertEquals(
            CalendarDate(2024, 2, 29),
            (result as CredentialsValidation.Valid).credentials.dateOfBirth,
        )
    }

    @Test
    fun `rejects a date after today`() {
        assertEquals(DateOfBirthError.InFuture, invalidDate(digits = "26072026"))
    }

    @Test
    fun `accepts today itself`() {
        val result = validate(dateOfBirthDigits = "25072026", passportNumber = "AB123456")

        assertNull((result as? CredentialsValidation.Invalid)?.dateOfBirthError)
    }

    @Test
    fun `rejects a year before nineteen hundred`() {
        assertEquals(DateOfBirthError.NotARealDate, invalidDate(digits = "01011899"))
    }

    @Test
    fun `rejects a passport number below the ICAO minimum length`() {
        val result = validate(dateOfBirthDigits = "01021990", passportNumber = "AB12")

        assertEquals(
            PassportNumberError.TooShort,
            (result as CredentialsValidation.Invalid).passportNumberError,
        )
    }

    @Test
    fun `rejects punctuation in a passport number`() {
        val result = validate(dateOfBirthDigits = "01021990", passportNumber = "AB-123456")

        assertEquals(
            PassportNumberError.InvalidCharacters,
            (result as CredentialsValidation.Invalid).passportNumberError,
        )
    }

    private fun invalidDate(digits: String): DateOfBirthError? {
        val result = validate(dateOfBirthDigits = digits, passportNumber = "AB123456")
        return (result as CredentialsValidation.Invalid).dateOfBirthError
    }
}
