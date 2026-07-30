package com.example.test_ai_project.auth.data.service

import com.example.test_ai_project.auth.data.local.FaceEnrolmentSource
import com.example.test_ai_project.auth.domain.model.FaceAlignment
import com.example.test_ai_project.auth.domain.model.FaceObservation
import com.example.test_ai_project.auth.domain.model.FaceVerificationError
import com.example.test_ai_project.auth.domain.model.FaceVerificationResult
import com.example.test_ai_project.auth.domain.model.MisalignmentReason
import com.example.test_ai_project.auth.domain.service.FaceVerificationService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max

/**
 * The face-verification feature's logic, in one place.
 *
 * The alignment rules live here rather than in the frame analyser so they can be tested
 * without a camera, and so tuning a threshold is a change with a failing test to prove it
 * — not an edit inside a frame callback.
 */
@Singleton
class DefaultFaceVerificationService @Inject constructor(
    private val enrolmentSource: FaceEnrolmentSource,
) : FaceVerificationService {

    override fun evaluateAlignment(faces: List<FaceObservation>): FaceAlignment = when {
        faces.isEmpty() -> misaligned(MisalignmentReason.NoFace)
        faces.size > 1 -> misaligned(MisalignmentReason.MultipleFaces)
        else -> evaluate(faces.first())
    }

    override suspend fun verify(face: FaceObservation): FaceVerificationResult =
        runCatching { enrolmentSource.matches(face) }
            .fold(
                onSuccess = { matched ->
                    if (matched) {
                        FaceVerificationResult.Verified
                    } else {
                        FaceVerificationResult.Failed(FaceVerificationError.NoMatch)
                    }
                },
                onFailure = {
                    FaceVerificationResult.Failed(FaceVerificationError.SensorUnavailable)
                },
            )

    /**
     * Ordered from most to least useful to be told: distance first, because a face that is
     * too far away is also unreliably centred and posed.
     */
    private fun evaluate(face: FaceObservation): FaceAlignment {
        val height = face.bounds.height
        val offset = max(
            abs(face.bounds.centerX - FRAME_CENTER),
            abs(face.bounds.centerY - FRAME_CENTER),
        )

        return when {
            height < MIN_FACE_HEIGHT -> misaligned(MisalignmentReason.TooFar)
            height > MAX_FACE_HEIGHT -> misaligned(MisalignmentReason.TooClose)
            offset > MAX_CENTER_OFFSET -> misaligned(MisalignmentReason.OffCentre)
            abs(face.yawDegrees) > MAX_YAW_DEGREES -> misaligned(MisalignmentReason.Turned)
            abs(face.rollDegrees) > MAX_ROLL_DEGREES -> misaligned(MisalignmentReason.Turned)
            else -> FaceAlignment.Aligned
        }
    }

    private fun misaligned(reason: MisalignmentReason) = FaceAlignment.Misaligned(reason)

    private companion object {
        const val FRAME_CENTER = 0.5f

        /** Below this the face is too small for the matcher to be trusted. */
        const val MIN_FACE_HEIGHT = 0.32f

        /** Above this the face is cropped by the frame more often than not. */
        const val MAX_FACE_HEIGHT = 0.88f

        /** How far the face may drift from the middle of the frame, on either axis. */
        const val MAX_CENTER_OFFSET = 0.14f

        const val MAX_YAW_DEGREES = 15f
        const val MAX_ROLL_DEGREES = 12f
    }
}
