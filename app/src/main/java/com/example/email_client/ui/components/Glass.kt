package com.example.email_client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.email_client.ui.theme.GlassFill
import com.example.email_client.ui.theme.GlassStroke
import com.example.email_client.ui.theme.LocalAccentGradient
import com.example.email_client.ui.theme.OnAccent
import com.example.email_client.ui.theme.TextSecondary
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/** Corner-radius scale for the whole app: 28dp surfaces, 24dp fields, pill buttons. */
object GlassShape {
    val Surface = RoundedCornerShape(28.dp)
    val Field = RoundedCornerShape(20.dp)
    val Chip = RoundedCornerShape(16.dp)
    val Pill = RoundedCornerShape(50)
}

/**
 * Liquid-glass fill: vertical white gradient + 1px gradient border + top specular.
 * Apply on top of a [hazeEffect] surface for real backdrop blur.
 */
fun Modifier.glass(
    shape: Shape = GlassShape.Surface,
    tintAlpha: Float = 0.10f,
): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                GlassFill.copy(alpha = tintAlpha + 0.09f),
                GlassFill.copy(alpha = tintAlpha * 0.55f),
            )
        )
    )
    .border(
        1.dp,
        Brush.verticalGradient(
            listOf(
                GlassStroke.copy(alpha = 0.55f),
                GlassStroke.copy(alpha = 0.10f),
            )
        ),
        shape,
    )

/** Frosted surface that blurs whatever the [HazeState] source drew behind it. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.liquidGlass(
    haze: HazeState?,
    shape: Shape = GlassShape.Surface,
    tintAlpha: Float = 0.10f,
): Modifier = if (haze == null) {
    this.glass(shape, tintAlpha)
} else {
    this
        .clip(shape)
        .hazeEffect(
            state = haze,
            style = HazeMaterials.ultraThin(containerColor = MaterialTheme.colorScheme.surface),
        )
        .glass(shape, tintAlpha)
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    haze: HazeState? = null,
    shape: Shape = GlassShape.Surface,
    tintAlpha: Float = 0.10f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.liquidGlass(haze, shape, tintAlpha), content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            androidx.compose.foundation.layout.Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
            }
        },
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
        shape = GlassShape.Pill,
        containerColor = Color.Transparent,
        contentColor = OnAccent,
        elevation = FloatingActionButtonDefaults.elevation(0.dp),
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
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String,
) {
    val shape = GlassShape.Pill
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (enabled) LocalAccentGradient.current
                else Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x22FFFFFF)))
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (enabled) OnAccent else TextSecondary, style = MaterialTheme.typography.labelLarge)
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
        shape = GlassShape.Field,
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(GlassStroke.copy(alpha = 0.35f), GlassStroke.copy(alpha = 0.10f))
                ),
                GlassShape.Field,
            ),
    )
}
