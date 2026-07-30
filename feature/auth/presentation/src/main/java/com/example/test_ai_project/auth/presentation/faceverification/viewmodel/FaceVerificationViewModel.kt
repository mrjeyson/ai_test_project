package com.example.test_ai_project.auth.presentation.faceverification.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.test_ai_project.auth.domain.model.FaceAlignment
import com.example.test_ai_project.auth.domain.model.FaceObservation
import com.example.test_ai_project.auth.domain.model.FaceVerificationResult
import com.example.test_ai_project.auth.domain.model.MisalignmentReason
import com.example.test_ai_project.auth.domain.service.FaceVerificationService
import com.example.test_ai_project.auth.presentation.faceverification.contract.CameraPermission
import com.example.test_ai_project.auth.presentation.faceverification.contract.FaceVerificationEffect
import com.example.test_ai_project.auth.presentation.faceverification.contract.FaceVerificationEvent
import com.example.test_ai_project.auth.presentation.faceverification.contract.FaceVerificationState
import com.example.test_ai_project.auth.presentation.faceverification.contract.VerificationPhase
import com.example.test_ai_project.resource.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Turns a stream of detected faces into a verification decision.
 *
 * It never touches CameraX or ML Kit — frames arrive already reduced to
 * [FaceObservation]s, which is what keeps this class testable with plain data.
 */
@HiltViewModel
class FaceVerificationViewModel @Inject constructor(
    private val faceVerificationService: FaceVerificationService,
) : BaseViewModel<FaceVerificationState, FaceVerificationEvent, FaceVerificationEffect>(
    FaceVerificationState(),
) {

    /**
     * The frame the user is confirming. Held here rather than in the UI state: it is
     * evidence for the matcher, not something the screen draws.
     */
    private var lastAlignedFace: FaceObservation? = null

    override fun onEvent(event: FaceVerificationEvent) {
        when (event) {
            is FaceVerificationEvent.CameraPermissionResult -> setState {
                copy(
                    cameraPermission = if (event.isGranted) {
                        CameraPermission.Granted
                    } else {
                        CameraPermission.Denied
                    },
                )
            }

            is FaceVerificationEvent.FacesDetected -> onFacesDetected(event.faces)
            FaceVerificationEvent.CameraFailed -> onCameraFailed()
            FaceVerificationEvent.Confirmed -> onConfirm()
            FaceVerificationEvent.RetryRequested -> onRetry()
        }
    }

    private fun onFacesDetected(faces: List<FaceObservation>) {
        // Frames keep arriving while the matcher runs; they must not reset the phase or
        // re-arm the button underneath it.
        if (currentState.phase != VerificationPhase.Scanning) return

        val alignment = faceVerificationService.evaluateAlignment(faces)
        lastAlignedFace = if (alignment is FaceAlignment.Aligned) faces.firstOrNull() else null

        setState {
            copy(
                alignment = alignment,
                stableAlignedFrames = if (alignment is FaceAlignment.Aligned) {
                    stableAlignedFrames + 1
                } else {
                    0
                },
            )
        }
    }

    private fun onCameraFailed() {
        setState {
            copy(
                isCameraUnavailable = true,
                phase = VerificationPhase.Scanning,
                stableAlignedFrames = 0,
                alignment = FaceAlignment.Misaligned(MisalignmentReason.NoFace),
            )
        }
    }

    private fun onConfirm() {
        val face = lastAlignedFace
        // Guard, not an assumption: the frame can go out of alignment between the button
        // arming and the tap landing.
        if (!currentState.isReadyToConfirm || face == null) return

        setState { copy(phase = VerificationPhase.Confirming, error = null) }

        viewModelScope.launch {
            when (val result = faceVerificationService.verify(face)) {
                FaceVerificationResult.Verified -> {
                    setState { copy(phase = VerificationPhase.Verified) }
                    sendEffect(FaceVerificationEffect.Verified)
                }

                is FaceVerificationResult.Failed -> setState {
                    copy(
                        phase = VerificationPhase.Scanning,
                        stableAlignedFrames = 0,
                        error = result.error,
                    )
                }
            }
        }
    }

    /** Back to scanning after a failed match or a camera that could not be opened. */
    private fun onRetry() {
        lastAlignedFace = null
        setState {
            copy(
                phase = VerificationPhase.Scanning,
                stableAlignedFrames = 0,
                alignment = FaceAlignment.Misaligned(MisalignmentReason.NoFace),
                error = null,
                isCameraUnavailable = false,
            )
        }
    }
}
