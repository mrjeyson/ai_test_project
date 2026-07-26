package com.example.test_ai_project.feature.login

import com.example.test_ai_project.core.model.AuthFailure
import com.example.test_ai_project.core.model.DateOfBirthError
import com.example.test_ai_project.core.model.PassportNumberError

/**
 * Everything the login screen needs to render, and nothing else.
 *
 * A single data class rather than a sealed hierarchy: unlike a list screen, a form is
 * genuinely all of its fields at once — text, per-field errors, and submission progress
 * coexist.
 */
data class LoginUiState(
    /** The eight `ddMMyyyy` digits with no separators; the field inserts slashes visually. */
    val dateOfBirthDigits: String = "",
    val passportNumber: String = "",
    val isPassportNumberVisible: Boolean = false,
    val dateOfBirthError: DateOfBirthError? = null,
    val passportNumberError: PassportNumberError? = null,
    /** A vault-level rejection, as opposed to a malformed field. */
    val authFailure: AuthFailure? = null,
    val isSubmitting: Boolean = false,
    val isAuthenticated: Boolean = false,
) {

    /**
     * Only emptiness gates the button. Deeper rules are checked on submit, so the user
     * gets a reason to read rather than a button that silently refuses to work.
     */
    val canSubmit: Boolean
        get() = dateOfBirthDigits.isNotEmpty() && passportNumber.isNotBlank() && !isSubmitting
}
