package com.example.test_ai_project.feature.faceverification

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.test_ai_project.core.model.FaceAlignment
import com.example.test_ai_project.core.model.FaceObservation
import com.example.test_ai_project.core.model.FaceVerificationFailure
import com.example.test_ai_project.core.model.MisalignmentReason
import com.example.test_ai_project.core.ui.R as UiR
import com.example.test_ai_project.core.ui.theme.AppTheme
import com.example.test_ai_project.core.ui.theme.VaultCharcoal
import com.example.test_ai_project.core.ui.theme.VaultTealLight
import com.example.test_ai_project.feature.faceverification.camera.FaceCameraPreview

/**
 * Stateful entry point: the only place in this file that touches Hilt, the ViewModel, or
 * the permission API.
 */
@Composable
fun FaceVerificationRoute(
    onVerified: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FaceVerificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onCameraPermissionResult,
    )

    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        // Asked immediately: the screen is a viewfinder and nothing else, so there is no
        // useful state to show before the permission decision.
        if (isGranted) {
            viewModel.onCameraPermissionResult(true)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val isVerified = uiState.phase == VerificationPhase.Verified
    LaunchedEffect(isVerified) {
        if (isVerified) onVerified()
    }

    FaceVerificationScreen(
        uiState = uiState,
        onFacesDetected = viewModel::onFacesDetected,
        onCameraError = viewModel::onCameraError,
        onConfirm = viewModel::onConfirm,
        onRetry = viewModel::onRetry,
        onRequestCameraPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onCancel = onCancel,
        modifier = modifier,
    )
}

/**
 * Stateless and side-effect free — apart from the camera it hosts, which is bound to this
 * composition rather than to the ViewModel.
 *
 * Colours are pinned to the dark tokens rather than read from the colour scheme: the app
 * theme is light, and a viewfinder is not.
 */
@Composable
fun FaceVerificationScreen(
    uiState: FaceVerificationUiState,
    onFacesDetected: (List<FaceObservation>) -> Unit,
    onCameraError: () -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (uiState.isPreviewActive) {
            FaceCameraPreview(
                onFaces = onFacesDetected,
                onCameraError = onCameraError,
                modifier = Modifier.fillMaxSize(),
            )
        }

        FaceFrameOverlay(
            isAligned = uiState.alignment is FaceAlignment.Aligned,
            modifier = Modifier.fillMaxSize(),
        )

        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            VerificationTopBar(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))

            // The frame occupies the middle; the panel is pinned to the bottom.
            Spacer(modifier = Modifier.weight(1f))

            BottomPanel(
                uiState = uiState,
                onConfirm = onConfirm,
                onRetry = onRetry,
                onRequestCameraPermission = onRequestCameraPermission,
                onCancel = onCancel,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(14.dp))
            OfflineBadge(modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * The scrim with an oval hole, plus the corner brackets.
 *
 * One even-odd [Path] rather than a scrim plus a blend-mode cut-out: even-odd needs no
 * offscreen compositing layer, which matters on a surface that redraws with the preview.
 */
@Composable
private fun FaceFrameOverlay(
    isAligned: Boolean,
    modifier: Modifier = Modifier,
) {
    val frameColor by animateColorAsState(
        targetValue = if (isAligned) VaultTealLight else Color.White.copy(alpha = 0.32f),
        label = "faceFrameColor",
    )
    val bracketColor = if (isAligned) VaultTealLight else VaultTealLight.copy(alpha = 0.55f)

    Canvas(modifier = modifier) {
        val ovalWidth = size.width * OVAL_WIDTH_FRACTION
        val ovalHeight = size.height * OVAL_HEIGHT_FRACTION
        val oval = Rect(
            offset = Offset(
                x = (size.width - ovalWidth) / 2f,
                y = size.height * OVAL_TOP_FRACTION,
            ),
            size = Size(ovalWidth, ovalHeight),
        )

        val scrim = Path().apply {
            addRect(Rect(Offset.Zero, size))
            addOval(oval)
            fillType = PathFillType.EvenOdd
        }
        drawPath(path = scrim, color = Color.Black.copy(alpha = SCRIM_ALPHA))

        drawOval(
            color = frameColor,
            topLeft = oval.topLeft,
            size = oval.size,
            style = Stroke(width = 2.dp.toPx()),
        )

        // Four L-shaped brackets on the oval's bounding box, as in the design.
        val armLength = ovalWidth * BRACKET_ARM_FRACTION
        val strokeWidth = 3.dp.toPx()
        listOf(
            Triple(oval.left, oval.top, 1f to 1f),
            Triple(oval.right, oval.top, -1f to 1f),
            Triple(oval.left, oval.bottom, 1f to -1f),
            Triple(oval.right, oval.bottom, -1f to -1f),
        ).forEach { (x, y, direction) ->
            val (horizontal, vertical) = direction
            drawLine(
                color = bracketColor,
                start = Offset(x, y),
                end = Offset(x + armLength * horizontal, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = bracketColor,
                start = Offset(x, y),
                end = Offset(x, y + armLength * vertical),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun VerificationTopBar(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = UiR.drawable.ic_shield),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = stringResource(id = UiR.string.brand_name),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
            Text(
                text = stringResource(id = R.string.face_verification_title),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.62f),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // A status badge, not a control: the model is bundled in the APK, so "on device"
        // is a fact to display rather than an action to offer.
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.10f),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_offline_ready),
                contentDescription = stringResource(id = R.string.face_verification_offline_badge),
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(6.dp).size(18.dp),
            )
        }
    }
}

