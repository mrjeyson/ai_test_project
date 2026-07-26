package com.example.test_ai_project.feature.faceverification

import com.example.test_ai_project.core.domain.repository.FaceVerificationRepository
import com.example.test_ai_project.core.domain.usecase.EvaluateFaceAlignmentUseCase
import com.example.test_ai_project.core.domain.usecase.VerifyFaceLocallyUseCase
import com.example.test_ai_project.core.model.FaceAlignment
import com.example.test_ai_project.core.model.FaceObservation
import com.example.test_ai_project.core.model.FaceVerificationFailure
import com.example.test_ai_project.core.model.FaceVerificationOutcome
import com.example.test_ai_project.core.model.MisalignmentReason
import com.example.test_ai_project.core.model.NormalizedRect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The real [EvaluateFaceAlignmentUseCase] is used — it is pure, and doubling it would only
 * prove the double agrees with itself. The repository, which is the part with work in it,
 * is faked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FaceVerificationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeFaceVerificationRepository()

    private fun viewModel() = FaceVerificationViewModel(
        evaluateFaceAlignment = EvaluateFaceAlignmentUseCase(),
        verifyFaceLocally = VerifyFaceLocallyUseCase(repository),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts with no face and no permission decision`() {
        val state = viewModel().uiState.value

        assertEquals(CameraPermission.Unknown, state.cameraPermission)
        assertEquals(FaceAlignment.Misaligned(MisalignmentReason.NoFace), state.alignment)
        assertFalse(state.isPreviewActive)
    }

    @Test
    fun `granting the permission activates the preview`() {
        val viewModel = viewModel()

        viewModel.onCameraPermissionResult(true)

        assertTrue(viewModel.uiState.value.isPreviewActive)
    }

    @Test
    fun `refusing the permission leaves the preview off`() {
        val viewModel = viewModel()

        viewModel.onCameraPermissionResult(false)

        assertEquals(CameraPermission.Denied, viewModel.uiState.value.cameraPermission)
        assertFalse(viewModel.uiState.value.isPreviewActive)
    }

    @Test
    fun `one aligned frame is not enough to arm the button`() {
        val viewModel = viewModel()

        viewModel.onFacesDetected(listOf(alignedFace))

        assertEquals(FaceAlignment.Aligned, viewModel.uiState.value.alignment)
        assertFalse(viewModel.uiState.value.isReadyToConfirm)
    }

    @Test
    fun `enough consecutive aligned frames arms the button`() {
        val viewModel = viewModel()

        repeat(FaceVerificationUiState.REQUIRED_STABLE_FRAMES) {
            viewModel.onFacesDetected(listOf(alignedFace))
        }

        assertTrue(viewModel.uiState.value.isReadyToConfirm)
    }

    @Test
    fun `a single bad frame resets the streak`() {
        val viewModel = viewModel()
        repeat(FaceVerificationUiState.REQUIRED_STABLE_FRAMES) {
            viewModel.onFacesDetected(listOf(alignedFace))
        }

        viewModel.onFacesDetected(emptyList())

        assertEquals(0, viewModel.uiState.value.stableAlignedFrames)
        assertFalse(viewModel.uiState.value.isReadyToConfirm)
    }

    @Test
    fun `confirming an unarmed form does nothing`() = runTest {
        val viewModel = viewModel()
        viewModel.onFacesDetected(listOf(alignedFace))

        viewModel.onConfirm()
        advanceUntilIdle()

        assertEquals(VerificationPhase.Scanning, viewModel.uiState.value.phase)
        assertEquals(0, repository.attempts)
    }

    @Test
    fun `a confirmed face verifies and reports the frame it matched`() = runTest {
        val viewModel = armed()

        viewModel.onConfirm()
        advanceUntilIdle()

        assertEquals(VerificationPhase.Verified, viewModel.uiState.value.phase)
        assertEquals(alignedFace, repository.lastFace)
    }

    @Test
    fun `frames arriving mid-match do not disturb the phase`() = runTest {
        val viewModel = armed()
        viewModel.onConfirm()

        viewModel.onFacesDetected(emptyList())

        assertEquals(VerificationPhase.Confirming, viewModel.uiState.value.phase)
        advanceUntilIdle()
        assertEquals(VerificationPhase.Verified, viewModel.uiState.value.phase)
    }

    @Test
    fun `a rejected match returns to scanning with a reason`() = runTest {
        repository.outcome = FaceVerificationOutcome.Failed(FaceVerificationFailure.NoMatch)
        val viewModel = armed()

        viewModel.onConfirm()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(VerificationPhase.Scanning, state.phase)
        assertEquals(FaceVerificationFailure.NoMatch, state.failure)
        assertEquals(0, state.stableAlignedFrames)
    }

    @Test
    fun `a thrown matcher is reported as the sensor being unavailable`() = runTest {
        repository.throwOnVerify = true
        val viewModel = armed()

        viewModel.onConfirm()
        advanceUntilIdle()

        assertEquals(
            FaceVerificationFailure.SensorUnavailable,
            viewModel.uiState.value.failure,
        )
    }

    @Test
    fun `a camera error is recoverable by retrying`() {
        val viewModel = viewModel()
        viewModel.onCameraPermissionResult(true)
        viewModel.onCameraError()
        assertFalse(viewModel.uiState.value.isPreviewActive)

        viewModel.onRetry()

        assertTrue(viewModel.uiState.value.isPreviewActive)
        assertEquals(0, viewModel.uiState.value.stableAlignedFrames)
    }

    private fun armed() = viewModel().also { viewModel ->
        viewModel.onCameraPermissionResult(true)
        repeat(FaceVerificationUiState.REQUIRED_STABLE_FRAMES) {
            viewModel.onFacesDetected(listOf(alignedFace))
        }
    }

    private val alignedFace = FaceObservation(
        bounds = NormalizedRect(left = 0.3f, top = 0.25f, right = 0.7f, bottom = 0.75f),
        yawDegrees = 0f,
        rollDegrees = 0f,
    )
}

private class FakeFaceVerificationRepository : FaceVerificationRepository {

    var outcome: FaceVerificationOutcome = FaceVerificationOutcome.Verified
    var throwOnVerify = false
    var attempts = 0
        private set
    var lastFace: FaceObservation? = null
        private set

    override suspend fun verify(face: FaceObservation): FaceVerificationOutcome {
        attempts++
        lastFace = face
        if (throwOnVerify) error("matcher unavailable")
        return outcome
    }
}
