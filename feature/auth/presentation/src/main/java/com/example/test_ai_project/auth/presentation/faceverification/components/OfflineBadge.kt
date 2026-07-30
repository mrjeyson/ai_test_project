package com.example.test_ai_project.auth.presentation.faceverification.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.component.AppText
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.scaled
import com.example.test_ai_project.resource.theme.spacing

/** The "nothing leaves this device" reassurance, pinned under the action panel. */
@Composable
internal fun OfflineBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = Color.White.copy(alpha = 0.10f),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = spacing.medium,
                vertical = spacing.small,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(7.scaled)
                    .background(color = OfflineDotColor, shape = CircleShape),
            )
            Spacer(modifier = Modifier.width(spacing.small))
            AppText(
                text = stringResource(id = ResR.string.face_verification_processing_offline),
                style = AppTextStyle.LabelSmall,
                color = Color.White.copy(alpha = 0.82f),
            )
        }
    }
}

private val OfflineDotColor = Color(0xFF3ED598)
