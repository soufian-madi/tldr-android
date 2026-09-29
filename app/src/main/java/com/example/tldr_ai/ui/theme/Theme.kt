package com.example.tldr_ai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat

/**
 * The app's own palette, carried alongside the Material scheme.
 *
 * Material's colour slots don't distinguish the three surface tones or the verdict hues we
 * need, so components read those from here and let stock Material widgets (dialogs, sheets,
 * buttons) pick up the mapped [MaterialTheme.colorScheme] instead.
 */
@Immutable
data class TldrPalette(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val onSurface: Color,
    val muted: Color,
    val calm: Color,
    val spin: Color,
    val bait: Color,
    val isDark: Boolean
)

private val DarkPalette = TldrPalette(
    background = InkBackground,
    surface = InkSurface,
    surfaceHigh = InkSurfaceHigh,
    outline = InkOutline,
    onSurface = InkOnSurface,
    muted = InkMuted,
    calm = VerdictCalmDark,
    spin = VerdictSpinDark,
    bait = VerdictBaitDark,
    isDark = true
)

private val LightPalette = TldrPalette(
    background = PaperBackground,
    surface = PaperSurface,
    surfaceHigh = PaperSurfaceHigh,
    outline = PaperOutline,
    onSurface = PaperOnSurface,
    muted = PaperMuted,
    calm = VerdictCalmLight,
    spin = VerdictSpinLight,
    bait = VerdictBaitLight,
    isDark = false
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

/** Shorthand for [LocalPalette] at the call site: `palette.muted`. */
val palette: TldrPalette
    @Composable get() = LocalPalette.current

private fun TldrPalette.toColorScheme() = if (isDark) {
    darkColorScheme(
        primary = onSurface,
        onPrimary = background,
        primaryContainer = surfaceHigh,
        onPrimaryContainer = onSurface,
        secondary = muted,
        onSecondary = background,
        secondaryContainer = surfaceHigh,
        onSecondaryContainer = onSurface,
        tertiary = onSurface,
        onTertiary = background,
        background = background,
        onBackground = onSurface,
        surface = background,
        onSurface = onSurface,
        surfaceVariant = surface,
        onSurfaceVariant = muted,
        outline = outline,
        outlineVariant = outline,
        error = bait,
        onError = background,
        errorContainer = surfaceHigh,
        onErrorContainer = bait,
        scrim = Color.Black
    )
} else {
    lightColorScheme(
        primary = onSurface,
        onPrimary = background,
        primaryContainer = surfaceHigh,
        onPrimaryContainer = onSurface,
        secondary = muted,
        onSecondary = background,
        secondaryContainer = surfaceHigh,
        onSecondaryContainer = onSurface,
        tertiary = onSurface,
        onTertiary = background,
        background = background,
        onBackground = onSurface,
        surface = background,
        onSurface = onSurface,
        surfaceVariant = surface,
        onSurfaceVariant = muted,
        outline = outline,
        outlineVariant = outline,
        error = bait,
        onError = background,
        errorContainer = surfaceHigh,
        onErrorContainer = bait,
        scrim = Color.Black
    )
}

private val TldrShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

@Composable
fun TldraiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val tldrPalette = if (darkTheme) DarkPalette else LightPalette

    // Dynamic colour is intentionally not used: the wallpaper-derived accent fought with the
    // verdict hues, which are the only colours here that carry meaning.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalPalette provides tldrPalette) {
        MaterialTheme(
            colorScheme = tldrPalette.toColorScheme(),
            typography = Typography,
            shapes = TldrShapes,
            content = content
        )
    }
}
