package com.example.test_ai_project.auth.presentation.faceverification.contract

import com.example.test_ai_project.auth.domain.model.FaceAlignment
import com.example.test_ai_project.auth.domain.model.FaceObservation
import com.example.test_ai_project.auth.domain.model.FaceVerificationError
import com.example.test_ai_project.auth.domain.model.MisalignmentReason
import com.example.test_ai_project.resource.base.UiEffect
import com.example.test_ai_project.resource.base.UiEvent
import com.example.test_ai_project.resource.base.UiState

/**
 * Everything the verification screen renders, and nothing else.
 *
 * Note the deliberate exception to the app's "errors are toasts" rule: [error] stays in
 * state rather than leaving as an effect. On this screen the instruction panel *is* the
 * primary content — a viewfinder with nothing but guidance on it — and a match failure has
 * to persist next to the retry button rather than fading after three seconds.
 */
data class FaceVerificationState(
    val cameraPermission: CameraPermission = CameraPermission.Unknown,
    val phase: VerificationPhase = VerificationPhase.Scanning,
    val alignment: FaceAlignment = FaceAlignment.Misaligned(MisalignmentReason.NoFace),
    /** Consecutive aligned frames — see [isReadyToConfirm]. */
    val stableAlignedFrames: Int = 0,
    val error: FaceVerificationError? = null,
    val isCameraUnavailable: Boolean = false,
) : UiState {

    /**
     * Requiring several consecutive aligned frames is what stops the button arming on a
     * single lucky frame as the user moves past the right position.
     */
    val isReadyToConfirm: Boolean
        get() = phase == VerificationPhase.Scanning &&
            alignment is FaceAlignment.Aligned &&
            stableAlignedFrames >= REQUIRED_STABLE_FRAMES

    val isPreviewActive: Boolean
        get() = cameraPermission == CameraPermission.Granted && !isCameraUnavailable

    companion object {
        const val REQUIRED_STABLE_FRAMES = 5
    }
}

enum class CameraPermission {
    /** Not asked yet — the screen asks on first composition. */
    Unknown,
    Granted,
    Denied,
}

enum class VerificationPhase {
    Scanning,
    Confirming,
    Verified,
}

sealed interface FaceVerificationEvent : UiEvent {
    data class CameraPermissionResult(val isGranted: Boolean) : FaceVerificationEvent
    data class FacesDetected(val faces: List<FaceObservation>) : FaceVerificationEvent
    data object CameraFailed : FaceVerificationEvent
    data object Confirmed : FaceVerificationEvent
    data object RetryRequested : FaceVerificationEvent
}

sealed interface FaceVerificationEffect : UiEffect {

    /** The face matched; the root graph decides what comes next. */
    data object Verified : FaceVerificationEffect
}
