package com.example.test_ai_project.home.presentation.settings.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.component.AppText
import com.example.test_ai_project.resource.preview.DevicePreview
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.AppTheme
import com.example.test_ai_project.resource.theme.sizes
import com.example.test_ai_project.resource.theme.spacing

/**
 * A titled group of settings, drawn as one card.
 *
 * The heading sits *outside* the card rather than as its first row, so the card boundary
 * marks exactly the things that can be interacted with — a heading inside it reads as a
 * disabled first row at a glance.
 */
@Composable
internal fun SettingsSection(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AppText(
            text = stringResource(id = titleRes),
            style = AppTextStyle.LabelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = spacing.small, bottom = spacing.small),
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(sizes.radius),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(spacing.medium), content = content)
        }
    }
}

@DevicePreview
@Composable
private fun SettingsSectionPreview() {
    AppTheme {
        SettingsSection(titleRes = ResR.string.settings_appearance_title) {
            AppText(text = stringResource(id = ResR.string.settings_dark_theme_label))
            Spacer(modifier = Modifier.height(spacing.small))
            AppText(
                text = stringResource(id = ResR.string.settings_dark_theme_caption),
                style = AppTextStyle.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
