package com.example.test_ai_project.feature.faceverification

import com.example.test_ai_project.core.model.FaceAlignment
import com.example.test_ai_project.core.model.FaceVerificationFailure
import com.example.test_ai_project.core.model.MisalignmentReason

/** Everything the verification screen needs to render, and nothing else. */
data class FaceVerificationUiState(
    val cameraPermission: CameraPermission = CameraPermission.Unknown,
    val phase: VerificationPhase = VerificationPhase.Scanning,
    val alignment: FaceAlignment = FaceAlignment.Misaligned(MisalignmentReason.NoFace),
    /** Consecutive aligned frames — see [isReadyToConfirm]. */
    val stableAlignedFrames: Int = 0,
    val failure: FaceVerificationFailure? = null,
    val isCameraUnavailable: Boolean = false,
) {

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
