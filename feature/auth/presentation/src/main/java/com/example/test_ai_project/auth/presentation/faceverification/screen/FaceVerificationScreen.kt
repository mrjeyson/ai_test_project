package com.example.test_ai_project.auth.presentation.faceverification.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.test_ai_project.auth.domain.model.FaceAlignment
import com.example.test_ai_project.auth.presentation.faceverification.camera.FaceCameraPreview
import com.example.test_ai_project.auth.presentation.faceverification.components.BottomPanel
import com.example.test_ai_project.auth.presentation.faceverification.components.FaceFrameOverlay
import com.example.test_ai_project.auth.presentation.faceverification.components.OfflineBadge
import com.example.test_ai_project.auth.presentation.faceverification.components.VerificationTopBar
import com.example.test_ai_project.auth.presentation.faceverification.contract.FaceVerificationEffect
import com.example.test_ai_project.auth.presentation.faceverification.contract.FaceVerificationEvent
import com.example.test_ai_project.auth.presentation.faceverification.contract.FaceVerificationState
import com.example.test_ai_project.auth.presentation.faceverification.contract.VerificationPhase
import com.example.test_ai_project.auth.presentation.faceverification.viewmodel.FaceVerificationViewModel
import com.example.test_ai_project.resource.preview.DevicePreview
import com.example.test_ai_project.resource.theme.AppTheme
import com.example.test_ai_project.resource.theme.spacing
import com.example.test_ai_project.resource.util.CollectAsEffect

/**
 * Stateful entry point: the only place in this file that touches Hilt, the ViewModel, or
 * the permission API.
 */
@Composable
fun FaceVerificationScreen(
    onVerified: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FaceVerificationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.onEvent(FaceVerificationEvent.CameraPermissionResult(isGranted))
        },
    )

    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        // Asked immediately: the screen is a viewfinder and nothing else, so there is no
        // useful state to show before the permission decision.
        if (isGranted) {
            viewModel.onEvent(FaceVerificationEvent.CameraPermissionResult(true))
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    viewModel.effects.CollectAsEffect { effect ->
        when (effect) {
            FaceVerificationEffect.Verified -> onVerified()
        }
    }

    FaceVerificationContent(
        state = state,
        onEvent = viewModel::onEvent,
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
fun FaceVerificationContent(
    state: FaceVerificationState,
    onEvent: (FaceVerificationEvent) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (state.isPreviewActive) {
            FaceCameraPreview(
                onFaces = { onEvent(FaceVerificationEvent.FacesDetected(it)) },
                onCameraError = { onEvent(FaceVerificationEvent.CameraFailed) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        FaceFrameOverlay(
            isAligned = state.alignment is FaceAlignment.Aligned,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            VerificationTopBar(
                modifier = Modifier.padding(
                    horizontal = spacing.medium,
                    vertical = spacing.small,
                ),
            )

            // The frame occupies the middle; the panel is pinned to the bottom.
            Spacer(modifier = Modifier.weight(1f))

            BottomPanel(
                state = state,
                onEvent = onEvent,
                onRequestCameraPermission = onRequestCameraPermission,
                onCancel = onCancel,
                modifier = Modifier.padding(horizontal = spacing.medium),
            )

            Spacer(modifier = Modifier.height(spacing.medium))
            OfflineBadge(modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(spacing.medium))
        }
    }
}

@DevicePreview
@Composable
private fun FaceVerificationContentPreview() {
    AppTheme {
        FaceVerificationContent(
            // Permission left Unknown so the preview draws the overlay without trying to
            // open a camera that does not exist in a render host.
            state = FaceVerificationState(
                phase = VerificationPhase.Confirming,
                alignment = FaceAlignment.Aligned,
                stableAlignedFrames = FaceVerificationState.REQUIRED_STABLE_FRAMES,
            ),
            onEvent = {},
            onRequestCameraPermission = {},
            onCancel = {},
        )
    }
}
