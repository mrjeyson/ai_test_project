package com.example.test_ai_project.resource.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.sizes
import com.example.test_ai_project.resource.theme.spacing

/**
 * The full-width primary action.
 *
 * While [isLoading] the button keeps its footprint and swaps the label for a spinner —
 * a collapsing or disappearing button during submission shifts everything under it.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(sizes.buttonHeight),
        // Loading is a form of disabled: a second tap must not submit twice.
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(sizes.radius),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(sizes.icon),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(text = text, style = AppTextStyle.Label)
                if (trailing != null) {
                    Spacer(modifier = Modifier.width(spacing.small))
                    trailing()
                }
            }
        }
    }
}
