package com.example.test_ai_project.auth.presentation.faceverification.viewmodel

import app.cash.turbine.test
import com.example.test_ai_project.auth.domain.model.FaceAlignment
import com.example.test_ai_project.auth.domain.model.FaceObservation
import com.example.test_ai_project.auth.domain.model.FaceVerificationError
import com.example.test_ai_project.auth.domain.model.FaceVerificationResult
import com.example.test_ai_project.auth.domain.model.MisalignmentReason
import com.example.test_ai_project.auth.domain.model.NormalizedRect
import com.example.test_ai_project.auth.domain.service.FaceVerificationService
import com.example.test_ai_project.auth.presentation.faceverification.contract.CameraPermission
import com.example.test_ai_project.auth.presentation.faceverification.contract.FaceVerificationEffect
import com.example.test_ai_project.auth.presentation.faceverification.contract.FaceVerificationEvent
import com.example.test_ai_project.auth.presentation.faceverification.contract.FaceVerificationState
import com.example.test_ai_project.auth.presentation.faceverification.contract.VerificationPhase
import com.example.test_ai_project.auth.presentation.testing.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * The alignment rules are exercised through a fake service rather than the real one: this
 * class's job is the frame-streak and phase machine, and `DefaultFaceVerificationServiceTest`
 * already covers the geometry.
 */
class FaceVerificationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val service = FakeFaceVerificationService()

    private fun viewModel() = FaceVerificationViewModel(service)

    @Test
    fun `starts with no face and no permission decision`() {
        val state = viewModel().uiState.value

        assertThat(state.cameraPermission).isEqualTo(CameraPermission.Unknown)
        assertThat(state.alignment)
            .isEqualTo(FaceAlignment.Misaligned(MisalignmentReason.NoFace))
        assertThat(state.isPreviewActive).isFalse()
    }

    @Test
    fun `granting the permission activates the preview`() {
        val viewModel = viewModel()

        viewModel.onEvent(FaceVerificationEvent.CameraPermissionResult(true))

        assertThat(viewModel.uiState.value.isPreviewActive).isTrue()
    }

    @Test
    fun `refusing the permission leaves the preview off`() {
        val viewModel = viewModel()

        viewModel.onEvent(FaceVerificationEvent.CameraPermissionResult(false))

        assertThat(viewModel.uiState.value.cameraPermission).isEqualTo(CameraPermission.Denied)
        assertThat(viewModel.uiState.value.isPreviewActive).isFalse()
    }

    @Test
    fun `one aligned frame is not enough to arm the button`() {
        val viewModel = viewModel()

        viewModel.onEvent(FaceVerificationEvent.FacesDetected(listOf(alignedFace)))

        assertThat(viewModel.uiState.value.alignment).isEqualTo(FaceAlignment.Aligned)
        assertThat(viewModel.uiState.value.isReadyToConfirm).isFalse()
    }

    @Test
    fun `enough consecutive aligned frames arms the button`() {
        val viewModel = viewModel()

        repeat(FaceVerificationState.REQUIRED_STABLE_FRAMES) {
            viewModel.onEvent(FaceVerificationEvent.FacesDetected(listOf(alignedFace)))
        }

        assertThat(viewModel.uiState.value.isReadyToConfirm).isTrue()
    }

    @Test
    fun `a single bad frame resets the streak`() {
        val viewModel = viewModel()
        repeat(FaceVerificationState.REQUIRED_STABLE_FRAMES) {
            viewModel.onEvent(FaceVerificationEvent.FacesDetected(listOf(alignedFace)))
        }
        service.alignment = FaceAlignment.Misaligned(MisalignmentReason.NoFace)

        viewModel.onEvent(FaceVerificationEvent.FacesDetected(emptyList()))

        assertThat(viewModel.uiState.value.stableAlignedFrames).isEqualTo(0)
        assertThat(viewModel.uiState.value.isReadyToConfirm).isFalse()
    }

    @Test
    fun `confirming an unarmed screen does nothing`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(FaceVerificationEvent.FacesDetected(listOf(alignedFace)))

        viewModel.onEvent(FaceVerificationEvent.Confirmed)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.phase).isEqualTo(VerificationPhase.Scanning)
        assertThat(service.attempts).isEqualTo(0)
    }

    @Test
    fun `a confirmed face verifies, emits the effect and reports the frame it matched`() =
        runTest {
            val viewModel = armed()

            viewModel.effects.test {
                viewModel.onEvent(FaceVerificationEvent.Confirmed)
                advanceUntilIdle()

                assertThat(awaitItem()).isEqualTo(FaceVerificationEffect.Verified)
            }

            assertThat(viewModel.uiState.value.phase).isEqualTo(VerificationPhase.Verified)
            assertThat(service.lastFace).isEqualTo(alignedFace)
        }

    @Test
    fun `frames arriving mid-match do not disturb the phase`() = runTest {
        val viewModel = armed()
        viewModel.onEvent(FaceVerificationEvent.Confirmed)

        viewModel.onEvent(FaceVerificationEvent.FacesDetected(emptyList()))

        assertThat(viewModel.uiState.value.phase).isEqualTo(VerificationPhase.Confirming)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.phase).isEqualTo(VerificationPhase.Verified)
    }

    @Test
    fun `a rejected match returns to scanning with a reason`() = runTest {
        service.result = FaceVerificationResult.Failed(FaceVerificationError.NoMatch)
        val viewModel = armed()

        viewModel.onEvent(FaceVerificationEvent.Confirmed)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.phase).isEqualTo(VerificationPhase.Scanning)
        assertThat(state.error).isEqualTo(FaceVerificationError.NoMatch)
        assertThat(state.stableAlignedFrames).isEqualTo(0)
    }

    @Test
    fun `a camera error is recoverable by retrying`() {
        val viewModel = viewModel()
        viewModel.onEvent(FaceVerificationEvent.CameraPermissionResult(true))
        viewModel.onEvent(FaceVerificationEvent.CameraFailed)
        assertThat(viewModel.uiState.value.isPreviewActive).isFalse()

        viewModel.onEvent(FaceVerificationEvent.RetryRequested)

        assertThat(viewModel.uiState.value.isPreviewActive).isTrue()
        assertThat(viewModel.uiState.value.stableAlignedFrames).isEqualTo(0)
    }

    private fun armed() = viewModel().also { viewModel ->
        viewModel.onEvent(FaceVerificationEvent.CameraPermissionResult(true))
        repeat(FaceVerificationState.REQUIRED_STABLE_FRAMES) {
            viewModel.onEvent(FaceVerificationEvent.FacesDetected(listOf(alignedFace)))
        }
    }

    private val alignedFace = FaceObservation(
        bounds = NormalizedRect(left = 0.3f, top = 0.25f, right = 0.7f, bottom = 0.75f),
        yawDegrees = 0f,
        rollDegrees = 0f,
    )
}

private class FakeFaceVerificationService : FaceVerificationService {

    var alignment: FaceAlignment = FaceAlignment.Aligned
    var result: FaceVerificationResult = FaceVerificationResult.Verified
    var attempts = 0
        private set
    var lastFace: FaceObservation? = null
        private set

    override fun evaluateAlignment(faces: List<FaceObservation>): FaceAlignment =
        if (faces.isEmpty()) FaceAlignment.Misaligned(MisalignmentReason.NoFace) else alignment

    override suspend fun verify(face: FaceObservation): FaceVerificationResult {
        attempts++
        lastFace = face
        return result
    }
}
