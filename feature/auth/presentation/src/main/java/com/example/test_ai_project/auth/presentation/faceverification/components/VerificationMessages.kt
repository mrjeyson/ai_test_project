package com.example.test_ai_project.auth.presentation.faceverification.components

import androidx.annotation.StringRes
import com.example.test_ai_project.auth.domain.model.FaceAlignment
import com.example.test_ai_project.auth.domain.model.FaceVerificationError
import com.example.test_ai_project.auth.domain.model.MisalignmentReason
import com.example.test_ai_project.auth.presentation.faceverification.contract.CameraPermission
import com.example.test_ai_project.auth.presentation.faceverification.contract.FaceVerificationState
import com.example.test_ai_project.auth.presentation.faceverification.contract.VerificationPhase
import com.example.test_ai_project.resource.R as ResR

internal enum class ActionKind { Confirm, Retry, GrantPermission }

internal data class ScreenAction(
    val kind: ActionKind,
    @param:StringRes val labelRes: Int,
    val isEnabled: Boolean,
    val isLoading: Boolean = false,
)

/**
 * One place decides what the primary button says and does, so the label can never
 * disagree with the tap.
 */
internal fun FaceVerificationState.action(): ScreenAction = when {
    cameraPermission == CameraPermission.Denied -> ScreenAction(
        kind = ActionKind.GrantPermission,
        labelRes = ResR.string.face_verification_action_allow_camera,
        isEnabled = true,
    )

    isCameraUnavailable || error != null -> ScreenAction(
        kind = ActionKind.Retry,
        labelRes = ResR.string.face_verification_action_retry,
        isEnabled = true,
    )

    phase == VerificationPhase.Confirming -> ScreenAction(
        kind = ActionKind.Confirm,
        labelRes = ResR.string.face_verification_action_confirming,
        isEnabled = false,
        isLoading = true,
    )

    phase == VerificationPhase.Verified -> ScreenAction(
        kind = ActionKind.Confirm,
        labelRes = ResR.string.face_verification_action_verified,
        isEnabled = false,
    )

    else -> ScreenAction(
        kind = ActionKind.Confirm,
        labelRes = ResR.string.face_verification_action_confirm,
        isEnabled = isReadyToConfirm,
    )
}

@StringRes
internal fun FaceVerificationState.titleRes(): Int = when {
    cameraPermission == CameraPermission.Denied ->
        ResR.string.face_verification_permission_title
    isCameraUnavailable -> ResR.string.face_verification_camera_unavailable_title
    phase == VerificationPhase.Confirming -> ResR.string.face_verification_confirming_title
    phase == VerificationPhase.Verified -> ResR.string.face_verification_verified_title
    alignment is FaceAlignment.Aligned -> ResR.string.face_verification_hold_still_title
    else -> ResR.string.face_verification_align_title
}

/**
 * The body text names the specific problem — "move closer" is actionable where "align your
 * face" is not.
 */
@StringRes
internal fun FaceVerificationState.bodyRes(): Int = when {
    cameraPermission == CameraPermission.Denied ->
        ResR.string.face_verification_permission_body
    isCameraUnavailable -> ResR.string.face_verification_camera_unavailable_body
    error != null -> error.messageRes()
    phase == VerificationPhase.Confirming -> ResR.string.face_verification_confirming_body
    phase == VerificationPhase.Verified -> ResR.string.face_verification_verified_body
    else -> when (val current = alignment) {
        FaceAlignment.Aligned -> ResR.string.face_verification_hold_still_body
        is FaceAlignment.Misaligned -> current.reason.messageRes()
    }
}

@StringRes
private fun MisalignmentReason.messageRes(): Int = when (this) {
    MisalignmentReason.NoFace -> ResR.string.face_verification_align_body
    MisalignmentReason.MultipleFaces -> ResR.string.face_verification_body_multiple_faces
    MisalignmentReason.TooFar -> ResR.string.face_verification_body_too_far
    MisalignmentReason.TooClose -> ResR.string.face_verification_body_too_close
    MisalignmentReason.OffCentre -> ResR.string.face_verification_body_off_centre
    MisalignmentReason.Turned -> ResR.string.face_verification_body_turned
}

@StringRes
private fun FaceVerificationError.messageRes(): Int = when (this) {
    FaceVerificationError.NoMatch -> ResR.string.face_verification_failure_no_match
    FaceVerificationError.SensorUnavailable -> ResR.string.face_verification_failure_sensor
}
