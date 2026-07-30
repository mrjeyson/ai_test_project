package com.example.test_ai_project.auth.domain.service

import com.example.test_ai_project.auth.domain.model.FaceAlignment
import com.example.test_ai_project.auth.domain.model.FaceObservation
import com.example.test_ai_project.auth.domain.model.FaceVerificationResult

/**
 * Everything the face-verification feature can do.
 *
 * The two operations are one responsibility — deciding whether a frame is good enough,
 * then matching it — so they share a service rather than becoming
 * `EvaluateFaceAlignmentUseCase` and `VerifyFaceLocallyUseCase`.
 */
interface FaceVerificationService {

    /**
     * Decides whether what the camera is seeing is good enough to verify from.
     *
     * Synchronous and pure, because it runs on every analysed frame: a coroutine per frame
     * would cost more than the rules themselves.
     */
    fun evaluateAlignment(faces: List<FaceObservation>): FaceAlignment

    /** Matches an already-aligned face against the enrolment on this device. */
    suspend fun verify(face: FaceObservation): FaceVerificationResult
}