@Composable
private fun BottomPanel(
    uiState: FaceVerificationUiState,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        InstructionCard(uiState = uiState)

        Spacer(modifier = Modifier.height(14.dp))

        val action = uiState.action()
        ActionButton(
            labelRes = action.labelRes,
            isLoading = action.isLoading,
            enabled = action.isEnabled,
            onClick = when (action.kind) {
                ActionKind.Confirm -> onConfirm
                ActionKind.Retry -> onRetry
                ActionKind.GrantPermission -> onRequestCameraPermission
            },
        )

        Spacer(modifier = Modifier.height(6.dp))

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(id = R.string.face_verification_cancel),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun InstructionCard(
    uiState: FaceVerificationUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.09f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = uiState.titleRes()),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = uiState.bodyRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** White on a dark viewfinder, per the design — not the app's teal primary. */
@Composable
private fun ActionButton(
    @StringRes labelRes: Int,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = VaultCharcoal,
            disabledContainerColor = Color.White.copy(alpha = 0.35f),
            disabledContentColor = VaultCharcoal.copy(alpha = 0.55f),
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = VaultCharcoal,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_circle),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = stringResource(id = labelRes),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun OfflineBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = Color.White.copy(alpha = 0.10f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(color = OfflineDotColor, shape = CircleShape),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.face_verification_processing_offline),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.82f),
            )
        }
    }
}

private enum class ActionKind { Confirm, Retry, GrantPermission }

private data class ScreenAction(
    val kind: ActionKind,
    @param:StringRes val labelRes: Int,
    val isEnabled: Boolean,
    val isLoading: Boolean = false,
)

/**
 * One place decides what the primary button says and does, so the label can never
 * disagree with the tap.
 */
private fun FaceVerificationUiState.action(): ScreenAction = when {
    cameraPermission == CameraPermission.Denied -> ScreenAction(
        kind = ActionKind.GrantPermission,
        labelRes = R.string.face_verification_action_allow_camera,
        isEnabled = true,
    )

    isCameraUnavailable || failure != null -> ScreenAction(
        kind = ActionKind.Retry,
        labelRes = R.string.face_verification_action_retry,
        isEnabled = true,
    )

    phase == VerificationPhase.Confirming -> ScreenAction(
        kind = ActionKind.Confirm,
        labelRes = R.string.face_verification_action_confirming,
        isEnabled = false,
        isLoading = true,
    )

    phase == VerificationPhase.Verified -> ScreenAction(
        kind = ActionKind.Confirm,
        labelRes = R.string.face_verification_action_verified,
        isEnabled = false,
    )

    else -> ScreenAction(
        kind = ActionKind.Confirm,
        labelRes = R.string.face_verification_action_confirm,
        isEnabled = isReadyToConfirm,
    )
}

