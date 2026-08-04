package com.example.tldr_ai.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The palette is deliberately hue-free. The only colour in the app is the clickbait verdict,
 * so colour always means exactly one thing: how much the headline is trying to manipulate you.
 */

// Dark — "ink"
val InkBackground = Color(0xFF0A0B0D)
val InkSurface = Color(0xFF121418)
val InkSurfaceHigh = Color(0xFF1B1F24)
val InkOutline = Color(0xFF262B31)
val InkOnSurface = Color(0xFFEDEFF2)
val InkMuted = Color(0xFF8A9199)

// Light — "paper"
val PaperBackground = Color(0xFFFBFAF8)
val PaperSurface = Color(0xFFF2F1ED)
val PaperSurfaceHigh = Color(0xFFE8E7E2)
val PaperOutline = Color(0xFFDCDAD3)
val PaperOnSurface = Color(0xFF14161A)
val PaperMuted = Color(0xFF6B7178)

// Verdict scale — the app's only hues
val VerdictCalmDark = Color(0xFF74D6A3)
val VerdictSpinDark = Color(0xFFE6B265)
val VerdictBaitDark = Color(0xFFEF7C6D)

val VerdictCalmLight = Color(0xFF1C8557)
val VerdictSpinLight = Color(0xFFA97220)
val VerdictBaitLight = Color(0xFFC0433A)
