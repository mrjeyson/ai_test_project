package com.example.test_ai_project.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
 * Lives in `:core:ui` so that every feature module — and every `@Preview` inside one —
 * can wrap itself in the real theme without depending on `:app`.
 *
 * There is one scheme, not a light/dark pair: the designs are light-only, and the two
 * dark surfaces — the launch window and the viewfinder — are painted from the brand
 * tokens directly rather than through the colour scheme.
 *
 * Material You dynamic colour is deliberately absent: SecureVault ships a fixed palette,
 * and letting the wallpaper repaint the vault would break the identity the brand and
 * authentication surfaces are built around.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VaultColorScheme,
        typography = Typography,
        content = content,
    )
}
