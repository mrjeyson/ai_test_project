package com.example.test_ai_project.auth.presentation.faceverification.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.test_ai_project.auth.domain.model.FaceObservation
import java.util.concurrent.Executors

/**
 * The front-camera viewfinder, with face detection bound to the same lifecycle.
 *
 * Binding lives in a [DisposableEffect] rather than in the ViewModel: the camera is tied
 * to this composition and to the Android lifecycle, and a ViewModel that outlives the
 * screen must not be holding a camera open.
 */
@Composable
internal fun FaceCameraPreview(
    onFaces: (List<FaceObservation>) -> Unit,
    onCameraError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            // FILL_CENTER, so the frame the user aligns to is not letterboxed.
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // The analyser is created once but the callbacks recompose; rememberUpdatedState is
    // what stops it from calling a stale lambda for the rest of the session.
    val currentOnFaces by rememberUpdatedState(onFaces)
    val currentOnCameraError by rememberUpdatedState(onCameraError)

    AndroidView(factory = { previewView }, modifier = modifier)

    DisposableEffect(lifecycleOwner) {
        // One background thread: STRATEGY_KEEP_ONLY_LATEST means a slow frame is dropped
        // rather than queued, so there is nothing to parallelise.
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val analyzer = FaceAnalyzer(
            onFaces = { faces -> currentOnFaces(faces) },
            onError = { currentOnCameraError() },
        )

        val providerFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null

        providerFuture.addListener(
            {
                runCatching {
                    val provider = providerFuture.get()
                    cameraProvider = provider

                    val preview = Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .apply { setAnalyzer(analysisExecutor, analyzer) }

                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        analysis,
                    )
                }.onFailure {
                    // No front camera, or the camera is held by another app. Reported, not
                    // thrown: the screen has a state for it.
                    currentOnCameraError()
                }
            },
            // bindToLifecycle must run on the main thread.
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            cameraProvider?.unbindAll()
            analyzer.close()
            analysisExecutor.shutdown()
        }
    }
}
