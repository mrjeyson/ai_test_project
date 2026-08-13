package com.example.test_ai_project.resource.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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
    // The one surface that deliberately opposes the page: the highlighted prayer row. A
    // named role rather than a hardcoded charcoal at the call site, because "the card that
    // contrasts with everything around it" has to mean something different in each scheme.
    inverseSurface = VaultCharcoal,
    inverseOnSurface = Color.White,
)

/**
 * The dark scheme, role for role.
 *
 * [VaultTealLight] carries the accent rather than [VaultTeal]: the brand teal is mixed for
 * white backgrounds and drops under 3:1 against ink, which is the same reason the viewfinder
 * has always used the lighter token.
 */
private val VaultDarkColorScheme = darkColorScheme(
    primary = VaultTealLight,
    onPrimary = VaultInkDeep,
    secondary = VaultTealLight,
    onSecondary = VaultInkDeep,
    tertiary = VaultTealLight,
    background = VaultInk,
    onBackground = VaultCloud,
    surface = VaultInkCard,
    onSurface = VaultCloud,
    surfaceVariant = VaultInkDeepVariant,
    onSurfaceVariant = VaultAsh,
    surfaceContainer = VaultInkDeepVariant,
    surfaceContainerHighest = VaultInkDeepVariant,
    outline = VaultInkHairline,
    outlineVariant = VaultInkHairline,
    // Inverted the other way: on ink, the card that stands apart is the *deeper* one, and
    // the white-on-dark content the light scheme already puts there keeps working unchanged.
    inverseSurface = VaultInkDeep,
    inverseOnSurface = Color.White,
)

/**
 * Whether the dark scheme is the one being painted.
 *
 * For the few brand colours that are not a Material role and still have to flip — see
 * [vaultFieldLabel]. Screens should reach for a `MaterialTheme.colorScheme` role first;
 * this exists for the cases where no role means the right thing.
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/**
 * The theme for the whole app, in one of two schemes.
 *
 * Lives in `:core:resource` so that every presentation module — and every preview inside
 * one — can wrap itself in the real theme without depending on `:app`.
 *
 * [darkTheme] is a parameter rather than a read of [ThemeService] here, and deliberately so:
 * a composable that injected its own store could not be previewed, and the choice is the
 * app's to make once at the root. `:app` collects [ThemeService.mode] and passes the answer
 * down; a preview passes nothing and gets the light designs.
 *
 * It does not follow the system setting either. The stored preference is the whole of the
 * decision, because a user who picks a theme on this screen has said what they want more
 * specifically than their phone-wide default does.
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
fun AppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scale = currentScaleFactor()

    CompositionLocalProvider(
        LocalScaleFactor provides scale,
        LocalSpacing provides spacingFor(scale),
        LocalSizes provides sizesFor(scale),
        LocalIsDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) VaultDarkColorScheme else VaultColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
