package com.example.test_ai_project.auth.domain.model


/**
 * What the user proves their identity with. Only ever constructed from validated input,
 * so anything downstream can trust the shape without re-checking it.
 */
data class VaultCredentials(
    val dateOfBirth: CalendarDate,
    val passportNumber: String,
)
