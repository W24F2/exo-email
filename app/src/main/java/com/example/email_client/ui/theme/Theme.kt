package com.example.email_client.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush

private val LiquidDark = darkColorScheme(
    primary = LiquidCyan,
    onPrimary = OnAccent,
    secondary = LiquidViolet,
    onSecondary = OnAccent,
    tertiary = LiquidPink,
    background = SpaceBase,
    onBackground = TextPrimary,
    surface = SpaceElevated,
    onSurface = TextPrimary,
    surfaceVariant = SpaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = TextSecondary,
)

val LocalAccentGradient = staticCompositionLocalOf {
    Brush.linearGradient(listOf(LiquidCyan, LiquidViolet, LiquidPink))
}

@Composable
fun LiquidMailTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LiquidDark,
        typography = LiquidTypography,
        content = content,
    )
}
