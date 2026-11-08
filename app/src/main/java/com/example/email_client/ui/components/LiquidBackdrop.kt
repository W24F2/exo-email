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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.email_client.ui.theme.LiquidCyan
import com.example.email_client.ui.theme.LiquidPink
import com.example.email_client.ui.theme.LiquidViolet
import com.example.email_client.ui.theme.SpaceBase

/**
 * Animated "liquid" backdrop: two slow-drifting radial gradient blobs over the
 * space base colour. Cheap to draw — pure gradients, no particles/blit loops.
 */
@Composable
fun LiquidBackdrop(content: @Composable BoxScope.() -> Unit) {
    val transition = rememberInfiniteTransition(label = "liquid")
    val driftX by transition.animateFloat(
        initialValue = -60f, targetValue = 60f,
        animationSpec = infiniteRepeatable(tween(14000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "driftX",
    )
    val driftY by transition.animateFloat(
        initialValue = -40f, targetValue = 40f,
        animationSpec = infiniteRepeatable(tween(19000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "driftY",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBase)
    ) {
        Box(
            modifier = Modifier
                .size(420.dp)
                .offset(x = (40 + driftX).dp, y = (-80 + driftY).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(LiquidCyan.copy(alpha = 0.16f), LiquidCyan.copy(alpha = 0f))
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(480.dp)
                .offset(x = (160 - driftX).dp, y = (420 + driftY).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(LiquidViolet.copy(alpha = 0.15f), LiquidPink.copy(alpha = 0f))
                    )
                )
        )
        content()
    }
}
