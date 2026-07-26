package com.example.test_ai_project.core.ui.theme

import androidx.compose.ui.graphics.Color

// SecureVault brand palette. Named after the role each colour plays, not after the hue,
// so a re-skin touches this file and nothing else.

/** The brand accent: primary buttons, links, the logo tile on light surfaces. */
val VaultTeal = Color(0xFF0F7A6B)

/** The accent lightened for dark surfaces, where [VaultTeal] fails contrast. */
val VaultTealLight = Color(0xFF5FD0BC)

val VaultOnTeal = Color(0xFFFFFFFF)
val VaultOnTealDark = Color(0xFF00382F)

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

// Dark surfaces — the splash, which stays dark whatever the system setting.

val VaultInk = Color(0xFF171B28)
val VaultInkElevated = Color(0xFF1E2233)

/** Darker than the background: the logo tile reads as a cut-out, not a raised card. */
val VaultInkDeep = Color(0xFF0D0E14)

val VaultOnInk = Color(0xFFE4E2F2)
val VaultOnInkMuted = Color(0xFF6C7189)
val VaultOutline = Color(0xFF262B3D)
