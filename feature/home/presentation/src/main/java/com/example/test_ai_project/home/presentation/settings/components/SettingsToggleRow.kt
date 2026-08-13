package com.example.test_ai_project.home.presentation.settings.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.component.AppSwitch
import com.example.test_ai_project.resource.component.AppText
import com.example.test_ai_project.resource.preview.DevicePreview
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.AppTheme
import com.example.test_ai_project.resource.theme.spacing

/**
 * A labelled setting with a switch.
 *
 * The whole row is the target, not just the thumb: [toggleable] with [Role.Switch] gives a
 * finger the full width to aim at and lets a screen reader announce the label, the state and
 * the action as one control. The switch itself is passed a null callback for that reason —
 * with its own handler the tap would be counted twice and the setting would land back where
 * it started.
 */
@Composable
internal fun SettingsToggleRow(
    @StringRes labelRes: Int,
    @StringRes captionRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = spacing.medium)) {
            AppText(
                text = stringResource(id = labelRes),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(spacing.small))
            AppText(
                text = stringResource(id = captionRes),
                style = AppTextStyle.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AppSwitch(checked = checked, onCheckedChange = null)
    }
}

@DevicePreview
@Composable
private fun SettingsToggleRowPreview() {
    AppTheme {
        SettingsToggleRow(
            labelRes = ResR.string.settings_dark_theme_label,
            captionRes = ResR.string.settings_dark_theme_caption,
            checked = true,
            onCheckedChange = {},
        )
    }
}
