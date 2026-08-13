package com.example.test_ai_project.resource.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// SecureVault brand palette. Named after the role each colour plays, not after the hue,
// so a re-skin touches this file and nothing else.

/** The brand accent: primary buttons, links, the logo tile on light surfaces. */
val VaultTeal = Color(0xFF0F7A6B)

/** The accent lightened for the viewfinder, where [VaultTeal] fails contrast. */
val VaultTealLight = Color(0xFF5FD0BC)

val VaultOnTeal = Color(0xFFFFFFFF)

// Light surfaces — authentication and everything past it.

/** Page background: an off-white with a green cast, so white cards read as raised. */
val VaultMist = Color(0xFFEFF4F3)
val VaultCard = Color(0xFFFFFFFF)

/** Chip fills and inactive tracks. */
val VaultMistDeep = Color(0xFFE4EBEA)

val VaultCharcoal = Color(0xFF101A20)

/** Field labels — darker than [VaultStone], which would disappear at 11sp. */
val VaultLabel = Color(0xFF3D4B53)

/** Helper text, placeholders, body copy. */
val VaultStone = Color(0xFF5E6C74)

/** Field borders and dividers — visible without competing with the content. */
val VaultHairline = Color(0xFFE2E8E8)

// Dark surfaces — the launch window, which the platform draws before Compose exists, and
// since the theme toggle landed, the dark scheme's page background too.
// Both are mirrored in the app module's colors.xml and must be kept in step with it.

/** The launch window background, and the dark theme's page background. */
val VaultInk = Color(0xFF171B28)

/** Darker than the background: the logo tile reads as a cut-out, not a raised card. */
val VaultInkDeep = Color(0xFF0D0E14)

// The rest of the dark scheme. Deliberately a separate set rather than alpha overlays on
// the light tokens: a translucent white over ink drifts blue as it stacks, and cards here
// stack two deep.

/** Card surface: lifted off [VaultInk] so a card reads as raised, exactly as white does on mist. */
val VaultInkCard = Color(0xFF1F2433)

/** Chip fills and inactive tracks — the dark counterpart of [VaultMistDeep]. */
val VaultInkDeepVariant = Color(0xFF2A3040)

/** Dividers and field borders on dark surfaces. */
val VaultInkHairline = Color(0xFF313A4D)

/** Body copy on dark surfaces — the counterpart of [VaultCharcoal]. */
val VaultCloud = Color(0xFFE7ECF2)

/** Helper text and captions on dark surfaces — the counterpart of [VaultStone]. */
val VaultAsh = Color(0xFF9BA7B4)

/**
 * The field-label colour, in whichever scheme is current.
 *
 * The one brand colour with no Material role behind it. [VaultLabel] is deliberately darker
 * than `onSurfaceVariant` because an 11sp uppercase label needs the extra weight, so it
 * cannot simply become that role — but left as a constant it would be ink-on-ink in the dark
 * scheme. Same reasoning in both directions: one step stronger than the body captions.
 */
val vaultFieldLabel: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) VaultCloud else VaultLabel
