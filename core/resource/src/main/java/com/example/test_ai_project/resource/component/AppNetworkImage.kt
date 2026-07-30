package com.example.test_ai_project.resource.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * The only way a remote image reaches the screen.
 *
 * Centralising it means a missing image looks the same everywhere: a poster with no URL
 * shows the same neutral tile in every list rather than each screen inventing its own
 * empty box. Callers that have a better placeholder — a genre glyph, say — pass one as
 * [fallback].
 *
 * The `ImageLoader` itself, including the disk cache that makes cached images survive
 * going offline, is configured once by the application class.
 */
@Composable
fun AppNetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit = { AppImagePlaceholder() },
) {
    if (url.isNullOrBlank()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { fallback() }
        return
    }

    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}

/** The neutral tile shown where an image should be but is not. */
@Composable
fun AppImagePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}
