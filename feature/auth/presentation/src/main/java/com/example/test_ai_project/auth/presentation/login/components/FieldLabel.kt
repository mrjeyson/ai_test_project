package com.example.test_ai_project.auth.presentation.login.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.test_ai_project.resource.component.AppIcon
import com.example.test_ai_project.resource.component.AppText
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.VaultLabel
import com.example.test_ai_project.resource.theme.scaled
import com.example.test_ai_project.resource.theme.spacing

/** The icon-and-caption heading above each credential field. */
@Composable
internal fun FieldLabel(
    @DrawableRes iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AppIcon(
            id = iconRes,
            // Decorative: the label next to it already says what the field is.
            contentDescription = null,
            size = 14.scaled,
            tint = VaultLabel,
        )
        Spacer(modifier = Modifier.width(spacing.small))
        AppText(text = label, style = AppTextStyle.LabelSmall, color = VaultLabel)
    }
}
