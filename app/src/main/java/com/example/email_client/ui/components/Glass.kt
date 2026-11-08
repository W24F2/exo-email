package com.example.email_client.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.email_client.ui.theme.GlassFill
import com.example.email_client.ui.theme.GlassStroke
import com.example.email_client.ui.theme.LocalAccentGradient
import com.example.email_client.ui.theme.OnAccent
import com.example.email_client.ui.theme.TextSecondary

fun Modifier.glass(
    shape: Shape = RoundedCornerShape(28.dp),
    tintAlpha: Float = 0.12f,
): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                GlassFill.copy(alpha = tintAlpha + 0.10f),
                GlassFill.copy(alpha = tintAlpha),
            )
        )
    )
    .border(
        1.dp,
        Brush.verticalGradient(
            listOf(
                GlassStroke.copy(alpha = 0.45f),
                GlassStroke.copy(alpha = 0.10f),
            )
        ),
        shape,
    )

/** Blur layer for content behind a frosted surface. minSdk 37 => RenderEffect always available. */
fun Modifier.glassBlur(radius: Dp = 28.dp): Modifier = this.graphicsLayer {
    renderEffect = RenderEffect.createBlurEffect(
        radius.toPx(), radius.toPx(), Shader.TileMode.DECAL
    ).asComposeRenderEffect()
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tintAlpha: Float = 0.12f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.glass(shape = shape, tintAlpha = tintAlpha), content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        modifier = modifier,
    )
}

@Composable
fun GlassFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {
        Icon(Icons.Filled.Edit, contentDescription = "Compose", tint = OnAccent)
    },
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = OnAccent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalAccentGradient.current),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

@Composable
fun GlassSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search mail",
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextSecondary) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = GlassFill.copy(alpha = 0.10f),
            unfocusedContainerColor = GlassFill.copy(alpha = 0.07f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}
