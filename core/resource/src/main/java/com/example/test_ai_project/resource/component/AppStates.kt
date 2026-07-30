package com.example.test_ai_project.resource.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.test_ai_project.resource.R
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.sizes
import com.example.test_ai_project.resource.theme.spacing

/**
 * Shared loading and error surfaces.
 *
 * Every feature renders the same states, so they belong to the design system rather than
 * being re-invented per screen.
 */
@Composable
fun AppLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppProgressIndicator()
    }
}

@Composable
fun AppErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppText(
            text = message,
            style = AppTextStyle.Body,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
        )
        if (onRetry != null) {
            AppButton(
                text = stringResource(id = R.string.action_retry),
                onClick = onRetry,
                modifier = Modifier
                    .padding(top = spacing.medium)
                    .width(sizes.contentMaxWidth),
            )
        }
    }
}
