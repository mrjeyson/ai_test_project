package com.example.test_ai_project.resource.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Collects a ViewModel's one-shot effect stream from a screen.
 *
 * Collection is bound to [Lifecycle.State.STARTED] via [repeatOnLifecycle], which is what
 * makes an effect safe to act on: a navigation delivered while the screen is in the back
 * stack would otherwise fire against a destroyed nav controller. Buffered effects arrive
 * when the screen comes back instead.
 */
@Composable
fun <T> Flow<T>.CollectAsEffect(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    onEffect: suspend (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(this, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            collect { onEffect(it) }
        }
    }
}
