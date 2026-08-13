package com.example.test_ai_project.home.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.component.AppButton
import com.example.test_ai_project.resource.component.AppText
import com.example.test_ai_project.resource.preview.DevicePreview
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.AppTheme
import com.example.test_ai_project.resource.theme.spacing

/**
 * The sign-out action, with the sentence that makes it safe to press.
 *
 * No confirmation dialog, because there is nothing to confirm: signing out ends a session
 * that costs one form to start again and destroys nothing on the way out. A dialog here
 * would ask the user to think about a decision that has no consequences.
 *
 * The caption sits above the button, where it is read before the tap rather than after it.
 */
@Composable
internal fun SignOutRow(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AppText(
            text = stringResource(id = ResR.string.settings_sign_out_caption),
            style = AppTextStyle.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        AppButton(
            text = stringResource(id = ResR.string.settings_sign_out),
            onClick = onSignOut,
        )
    }
}

@DevicePreview
@Composable
private fun SignOutRowPreview() {
    AppTheme {
        SignOutRow(onSignOut = {})
    }
}
