package com.example.email_client.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

private val LiquidLight = lightColorScheme(
    primary = Color(0xFF0891B2),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFDB2777),
    onTertiary = Color(0xFFFFFFFF),
    background = LightBase,
    onBackground = LightTextPrimary,
    surface = LightElevated,
    onSurface = LightTextPrimary,
    surfaceVariant = LightElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightTextSecondary,
    error = Color(0xFFDC2626),
)

/** Primary iridescent accent gradient (cyan -> violet -> pink). */
val LocalAccentGradient = staticCompositionLocalOf {
    Brush.linearGradient(listOf(LiquidCyan, LiquidViolet, LiquidPink))
}

/** Cooler accent gradient used for secondary glass / blobs. */
val LocalCoolGradient = staticCompositionLocalOf {
    Brush.linearGradient(listOf(LiquidTeal, LiquidBlue, LiquidViolet))
}

/** Text colors that adapt to theme. */
val LocalTextPrimary = staticCompositionLocalOf { TextPrimary }
val LocalTextSecondary = staticCompositionLocalOf { TextSecondary }
val LocalTextTertiary = staticCompositionLocalOf { TextTertiary }
val LocalOnAccent = staticCompositionLocalOf { OnAccent }

/** Whether the current theme is dark - for glass/backdrop color decisions. */
val LocalIsDark = staticCompositionLocalOf { true }

@Composable
fun LiquidMailTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val textPrimary = if (dark) TextPrimary else LightTextPrimary
    val textSecondary = if (dark) TextSecondary else LightTextSecondary
    val textTertiary = if (dark) TextTertiary else LightTextTertiary
    val onAccent = if (dark) OnAccent else LightOnAccent

    CompositionLocalProvider(
        LocalAccentGradient provides Brush.linearGradient(
            if (dark) listOf(LiquidCyan, LiquidViolet, LiquidPink)
            else listOf(Color(0xFF0891B2), Color(0xFF7C3AED), Color(0xFFDB2777))
        ),
        LocalCoolGradient provides Brush.linearGradient(listOf(LiquidTeal, LiquidBlue, LiquidViolet)),
        LocalTextPrimary provides textPrimary,
        LocalTextSecondary provides textSecondary,
        LocalTextTertiary provides textTertiary,
        LocalOnAccent provides onAccent,
        LocalIsDark provides dark,
    ) {
        MaterialTheme(
            colorScheme = if (dark) LiquidDark else LiquidLight,
            typography = LiquidTypography,
            content = content,
        )
    }
}
