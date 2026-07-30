package com.example.test_ai_project.auth.domain.model

/**
 * Why an entered credential was rejected before it ever reached the vault.
 *
 * Enums, not messages: the wording is a UI concern, and `:core:model` has no resources.
 */
enum class DateOfBirthError {
    /** Fewer than eight digits entered. */
    Incomplete,

    /** Well-formed but not a real date — 31/02, or a year before 1900. */
    NotARealDate,

    InFuture,
}

enum class PassportNumberError {
    Empty,

    /** Shorter than the ICAO minimum of six characters. */
    TooShort,

    /** Passport numbers are letters and digits only. */
    InvalidCharacters,
}
