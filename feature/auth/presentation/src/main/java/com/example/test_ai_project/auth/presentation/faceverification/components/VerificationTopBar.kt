package com.example.test_ai_project.auth.presentation.faceverification.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.test_ai_project.auth.presentation.R
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.component.AppIcon
import com.example.test_ai_project.resource.component.AppText
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.scaled
import com.example.test_ai_project.resource.theme.spacing

/** Brand, screen title, and the on-device status badge. */
@Composable
internal fun VerificationTopBar(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(
            id = ResR.drawable.ic_shield,
            contentDescription = null,
            size = 22.scaled,
            tint = Color.White,
        )
        Spacer(modifier = Modifier.width(spacing.small))
        Column {
            AppText(
                text = stringResource(id = ResR.string.brand_name),
                style = AppTextStyle.Label,
                color = Color.White,
            )
            AppText(
                text = stringResource(id = ResR.string.face_verification_title),
                style = AppTextStyle.Caption,
                color = Color.White.copy(alpha = 0.62f),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // A status badge, not a control: the model is bundled in the APK, so "on device"
        // is a fact to display rather than an action to offer.
        Surface(
            shape = RoundedCornerShape(spacing.small),
            color = Color.White.copy(alpha = 0.10f),
        ) {
            AppIcon(
                id = R.drawable.ic_offline_ready,
                contentDescription = stringResource(
                    id = ResR.string.face_verification_offline_badge,
                ),
                size = 18.scaled,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(spacing.small),
            )
        }
    }
}
