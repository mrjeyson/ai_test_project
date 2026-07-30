package com.example.test_ai_project.resource.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Responsive scaling without breakpoints.
 *
 * Rather than switching layouts at a width threshold, every dimension is multiplied by one
 * factor derived from the screen width. A phone sits at 1.0 and changes nothing; a tablet
 * grows text and spacing proportionally instead of stretching a phone layout across a
 * wider viewport.
 *
 * The clamp matters more than the ratio: unbounded scaling on a large tablet produces
 * comically large buttons, so the factor stops at [MAX_SCALE].
 */
object ScreenMetrics {
    const val BASE_WIDTH_DP = 360f
    const val MIN_SCALE = 1.0f
    const val MAX_SCALE = 1.2f

    fun scaleFactor(screenWidthDp: Int): Float =
        (screenWidthDp / BASE_WIDTH_DP).coerceIn(MIN_SCALE, MAX_SCALE)
}

/**
 * The spacing scale. Three steps, because a fourth would be chosen arbitrarily and
 * immediately drift.
 *
 * Values here are already multiplied by the screen scale — a screen reads `spacing.medium`
 * and never does the arithmetic itself.
 */
data class Spacing(
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
)

/** Fixed component dimensions, likewise pre-scaled. */
data class Sizes(
    val buttonHeight: Dp = 52.dp,
    val icon: Dp = 20.dp,
    val iconLarge: Dp = 32.dp,
    val radius: Dp = 10.dp,
    val avatar: Dp = 64.dp,
    val contentMaxWidth: Dp = 480.dp,
)

val LocalSpacing = compositionLocalOf { Spacing() }
val LocalSizes = compositionLocalOf { Sizes() }
val LocalScaleFactor = compositionLocalOf { 1f }

/** `16.scaled` — a dimension that respects the screen scale. Never use `.dp` in a screen. */
val Int.scaled: Dp
    @Composable @ReadOnlyComposable
    get() = (this * LocalScaleFactor.current).dp

/** `14.scaledSp` — the type equivalent of [scaled]. */
val Int.scaledSp: TextUnit
    @Composable @ReadOnlyComposable
    get() = (this * LocalScaleFactor.current).sp

internal fun spacingFor(scale: Float) = Spacing(
    small = (8 * scale).dp,
    medium = (16 * scale).dp,
    large = (24 * scale).dp,
)

internal fun sizesFor(scale: Float) = Sizes(
    buttonHeight = (52 * scale).dp,
    icon = (20 * scale).dp,
    iconLarge = (32 * scale).dp,
    radius = (10 * scale).dp,
    avatar = (64 * scale).dp,
    contentMaxWidth = (480 * scale).dp,
)

/** Shorthand so screens read `spacing.medium` rather than `LocalSpacing.current.medium`. */
val spacing: Spacing
    @Composable @ReadOnlyComposable
    get() = LocalSpacing.current

val sizes: Sizes
    @Composable @ReadOnlyComposable
    get() = LocalSizes.current

/** The scale for the current window, used by [AppTheme] to build the two scales above. */
@Composable
@ReadOnlyComposable
internal fun currentScaleFactor(): Float =
    ScreenMetrics.scaleFactor(LocalConfiguration.current.screenWidthDp)
