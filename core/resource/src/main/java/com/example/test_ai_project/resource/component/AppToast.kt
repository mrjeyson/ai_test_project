package com.example.test_ai_project.resource.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.sizes
import com.example.test_ai_project.resource.theme.spacing
import kotlinx.coroutines.delay

/** How a message is coloured. Errors are the common case; success is for confirmations. */
enum class AppToastKind { Error, Success }

@Immutable
data class AppToastMessage(
    val text: String,
    val kind: AppToastKind = AppToastKind.Error,
    /** Distinguishes two identical messages, so the second one still re-shows the toast. */
    val id: Long = 0L,
)

/**
 * The handle a screen uses to raise a message.
 *
 * Screens never render an error themselves: a ViewModel emits a one-shot effect, the
 * screen forwards it here, and the single [AppToastHost] at the root of the app draws it.
 * One host means two screens cannot stack two toasts on top of each other.
 */
@Stable
class AppToastState {
    var current: AppToastMessage? by mutableStateOf(null)
        private set

    private var counter = 0L

    fun show(text: String, kind: AppToastKind = AppToastKind.Error) {
        counter += 1
        current = AppToastMessage(text = text, kind = kind, id = counter)
    }

    fun dismiss() {
        current = null
    }
}

@Composable
fun rememberAppToastState(): AppToastState = remember { AppToastState() }

/**
 * Provided once at the root. Reading it from a screen that is not under an
 * [AppToastHost] is a wiring bug, so the default fails loudly rather than silently
 * swallowing every error message in the app.
 */
val LocalAppToast = compositionLocalOf<AppToastState> {
    error("No AppToastState provided. Wrap the nav host in AppToastHost.")
}

/**
 * Draws the current message at the top of the window and clears it after [DISPLAY_MILLIS].
 *
 * Keyed on the message id rather than the text, so raising the same error twice restarts
 * the timer instead of being treated as no change at all.
 */
@Composable
fun AppToastHost(
    state: AppToastState,
    modifier: Modifier = Modifier,
) {
    val message = state.current

    LaunchedEffect(message?.id) {
        if (message != null) {
            delay(DISPLAY_MILLIS)
            state.dismiss()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        // Held so the text does not blank out mid-exit-animation as state clears.
        val shown = remember(message?.id) { message }
        if (shown != null) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium, vertical = spacing.small)
                    .background(
                        color = when (shown.kind) {
                            AppToastKind.Error -> MaterialTheme.colorScheme.error
                            AppToastKind.Success -> MaterialTheme.colorScheme.primary
                        },
                        shape = RoundedCornerShape(sizes.radius),
                    )
                    .padding(horizontal = spacing.medium, vertical = spacing.small),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = shown.text,
                    style = AppTextStyle.BodySmall,
                    color = when (shown.kind) {
                        AppToastKind.Error -> MaterialTheme.colorScheme.onError
                        AppToastKind.Success -> MaterialTheme.colorScheme.onPrimary
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private const val DISPLAY_MILLIS = 3_000L
