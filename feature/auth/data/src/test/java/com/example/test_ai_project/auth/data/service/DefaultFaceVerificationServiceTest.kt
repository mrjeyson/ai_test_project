package com.example.test_ai_project.auth.data.service

import com.example.test_ai_project.auth.data.local.FaceEnrolmentSource
import com.example.test_ai_project.auth.domain.model.FaceAlignment
import com.example.test_ai_project.auth.domain.model.FaceObservation
import com.example.test_ai_project.auth.domain.model.FaceVerificationError
import com.example.test_ai_project.auth.domain.model.FaceVerificationResult
import com.example.test_ai_project.auth.domain.model.MisalignmentReason
import com.example.test_ai_project.auth.domain.model.NormalizedRect
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The payoff for normalising ML Kit's output in the analyser: every alignment rule is
 * assertable from plain numbers, with no camera and no model.
 */
class DefaultFaceVerificationServiceTest {

    private val enrolmentSource = mockk<FaceEnrolmentSource>()
    private val service = DefaultFaceVerificationService(enrolmentSource)

    @Test
    fun `an empty frame reports no face`() {
        assertThat(service.evaluateAlignment(emptyList()))
            .isEqualTo(misaligned(MisalignmentReason.NoFace))
    }

    @Test
    fun `two faces are refused rather than guessed between`() {
        val faces = listOf(face(), face(centerX = 0.4f))

        assertThat(service.evaluateAlignment(faces))
            .isEqualTo(misaligned(MisalignmentReason.MultipleFaces))
    }

    @Test
    fun `a centred frontal face of the right size is aligned`() {
        assertThat(service.evaluateAlignment(listOf(face()))).isEqualTo(FaceAlignment.Aligned)
    }

    @Test
    fun `a small face is too far away`() {
        assertThat(service.evaluateAlignment(listOf(face(height = 0.2f))))
            .isEqualTo(misaligned(MisalignmentReason.TooFar))
    }

    @Test
    fun `a face filling the frame is too close`() {
        assertThat(service.evaluateAlignment(listOf(face(height = 0.95f))))
            .isEqualTo(misaligned(MisalignmentReason.TooClose))
    }

    @Test
    fun `a face away from the middle is off centre`() {
        assertThat(service.evaluateAlignment(listOf(face(centerX = 0.8f))))
            .isEqualTo(misaligned(MisalignmentReason.OffCentre))
    }

    @Test
    fun `vertical drift is caught as well as horizontal`() {
        assertThat(service.evaluateAlignment(listOf(face(centerY = 0.8f))))
            .isEqualTo(misaligned(MisalignmentReason.OffCentre))
    }

    @Test
    fun `a turned head is refused`() {
        assertThat(service.evaluateAlignment(listOf(face(yaw = 30f))))
            .isEqualTo(misaligned(MisalignmentReason.Turned))
    }

    @Test
    fun `a tilted head is refused`() {
        assertThat(service.evaluateAlignment(listOf(face(roll = -25f))))
            .isEqualTo(misaligned(MisalignmentReason.Turned))
    }

    @Test
    fun `a slight turn is tolerated`() {
        assertThat(service.evaluateAlignment(listOf(face(yaw = 8f, roll = 5f))))
            .isEqualTo(FaceAlignment.Aligned)
    }

    @Test
    fun `distance is reported before centring, because a distant face is not reliably placed`() {
        val distantAndOffCentre = face(height = 0.1f, centerX = 0.9f)

        assertThat(service.evaluateAlignment(listOf(distantAndOffCentre)))
            .isEqualTo(misaligned(MisalignmentReason.TooFar))
    }

    @Test
    fun `a matching face verifies`() = runTest {
        coEvery { enrolmentSource.matches(any()) } returns true

        assertThat(service.verify(face())).isEqualTo(FaceVerificationResult.Verified)
    }

    @Test
    fun `a non-matching face is reported as no match`() = runTest {
        coEvery { enrolmentSource.matches(any()) } returns false

        assertThat(service.verify(face()))
            .isEqualTo(FaceVerificationResult.Failed(FaceVerificationError.NoMatch))
    }

    @Test
    fun `a thrown matcher is reported as the sensor being unavailable`() = runTest {
        coEvery { enrolmentSource.matches(any()) } throws IllegalStateException("matcher")

        assertThat(service.verify(face()))
            .isEqualTo(FaceVerificationResult.Failed(FaceVerificationError.SensorUnavailable))
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
