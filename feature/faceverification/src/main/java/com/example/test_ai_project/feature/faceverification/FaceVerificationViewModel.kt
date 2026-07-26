package com.example.test_ai_project.feature.faceverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test_ai_project.core.domain.usecase.EvaluateFaceAlignmentUseCase
import com.example.test_ai_project.core.domain.usecase.VerifyFaceLocallyUseCase
import com.example.test_ai_project.core.model.FaceAlignment
import com.example.test_ai_project.core.model.FaceObservation
import com.example.test_ai_project.core.model.FaceVerificationFailure
import com.example.test_ai_project.core.model.FaceVerificationOutcome
import com.example.test_ai_project.core.model.MisalignmentReason
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Turns a stream of detected faces into a verification decision.
 *
 * It never touches CameraX or ML Kit — frames arrive already reduced to
 * [FaceObservation]s, which is what keeps this class testable with plain data.
 */
@HiltViewModel
class FaceVerificationViewModel @Inject constructor(
    private val evaluateFaceAlignment: EvaluateFaceAlignmentUseCase,
    private val verifyFaceLocally: VerifyFaceLocallyUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FaceVerificationUiState())
    val uiState: StateFlow<FaceVerificationUiState> = _uiState.asStateFlow()

    /**
     * The frame the user is confirming. Held here rather than in the UI state: it is
     * evidence for the matcher, not something the screen draws.
     */
    private var lastAlignedFace: FaceObservation? = null

    fun onCameraPermissionResult(isGranted: Boolean) {
        _uiState.update {
            it.copy(
                cameraPermission = if (isGranted) {
                    CameraPermission.Granted
                } else {
                    CameraPermission.Denied
                },
            )
        }
    }

    fun onFacesDetected(faces: List<FaceObservation>) {
        // Frames keep arriving while the matcher runs; they must not reset the phase or
        // re-arm the button underneath it.
        if (_uiState.value.phase != VerificationPhase.Scanning) return

        val alignment = evaluateFaceAlignment(faces)
        lastAlignedFace = if (alignment is FaceAlignment.Aligned) faces.firstOrNull() else null

        _uiState.update { state ->
            state.copy(
                alignment = alignment,
                stableAlignedFrames = if (alignment is FaceAlignment.Aligned) {
                    state.stableAlignedFrames + 1
                } else {
                    0
                },
            )
        }
    }

    fun onCameraError() {
        _uiState.update {
            it.copy(
                isCameraUnavailable = true,
                phase = VerificationPhase.Scanning,
                stableAlignedFrames = 0,
                alignment = FaceAlignment.Misaligned(MisalignmentReason.NoFace),
            )
        }
    }

    fun onConfirm() {
        val state = _uiState.value
        val face = lastAlignedFace
        // Guard, not an assumption: the frame can go out of alignment between the button
        // arming and the tap landing.
        if (!state.isReadyToConfirm || face == null) return

        _uiState.update { it.copy(phase = VerificationPhase.Confirming, failure = null) }

        viewModelScope.launch {
            val outcome = runCatching { verifyFaceLocally(face) }
                .getOrElse { FaceVerificationOutcome.Failed(FaceVerificationFailure.SensorUnavailable) }

            _uiState.update { current ->
                when (outcome) {
                    FaceVerificationOutcome.Verified -> current.copy(
                        phase = VerificationPhase.Verified,
                    )

                    is FaceVerificationOutcome.Failed -> current.copy(
                        phase = VerificationPhase.Scanning,
                        stableAlignedFrames = 0,
                        failure = outcome.reason,
                    )
                }
            }
        }
    }

    /** Back to scanning after a failed match or a camera that could not be opened. */
    fun onRetry() {
        lastAlignedFace = null
        _uiState.update {
            it.copy(
                phase = VerificationPhase.Scanning,
                stableAlignedFrames = 0,
                alignment = FaceAlignment.Misaligned(MisalignmentReason.NoFace),
                failure = null,
                isCameraUnavailable = false,
            )
        }
    }
}
