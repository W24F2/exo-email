package com.example.email_client.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val LiquidDark = darkColorScheme(
    primary = LiquidCyan,
    onPrimary = OnAccent,
    secondary = LiquidViolet,
    onSecondary = OnAccent,
    tertiary = LiquidPink,
    onTertiary = OnAccent,
    background = SpaceBase,
    onBackground = TextPrimary,
    surface = SpaceElevated,
    onSurface = TextPrimary,
    surfaceVariant = SpaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = TextSecondary,
    error = Color(0xFFFF8A80),
)

/** Primary iridescent accent gradient (cyan -> violet -> pink). */
val LocalAccentGradient = staticCompositionLocalOf {
    Brush.linearGradient(listOf(LiquidCyan, LiquidViolet, LiquidPink))
}

/** Cooler accent gradient used for secondary glass / blobs. */
val LocalCoolGradient = staticCompositionLocalOf {
    Brush.linearGradient(listOf(LiquidTeal, LiquidBlue, LiquidViolet))
}

@Composable
fun LiquidMailTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAccentGradient provides Brush.linearGradient(listOf(LiquidCyan, LiquidViolet, LiquidPink)),
        LocalCoolGradient provides Brush.linearGradient(listOf(LiquidTeal, LiquidBlue, LiquidViolet)),
    ) {
        MaterialTheme(
            colorScheme = LiquidDark,
            typography = LiquidTypography,
            content = content,
        )
    }
}
