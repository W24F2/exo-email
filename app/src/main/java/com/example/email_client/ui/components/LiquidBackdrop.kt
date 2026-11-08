package com.example.email_client.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.email_client.ui.theme.LiquidBlue
import com.example.email_client.ui.theme.LiquidCyan
import com.example.email_client.ui.theme.LiquidPink
import com.example.email_client.ui.theme.LiquidViolet
import com.example.email_client.ui.theme.SpaceBase
import com.example.email_client.ui.theme.SpaceDeep
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Animated "liquid" backdrop: drifting blurred colour blobs over a vertical
 * space gradient. Also the [hazeSource] that glass surfaces blur against.
 */
@Composable
fun LiquidBackdrop(
    haze: HazeState? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = haze ?: remember { HazeState() }
    val transition = rememberInfiniteTransition(label = "liquid")
    val driftX by transition.animateFloat(
        initialValue = -70f, targetValue = 70f,
        animationSpec = infiniteRepeatable(tween(16000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "driftX",
    )
    val driftY by transition.animateFloat(
        initialValue = -50f, targetValue = 50f,
        animationSpec = infiniteRepeatable(tween(21000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "driftY",
    )
    val driftX2 by transition.animateFloat(
        initialValue = 60f, targetValue = -60f,
        animationSpec = infiniteRepeatable(tween(13000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "driftX2",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SpaceBase, SpaceDeep)
                )
            )
    ) {
        // Haze source: blobs + gradient are what glass blurs
        Box(Modifier.fillMaxSize().hazeSource(state)) {
            Blob(480.dp, (30 + driftX).dp, (-100 + driftY).dp, LiquidCyan, 0.20f)
            Blob(540.dp, (170 + driftX2).dp, (430 + driftY).dp, LiquidViolet, 0.16f)
            Blob(380.dp, (-60 - driftX).dp, (600 - driftY).dp, LiquidPink, 0.12f)
            Blob(340.dp, (220 - driftX2).dp, (120 - driftY).dp, LiquidBlue, 0.10f)
        }
        content()
    }
}

@Composable
private fun Blob(
    size: androidx.compose.ui.unit.Dp,
    x: androidx.compose.ui.unit.Dp,
    y: androidx.compose.ui.unit.Dp,
    color: androidx.compose.ui.graphics.Color,
    alpha: Float,
) {
    Box(
        modifier = Modifier
            .size(size)
            .offset(x = x, y = y)
            .clip(CircleShape)
            .blur(60.dp)
            .background(
                Brush.radialGradient(
                    listOf(color.copy(alpha = alpha), color.copy(alpha = 0f))
                )
            )
    )
}
