package com.example.test_ai_project.auth.presentation.login.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.component.AppIcon
import com.example.test_ai_project.resource.component.AppText
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.VaultLabel
import com.example.test_ai_project.resource.theme.scaled
import com.example.test_ai_project.resource.theme.spacing

/** The "processed on this device" reassurance under the credentials card. */
@Composable
internal fun TrustChip(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = spacing.medium,
                vertical = spacing.small,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(
                id = ResR.drawable.ic_shield,
                contentDescription = null,
                size = 13.scaled,
                tint = VaultLabel,
            )
            Spacer(modifier = Modifier.width(spacing.small))
            AppText(
                text = stringResource(id = ResR.string.login_trust_chip),
                style = AppTextStyle.LabelSmall,
                color = VaultLabel,
            )
        }
    }
}
