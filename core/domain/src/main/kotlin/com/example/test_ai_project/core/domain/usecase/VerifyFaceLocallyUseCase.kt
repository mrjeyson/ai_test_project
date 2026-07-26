package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.repository.FaceVerificationRepository
import com.example.test_ai_project.core.model.FaceObservation
import com.example.test_ai_project.core.model.FaceVerificationOutcome
import javax.inject.Inject

/** Matches an already-aligned face against the enrolment on this device. */
class VerifyFaceLocallyUseCase @Inject constructor(
    private val faceVerificationRepository: FaceVerificationRepository,
) {
    suspend operator fun invoke(face: FaceObservation): FaceVerificationOutcome =
        faceVerificationRepository.verify(face)
}