@StringRes
private fun FaceVerificationUiState.titleRes(): Int = when {
    cameraPermission == CameraPermission.Denied -> R.string.face_verification_permission_title
    isCameraUnavailable -> R.string.face_verification_camera_unavailable_title
    phase == VerificationPhase.Confirming -> R.string.face_verification_confirming_title
    phase == VerificationPhase.Verified -> R.string.face_verification_verified_title
    alignment is FaceAlignment.Aligned -> R.string.face_verification_hold_still_title
    else -> R.string.face_verification_align_title
}

/**
 * The body text names the specific problem — "move closer" is actionable where "align your
 * face" is not.
 */
@StringRes
private fun FaceVerificationUiState.bodyRes(): Int = when {
    cameraPermission == CameraPermission.Denied -> R.string.face_verification_permission_body
    isCameraUnavailable -> R.string.face_verification_camera_unavailable_body
    failure != null -> failure.messageRes()
    phase == VerificationPhase.Confirming -> R.string.face_verification_confirming_body
    phase == VerificationPhase.Verified -> R.string.face_verification_verified_body
    else -> when (val current = alignment) {
        FaceAlignment.Aligned -> R.string.face_verification_hold_still_body
        is FaceAlignment.Misaligned -> current.reason.messageRes()
    }
}

@StringRes
private fun MisalignmentReason.messageRes(): Int = when (this) {
    MisalignmentReason.NoFace -> R.string.face_verification_align_body
    MisalignmentReason.MultipleFaces -> R.string.face_verification_body_multiple_faces
    MisalignmentReason.TooFar -> R.string.face_verification_body_too_far
    MisalignmentReason.TooClose -> R.string.face_verification_body_too_close
    MisalignmentReason.OffCentre -> R.string.face_verification_body_off_centre
    MisalignmentReason.Turned -> R.string.face_verification_body_turned
}

@StringRes
private fun FaceVerificationFailure.messageRes(): Int = when (this) {
    FaceVerificationFailure.NoMatch -> R.string.face_verification_failure_no_match
    FaceVerificationFailure.SensorUnavailable -> R.string.face_verification_failure_sensor
}

private val OfflineDotColor = Color(0xFF3ED598)

private const val OVAL_WIDTH_FRACTION = 0.72f
private const val OVAL_HEIGHT_FRACTION = 0.42f
private const val OVAL_TOP_FRACTION = 0.24f
private const val BRACKET_ARM_FRACTION = 0.12f
private const val SCRIM_ALPHA = 0.74f

@Preview
@Composable
private fun FaceVerificationScanningPreview() {
    AppTheme(darkTheme = true) {
        FaceVerificationScreen(
            // Permission left Unknown so the preview draws the overlay without trying to
            // open a camera that does not exist in a render host.
            uiState = FaceVerificationUiState(),
            onFacesDetected = {},
            onCameraError = {},
            onConfirm = {},
            onRetry = {},
            onRequestCameraPermission = {},
            onCancel = {},
        )
    }
}

@Preview
@Composable
private fun FaceVerificationConfirmingPreview() {
    AppTheme(darkTheme = true) {
        FaceVerificationScreen(
            uiState = FaceVerificationUiState(
                phase = VerificationPhase.Confirming,
                alignment = FaceAlignment.Aligned,
                stableAlignedFrames = FaceVerificationUiState.REQUIRED_STABLE_FRAMES,
            ),
            onFacesDetected = {},
            onCameraError = {},
            onConfirm = {},
            onRetry = {},
            onRequestCameraPermission = {},
            onCancel = {},
        )
    }
}
