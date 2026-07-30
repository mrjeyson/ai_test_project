package com.example.test_ai_project.auth.presentation.faceverification.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.test_ai_project.auth.presentation.R
import com.example.test_ai_project.resource.component.AppIcon
import com.example.test_ai_project.resource.component.AppProgressIndicator
import com.example.test_ai_project.resource.component.AppText
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.VaultCharcoal
import com.example.test_ai_project.resource.theme.scaled
import com.example.test_ai_project.resource.theme.sizes
import com.example.test_ai_project.resource.theme.spacing

/**
 * White on a dark viewfinder, per the design — not the app's teal primary, which is why
 * this is a local button rather than `AppButton`.
 */
@Composable
internal fun VerificationActionButton(
    @StringRes labelRes: Int,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(sizes.buttonHeight),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(sizes.radius),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = VaultCharcoal,
            disabledContainerColor = Color.White.copy(alpha = 0.35f),
            disabledContentColor = VaultCharcoal.copy(alpha = 0.55f),
        ),
    ) {
        if (isLoading) {
            AppProgressIndicator(size = 18.scaled, color = VaultCharcoal)
        } else {
            AppIcon(
                id = R.drawable.ic_check_circle,
                contentDescription = null,
                size = 18.scaled,
            )
        }
        Spacer(modifier = Modifier.width(spacing.small))
        AppText(text = stringResource(id = labelRes), style = AppTextStyle.Label)
    }
}
