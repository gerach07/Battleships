package com.anasio.battleships.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun BattleshipTheme(content: @Composable () -> Unit) {
    val p = LocalColorPalette.current
    val scheme = darkColorScheme(
        primary = p.primary,
        onPrimary = androidx.compose.ui.graphics.Color.White,
        secondary = p.accent,
        tertiary = p.yellow,
        background = p.background,
        surface = p.surface,
        surfaceVariant = p.card,
        onBackground = androidx.compose.ui.graphics.Color.White,
        onSurface = androidx.compose.ui.graphics.Color.White,
        onSurfaceVariant = p.textPrimary,
        error = p.red,
        onError = androidx.compose.ui.graphics.Color.White,
        outline = p.border,
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = BattleshipTypography,
        content = content,
    )
}
