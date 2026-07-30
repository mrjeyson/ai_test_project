package com.example.test_ai_project.resource.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val VaultColorScheme = lightColorScheme(
    primary = VaultTeal,
    onPrimary = VaultOnTeal,
    secondary = VaultTeal,
    onSecondary = VaultOnTeal,
    tertiary = VaultTeal,
    background = VaultMist,
    onBackground = VaultCharcoal,
    surface = VaultCard,
    onSurface = VaultCharcoal,
    surfaceVariant = VaultMistDeep,
    onSurfaceVariant = VaultStone,
    surfaceContainer = VaultMistDeep,
    surfaceContainerHighest = VaultMistDeep,
    outline = VaultHairline,
    outlineVariant = VaultHairline,
)

/**
 * The single theme for the whole app.
 *
 * Lives in `:core:resource` so that every presentation module — and every preview inside
 * one — can wrap itself in the real theme without depending on `:app`.
 *
 * There is one scheme, not a light/dark pair: the designs are light-only, and the two
 * dark surfaces — the launch window and the viewfinder — are painted from the brand
 * tokens directly rather than through the colour scheme.
 *
 * Material You dynamic colour is deliberately absent: SecureVault ships a fixed palette,
 * and letting the wallpaper repaint the vault would break the identity the brand and
 * authentication surfaces are built around.
 *
 * Beyond Material's own locals it provides the scaled [Spacing] and [Sizes], computed once
 * here from the window width. Providing them pre-multiplied is what lets a screen write
 * `spacing.medium` and get a value that is already correct for the device.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val scale = currentScaleFactor()

    CompositionLocalProvider(
        LocalScaleFactor provides scale,
        LocalSpacing provides spacingFor(scale),
        LocalSizes provides sizesFor(scale),
    ) {
        MaterialTheme(
            colorScheme = VaultColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
