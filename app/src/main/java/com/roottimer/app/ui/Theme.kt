package com.roottimer.app.ui

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/* Dopamine-inspired light palette: white cards on a cool near-white
 * background, blue-violet accent. */
val Accent = Color(0xFF5B6CFF)
val Accent2 = Color(0xFF8A5CFF)
val AccentSoft = Color(0xFFEEF0FF)
val TextPrimary = Color(0xFF171A26)
val TextSecondary = Color(0xFF6B7280)
val Danger = Color(0xFFE5484D)
val Warn = Color(0xFFF0A020)
val CardBg = Color(0xFFFFFFFF)
val BgTop = Color(0xFFF8F9FF)
val BgBottom = Color(0xFFEEF0FF)
val DividerColor = Color(0xFFE9EBF3)

val RootTimerColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentSoft,
    onPrimaryContainer = TextPrimary,
    secondary = Accent2,
    onSecondary = Color.White,
    background = BgTop,
    onBackground = TextPrimary,
    surface = CardBg,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F3FA),
    onSurfaceVariant = TextSecondary,
    error = Danger,
    onError = Color.White,
)
