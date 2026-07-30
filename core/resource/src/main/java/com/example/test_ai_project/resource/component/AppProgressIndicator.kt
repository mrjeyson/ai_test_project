package com.example.test_ai_project.resource.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.test_ai_project.resource.theme.sizes

/** The one spinner. Sized from the design system so progress reads the same everywhere. */
@Composable
fun AppProgressIndicator(
    modifier: Modifier = Modifier,
    size: Dp? = null,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    CircularProgressIndicator(
        modifier = modifier.size(size ?: sizes.iconLarge),
        color = color,
        strokeWidth = 2.dp,
    )
}
