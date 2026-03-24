package com.prathik.fairshare.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand ─────────────────────────────────────────────────────────────────────
val Green400 = Color(0xFF22C97A)   // Primary — positive balances, CTAs, buttons
val Green600 = Color(0xFF1A9E5E)   // Pressed state
val Green900 = Color(0xFF0D3D22)   // Tinted surface — "you lent" badge bg
val Green200 = Color(0xFF7EEDB5)   // Subtle text on dark bg

val Orange400 = Color(0xFFFF6B35)  // Negative — "you owe"
val Orange900 = Color(0xFF2A1500)  // Tinted surface for negative states

// ── App backgrounds ───────────────────────────────────────────────────────────
val Black    = Color(0xFF0A0A0B)   // Deepest — splash only
val Surface0 = Color(0xFF111112)   // App background
val Surface1 = Color(0xFF161618)   // Bottom nav, secondary bg
val Surface2 = Color(0xFF1A1A1C)   // Cards, inputs, chips
val Surface3 = Color(0xFF1E1E20)   // Dividers
val Surface4 = Color(0xFF2A2A2D)   // Borders, outlines

// ── Text ──────────────────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFAAAAAA)
val TextTertiary  = Color(0xFF666666)
val TextDisabled  = Color(0xFF444444)

// ── Semantic ──────────────────────────────────────────────────────────────────
val Positive   = Green400
val Negative   = Orange400
val PositiveBg = Green900
val NegativeBg = Orange900

// ── Avatar colors — assigned by hashing userId ────────────────────────────────
val AvatarColors = listOf(
    Color(0xFF4A6FE8),
    Color(0xFF8B4FE8),
    Color(0xFFE86A4F),
    Color(0xFF1D7A52),
    Color(0xFFE8A84F),
    Color(0xFF4FE8B3),
    Color(0xFFE84F8B),
    Color(0xFF6AE84F),
)

// ── Group tile gradients ───────────────────────────────────────────────────────
val TileHomeStart    = Color(0xFF1A2E26)
val TileHomeEnd      = Color(0xFF0D1F19)
val TileHomeBorder   = Color(0xFF1D3D2E)

val TileTripStart    = Color(0xFF1A2040)
val TileTripEnd      = Color(0xFF0D1428)
val TileTripBorder   = Color(0xFF1D2A5A)

val TileCoupleStart  = Color(0xFF2A1A2E)
val TileCoupleEnd    = Color(0xFF1A0D20)
val TileCoupleBorder = Color(0xFF3D1D4A)

val TileOfficeStart  = Color(0xFF2A2010)
val TileOfficeEnd    = Color(0xFF1A1408)
val TileOfficeBorder = Color(0xFF4A3A1A)

val TileFriendsStart  = Color(0xFF1A1A2E)
val TileFriendsEnd    = Color(0xFF0D0D1E)
val TileFriendsBorder = Color(0xFF2A2A4A)

val TileEventStart  = Color(0xFF2A1A1A)
val TileEventEnd    = Color(0xFF1E0D0D)
val TileEventBorder = Color(0xFF4A2020)

val TileOtherStart  = Color(0xFF1A1A1A)
val TileOtherEnd    = Color(0xFF111112)
val TileOtherBorder = Color(0xFF2A2A2D)