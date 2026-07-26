package com.example.test_ai_project.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
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

private val DarkColorScheme = darkColorScheme(
    primary = VaultTealLight,
    onPrimary = VaultOnTealDark,
    secondary = VaultTealLight,
    onSecondary = VaultOnTealDark,
    tertiary = VaultTealLight,
    background = VaultInk,
    onBackground = VaultOnInk,
    surface = VaultInk,
    onSurface = VaultOnInk,
    surfaceVariant = VaultInkElevated,
    onSurfaceVariant = VaultOnInkMuted,
    surfaceContainer = VaultInkElevated,
    surfaceContainerHighest = VaultInkElevated,
    outline = VaultOutline,
    outlineVariant = VaultOutline,
)

/**
 * The single theme for the whole app.
 *
 * Lives in `:core:ui` so that every feature module — and every `@Preview` inside one —
 * can wrap itself in the real theme without depending on `:app`.
 *
 * Material You dynamic colour is deliberately absent: SecureVault ships a fixed palette,
 * and letting the wallpaper repaint the vault would break the identity the splash and
 * authentication surfaces are built around.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
