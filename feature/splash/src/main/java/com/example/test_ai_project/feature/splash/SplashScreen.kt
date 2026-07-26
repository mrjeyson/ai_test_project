package com.example.test_ai_project.feature.splash

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.test_ai_project.core.ui.component.BrandLogo
import com.example.test_ai_project.core.ui.component.BrandWordmark
import com.example.test_ai_project.core.ui.theme.AppTheme
import com.example.test_ai_project.core.ui.theme.VaultInk
import com.example.test_ai_project.core.ui.theme.VaultInkElevated
import com.example.test_ai_project.core.ui.theme.VaultOnInk
import com.example.test_ai_project.core.ui.theme.VaultOnInkMuted
import com.example.test_ai_project.core.ui.theme.VaultOutline

/**
 * Stateful entry point: the only place in this file that touches Hilt or the ViewModel.
 * Keeping it separate from [SplashScreen] is what makes the screen previewable and
 * testable without a DI graph.
 */
@Composable
fun SplashRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Keyed on the boolean, not on `uiState`: progress updates must not re-fire
    // navigation, and a recomposition after Ready must not fire it twice.
    val isReady = uiState is SplashUiState.Ready
    LaunchedEffect(isReady) {
        if (isReady) onFinished()
    }

    SplashScreen(uiState = uiState, modifier = modifier)
}

/** Stateless and side-effect free — driven entirely by its parameters. */
@Composable
fun SplashScreen(
    uiState: SplashUiState,
    modifier: Modifier = Modifier,
) {
    val targetProgress = when (uiState) {
        is SplashUiState.Initializing -> uiState.progress
        SplashUiState.Ready -> 1f
    }
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = PROGRESS_ANIMATION_MILLIS),
        label = "bootstrapProgress",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            // The gradient lifts the top of the screen just enough to seat the logo;
            // it is flat ink from mid-screen down.
            .background(
                Brush.verticalGradient(
                    0f to VaultInkElevated,
                    0.45f to VaultInk,
                    1f to VaultInk,
                ),
            )
            .safeDrawingPadding()
            .padding(top = LogoTopPadding),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandLogo()
            Spacer(modifier = Modifier.height(12.dp))
            // Colours are pinned to the ink tokens rather than read from the colour
            // scheme: the app theme is light, and this one screen is not.
            BrandWordmark(color = VaultOnInk)
        }

        Spacer(modifier = Modifier.height(28.dp))

        BootstrapStatus(
            uiState = uiState,
            progress = progress,
            // Asymmetric on purpose: the status block hangs off the left margin while
            // the bar stops short of the right edge, as in the design.
            modifier = Modifier.padding(start = 20.dp, end = 52.dp),
        )
    }
}

@Composable
private fun BootstrapStatus(
    uiState: SplashUiState,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = uiState.captionRes()),
            style = MaterialTheme.typography.labelSmall,
            color = VaultOnInkMuted,
            // Two lines are reserved so a shorter caption does not shift the bar upward.
            minLines = 2,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )

        Spacer(modifier = Modifier.height(10.dp))

        BootstrapProgressBar(progress = progress)
    }
}

/**
 * A 2dp rule rather than a [androidx.compose.material3.LinearProgressIndicator]: the
 * design has no stop indicator, no track gap and no minimum height, and fighting those
 * defaults costs more than drawing two rounded rects.
 */
@Composable
private fun BootstrapProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val fraction = progress.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = VaultOutline, cornerRadius = radius)
        if (fraction > 0f) {
            drawRoundRect(
                color = VaultOnInk,
                size = Size(width = size.width * fraction, height = size.height),
                cornerRadius = radius,
            )
        }
    }
}

@StringRes
private fun SplashUiState.captionRes(): Int = when (this) {
    is SplashUiState.Initializing -> when (stage) {
        BootstrapStage.LocalEnvironment -> R.string.splash_stage_local_environment
        BootstrapStage.SecureStorage -> R.string.splash_stage_secure_storage
        BootstrapStage.Session -> R.string.splash_stage_session
    }

    SplashUiState.Ready -> R.string.splash_stage_ready
}

private val LogoTopPadding = 24.dp
private const val PROGRESS_ANIMATION_MILLIS = 400

@Preview
@Composable
private fun SplashScreenInitializingPreview() {
    AppTheme(darkTheme = true) {
        SplashScreen(
            uiState = SplashUiState.Initializing(
                stage = BootstrapStage.LocalEnvironment,
                progress = 0.33f,
            ),
        )
    }
}

@Preview
@Composable
private fun SplashScreenReadyPreview() {
    AppTheme(darkTheme = true) {
        SplashScreen(uiState = SplashUiState.Ready)
    }
}
