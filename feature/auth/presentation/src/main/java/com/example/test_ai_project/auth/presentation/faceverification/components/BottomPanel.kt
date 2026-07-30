package com.example.test_ai_project.auth.presentation.faceverification.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.test_ai_project.auth.presentation.faceverification.contract.FaceVerificationEvent
import com.example.test_ai_project.auth.presentation.faceverification.contract.FaceVerificationState
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.component.AppText
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.spacing

/** Guidance, the primary action, and the way out — the whole bottom half of the screen. */
@Composable
internal fun BottomPanel(
    state: FaceVerificationState,
    onEvent: (FaceVerificationEvent) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        InstructionCard(state = state)

        Spacer(modifier = Modifier.height(spacing.medium))

        val action = state.action()
        VerificationActionButton(
            labelRes = action.labelRes,
            isLoading = action.isLoading,
            enabled = action.isEnabled,
            onClick = when (action.kind) {
                ActionKind.Confirm -> {
                    { onEvent(FaceVerificationEvent.Confirmed) }
                }

                ActionKind.Retry -> {
                    { onEvent(FaceVerificationEvent.RetryRequested) }
                }

                ActionKind.GrantPermission -> onRequestCameraPermission
            },
        )

        Spacer(modifier = Modifier.height(spacing.small))

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            AppText(
                text = stringResource(id = ResR.string.face_verification_cancel),
                style = AppTextStyle.Label,
                color = Color.White,
            )
        }
    }
}
