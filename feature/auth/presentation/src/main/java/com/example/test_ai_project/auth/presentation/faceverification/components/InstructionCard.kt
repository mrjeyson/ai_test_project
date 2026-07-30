package com.example.test_ai_project.auth.presentation.faceverification.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.test_ai_project.auth.presentation.faceverification.contract.FaceVerificationState
import com.example.test_ai_project.resource.component.AppText
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.spacing

/** The guidance panel: what is wrong right now, and what to do about it. */
@Composable
internal fun InstructionCard(
    state: FaceVerificationState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(spacing.medium),
        color = Color.White.copy(alpha = 0.09f),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = spacing.large,
                vertical = spacing.medium,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppText(
                text = stringResource(id = state.titleRes()),
                style = AppTextStyle.Label,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(spacing.small))
            AppText(
                text = stringResource(id = state.bodyRes()),
                style = AppTextStyle.BodySmall,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
