package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.model.FaceAlignment
import com.example.test_ai_project.core.model.FaceObservation
import com.example.test_ai_project.core.model.MisalignmentReason
import com.example.test_ai_project.core.model.NormalizedRect
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The payoff for normalising ML Kit's output in the analyser: every alignment rule is
 * assertable from plain numbers, with no camera and no model.
 */
class EvaluateFaceAlignmentUseCaseTest {

    private val evaluate = EvaluateFaceAlignmentUseCase()

    @Test
    fun `an empty frame reports no face`() {
        assertEquals(misaligned(MisalignmentReason.NoFace), evaluate(emptyList()))
    }

    @Test
    fun `two faces are refused rather than guessed between`() {
        val faces = listOf(face(), face(centerX = 0.4f))

        assertEquals(misaligned(MisalignmentReason.MultipleFaces), evaluate(faces))
    }

    @Test
    fun `a centred frontal face of the right size is aligned`() {
        assertEquals(FaceAlignment.Aligned, evaluate(listOf(face())))
    }

    @Test
    fun `a small face is too far away`() {
        assertEquals(
            misaligned(MisalignmentReason.TooFar),
            evaluate(listOf(face(height = 0.2f))),
        )
    }

    @Test
    fun `a face filling the frame is too close`() {
        assertEquals(
            misaligned(MisalignmentReason.TooClose),
            evaluate(listOf(face(height = 0.95f))),
        )
    }

    @Test
    fun `a face away from the middle is off centre`() {
        assertEquals(
            misaligned(MisalignmentReason.OffCentre),
            evaluate(listOf(face(centerX = 0.8f))),
        )
    }

    @Test
    fun `vertical drift is caught as well as horizontal`() {
        assertEquals(
            misaligned(MisalignmentReason.OffCentre),
            evaluate(listOf(face(centerY = 0.2f))),
        )
    }

    @Test
    fun `a turned head is refused`() {
        assertEquals(
            misaligned(MisalignmentReason.Turned),
            evaluate(listOf(face(yaw = 30f))),
        )
    }

    @Test
    fun `a tilted head is refused`() {
        assertEquals(
            misaligned(MisalignmentReason.Turned),
            evaluate(listOf(face(roll = -25f))),
        )
    }

    @Test
    fun `a slight turn is tolerated`() {
        assertEquals(FaceAlignment.Aligned, evaluate(listOf(face(yaw = 8f, roll = 5f))))
    }

    @Test
    fun `distance is reported before centring, because a distant face is not reliably placed`() {
        val distantAndOffCentre = face(height = 0.1f, centerX = 0.9f)

        assertEquals(misaligned(MisalignmentReason.TooFar), evaluate(listOf(distantAndOffCentre)))
    }

    private fun misaligned(reason: MisalignmentReason) = FaceAlignment.Misaligned(reason)

    private fun face(
        centerX: Float = 0.5f,
        centerY: Float = 0.5f,
        height: Float = 0.5f,
        yaw: Float = 0f,
        roll: Float = 0f,
    ): FaceObservation {
        val width = height * FACE_ASPECT
        return FaceObservation(
            bounds = NormalizedRect(
                left = centerX - width / 2f,
                top = centerY - height / 2f,
                right = centerX + width / 2f,
                bottom = centerY + height / 2f,
            ),
            yawDegrees = yaw,
            rollDegrees = roll,
        )
    }

    private companion object {
        /** Roughly the width-to-height ratio of a detected face box. */
        const val FACE_ASPECT = 0.78f
    }
}
