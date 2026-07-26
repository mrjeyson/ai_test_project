package com.example.test_ai_project.core.model

/** The result of matching a well-aligned face against the enrolment on this device. */
sealed interface FaceVerificationOutcome {

    data object Verified : FaceVerificationOutcome

    data class Failed(val reason: FaceVerificationFailure) : FaceVerificationOutcome
}

enum class FaceVerificationFailure {
    /** A face was captured, but it is not the enrolled one. */
    NoMatch,

    /** The on-device matcher could not run — retrying is worthwhile. */
    SensorUnavailable,
}
