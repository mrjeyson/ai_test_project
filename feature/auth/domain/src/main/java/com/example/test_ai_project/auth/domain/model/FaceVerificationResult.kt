package com.example.test_ai_project.auth.domain.model

/**
 * The result of matching a well-aligned face against the enrolment on this device.
 *
 * The data layer maps whatever the matcher deals in onto [FaceVerificationError] before
 * returning it, so nothing above this point learns how the comparison was performed.
 */
sealed interface FaceVerificationResult {

    data object Verified : FaceVerificationResult

    /** [message] carries a matcher-supplied explanation when there is one. */
    data class Failed(
        val error: FaceVerificationError,
        val message: String? = null,
    ) : FaceVerificationResult
}

enum class FaceVerificationError {
    /** A face was captured, but it is not the enrolled one. */
    NoMatch,

    /** The on-device matcher could not run — retrying is worthwhile. */
    SensorUnavailable,
}
