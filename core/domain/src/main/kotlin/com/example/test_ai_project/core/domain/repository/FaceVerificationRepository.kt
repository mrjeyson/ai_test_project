package com.example.test_ai_project.core.domain.repository

import com.example.test_ai_project.core.model.FaceObservation
import com.example.test_ai_project.core.model.FaceVerificationOutcome

/**
 * Matches a captured face against the enrolment held on this device.
 *
 * Takes the [FaceObservation] the decision was made from, so an implementation can match
 * on it — and so the call carries evidence of what was actually on screen when the user
 * confirmed.
 */
interface FaceVerificationRepository {
    suspend fun verify(face: FaceObservation): FaceVerificationOutcome
}
