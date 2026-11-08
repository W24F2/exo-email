package com.example.exoemail.ui.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.exoemail.ui.components.GlassButton
import com.example.exoemail.ui.components.GlassSearchBar
import com.example.exoemail.ui.navigation.ComposeRoute
import com.example.exoemail.ui.navigation.DraftsRoute
import com.example.exoemail.ui.navigation.FolderRoute
import com.example.exoemail.ui.navigation.FoldersRoute
import com.example.exoemail.ui.navigation.InboxRoute
import com.example.exoemail.ui.navigation.SentRoute
import com.example.exoemail.ui.navigation.StarredRoute
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials

data class ShellTab(val route: Any, val label: String, val icon: ImageVector)

val ShellTabs = listOf(
    ShellTab(InboxRoute, "Inbox", Icons.Filled.Email),
    ShellTab(StarredRoute, "Starred", Icons.Filled.Star),
    ShellTab(SentRoute, "Sent", Icons.AutoMirrored.Filled.Send),
    ShellTab(DraftsRoute, "Drafts", Icons.Filled.Drafts),
    ShellTab(FoldersRoute, "Folders", Icons.Filled.Menu),
)

/** Extra bottom space content needs so the last row isn't hidden under the dock. */
private val DockContentPadding = 96.dp

/**
 * Shared app chrome: top bar (avatar + search) + floating liquid-glass dock.
 *
 * The content slot renders on a [hazeSource] backdrop and the dock is a
 * floating pill with a real-time [hazeEffect] blur â€” mail scrolling behind it
 * refracts through the glass. Realtime by construction: Haze re-samples the
 * source every frame the content changes.
 */
@Composable
fun LiquidShell(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    activeTab: Any,
    onTab: (Any) -> Unit,
    onAvatar: () -> Unit,
    accountsExist: Boolean,
    onAddAccount: () -> Unit,
    accountInitial: String,
    content: @Composable BoxScope.() -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val hazeState = remember { HazeState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Content extends edge-to-edge below the top bar; it is the haze source
        // the dock blurs. Top inset = status bar + top bar height (~64dp).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 64.dp)
                .hazeSource(hazeState),
        ) {
            content()
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { onAvatar() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    accountInitial.ifEmpty { "â€¢" },
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            GlassSearchBar(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Search in $title",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            )
        }

        // Floating liquid-glass dock
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            LiquidDock(
                hazeState = hazeState,
                activeTab = activeTab,
                onTab = onTab,
                onCompose = { onTab(ComposeRoute()) },
                showCompose = accountsExist,
                haptics = haptics,
            )
        }

        if (!accountsExist) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                GlassButton(onClick = onAddAccount, label = "Add Account", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun LiquidDock(
    hazeState: HazeState,
    activeTab: Any,
    onTab: (Any) -> Unit,
    onCompose: () -> Unit,
    showCompose: Boolean,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    val shape = RoundedCornerShape(50)
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val hazeTint = if (isDark) Color(0xFF14181F) else Color(0xFFF4F6FA)

    Row(
        modifier = Modifier
            .height(64.dp)
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .clip(shape)
            // Real-time backdrop blur: samples whatever is scrolling behind.
            .hazeEffect(
                state = hazeState,
                style = HazeMaterials.regular(hazeTint),
            )
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (isDark) 0.14f else 0.55f),
                        Color.White.copy(alpha = if (isDark) 0.08f else 0.35f),
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (isDark) 0.35f else 0.7f),
                        Color.White.copy(alpha = if (isDark) 0.10f else 0.25f),
                    )
                ),
                shape = shape,
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShellTabs.forEach { tab ->
            val isSelected = activeTab == tab.route ||
                (tab.route is FoldersRoute && activeTab is FolderRoute)
            val tint = if (isSelected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTab(tab.route)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(20.dp))
                Text(tab.label, color = tint, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (showCompose) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCompose()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Create,
                    contentDescription = "Compose",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
