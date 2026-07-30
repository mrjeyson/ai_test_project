package com.example.test_ai_project.auth.presentation.login.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.test_ai_project.resource.component.AppText
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.spacing

/**
 * A per-field complaint, shown under the field it belongs to.
 *
 * Distinct from the app's toast: a field error persists until the user fixes that field,
 * whereas a vault-level failure is transient and belongs to the toast host.
 */
@Composable
internal fun FieldError(
    @StringRes messageRes: Int,
    modifier: Modifier = Modifier,
) {
    AppText(
        text = stringResource(id = messageRes),
        style = AppTextStyle.Caption,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier.padding(top = spacing.small),
    )
}
