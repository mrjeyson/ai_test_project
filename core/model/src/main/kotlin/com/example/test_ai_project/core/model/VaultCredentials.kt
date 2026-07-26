package com.example.test_ai_project.core.model

/**
 * What the user proves their identity with. Only ever constructed from validated input,
 * so anything downstream can trust the shape without re-checking it.
 */
data class VaultCredentials(
    val dateOfBirth: CalendarDate,
    val passportNumber: String,
)

/** The result of a local authentication attempt. */
sealed interface AuthOutcome {

    data object Success : AuthOutcome

    data class Failed(val reason: AuthFailure) : AuthOutcome
}

enum class AuthFailure {
    /** The details did not match the credential enrolled on this device. */
    CredentialsRejected,

    /** Local secure storage could not be reached — retrying is worthwhile. */
    VaultUnavailable,
}
