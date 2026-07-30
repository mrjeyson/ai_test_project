package com.example.test_ai_project.auth.presentation.login.contract

import com.example.test_ai_project.auth.domain.model.DateOfBirthError
import com.example.test_ai_project.auth.domain.model.PassportNumberError
import com.example.test_ai_project.resource.base.UiEffect
import com.example.test_ai_project.resource.base.UiEvent
import com.example.test_ai_project.resource.base.UiState

/**
 * Everything the login screen renders, and nothing else.
 *
 * A single data class rather than a sealed hierarchy: unlike a list screen, a form is
 * genuinely all of its fields at once — text, per-field errors, and submission progress
 * coexist.
 *
 * Note what is *not* here: the vault-level failure. A rejected credential is a one-shot
 * event, so it leaves as a [LoginEffect] and is drawn by the app's single toast host.
 * Per-field errors do stay in state, because they persist until the user edits the field
 * they belong to.
 */
data class LoginState(
    /** The eight `ddMMyyyy` digits with no separators; the field inserts slashes visually. */
    val dateOfBirthDigits: String = "",
    val passportNumber: String = "",
    val isPassportNumberVisible: Boolean = false,
    val dateOfBirthError: DateOfBirthError? = null,
    val passportNumberError: PassportNumberError? = null,
    val isSubmitting: Boolean = false,
) : UiState {

    /**
     * Only emptiness gates the button. Deeper rules are checked on submit, so the user
     * gets a reason to read rather than a button that silently refuses to work.
     */
    val canSubmit: Boolean
        get() = dateOfBirthDigits.isNotEmpty() && passportNumber.isNotBlank() && !isSubmitting
}

sealed interface LoginEvent : UiEvent {
    data class DateOfBirthChanged(val input: String) : LoginEvent
    data class PassportNumberChanged(val input: String) : LoginEvent
    data object PassportNumberVisibilityToggled : LoginEvent
    data object Submitted : LoginEvent
}

sealed interface LoginEffect : UiEffect {

    /** Authentication succeeded; the root graph decides what comes next. */
    data object Authenticated : LoginEffect

    /**
     * Carries a resolved string rather than a resource id: the ViewModel prefers a
     * server-supplied message when there is one, and only falls back to a localized
     * default, so the choice is already made by the time it gets here.
     */
    data class ShowError(val message: LoginErrorMessage) : LoginEffect
}

/** Either a message the backend supplied, or one of ours to look up. */
sealed interface LoginErrorMessage {
    data class Literal(val text: String) : LoginErrorMessage
    data class Resource(val id: Int) : LoginErrorMessage
}
