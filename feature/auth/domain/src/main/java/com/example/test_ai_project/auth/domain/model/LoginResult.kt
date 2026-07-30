package com.example.test_ai_project.auth.domain.model

/**
 * The outcome of an authentication attempt, in the login feature's own vocabulary.
 *
 * The data layer maps whatever it deals in — a keystore exception, an [AppError] once
 * there is a backend — onto [LoginError] before returning it, so nothing above this point
 * has to know how the check was performed.
 */
sealed interface LoginResult {

    data object Success : LoginResult

    /**
     * [message] carries a server-supplied explanation when there is one. A caller prefers
     * it over the localized fallback for [error], because a backend that bothered to
     * explain itself is more specific than anything the app can say generically.
     */
    data class Failed(
        val error: LoginError,
        val message: String? = null,
    ) : LoginResult
}

enum class LoginError {
    /** The details did not match the credential enrolled on this device. */
    CredentialsRejected,

    /** Local secure storage could not be reached — retrying is worthwhile. */
    VaultUnavailable,
}
