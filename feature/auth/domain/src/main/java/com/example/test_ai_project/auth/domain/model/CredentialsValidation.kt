package com.example.test_ai_project.auth.domain.model

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
