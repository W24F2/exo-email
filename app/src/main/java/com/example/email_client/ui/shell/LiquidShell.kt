package com.example.email_client.ui.shell

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.email_client.ui.components.GlassCard
import com.example.email_client.ui.components.GlassSearchBar
import com.example.email_client.ui.components.GlassShape
import com.example.email_client.ui.components.LiquidBackdrop
import com.example.email_client.ui.theme.LocalCoolGradient
import com.example.email_client.ui.theme.OnAccent
import com.example.email_client.ui.theme.TextSecondary
import com.example.email_client.ui.theme.TextTertiary
import dev.chrisbanes.haze.HazeState

data class ShellTab(val route: Any, val label: String, val icon: ImageVector)

val ShellTabs = listOf(
    ShellTab(com.example.email_client.ui.navigation.InboxRoute, "Mail", Icons.Filled.Email),
    ShellTab(com.example.email_client.ui.navigation.FoldersRoute, "Folders", Icons.Filled.Menu),
    ShellTab(com.example.email_client.ui.navigation.ComposeRoute(), "Write", Icons.Filled.Create),
    ShellTab(com.example.email_client.ui.navigation.AccountsRoute, "Accounts", Icons.Filled.Person),
)

/**
 * Shared app chrome: liquid backdrop + frosted top bar (avatar + search) +
 * floating bottom nav pill. Content slots between them.
 */
@Composable
fun LiquidShell(
    haze: HazeState,
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    activeTab: Any,
    onTab: (Any) -> Unit,
    onAvatar: () -> Unit,
    accountInitial: String,
    content: @Composable BoxScope.() -> Unit,
) {
    LiquidBackdrop(haze = haze) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(LocalCoolGradient.current),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onAvatar) {
                    Text(accountInitial, color = OnAccent)
                }
            }
            GlassSearchBar(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Search in $title",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            )
        }

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 76.dp, bottom = 96.dp),
            content = content,
        )

        // Floating liquid bottom-nav pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            GlassCard(
                haze = haze,
                shape = GlassShape.Pill,
                tintAlpha = 0.14f,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShellTabs.forEach { tab ->
                        val selected = activeTab == tab.route ||
                            (tab.route is com.example.email_client.ui.navigation.FoldersRoute &&
                                activeTab is com.example.email_client.ui.navigation.FolderRoute)
                        Box(
                            modifier = Modifier
                                .clip(if (selected) GlassShape.Pill else CircleShape)
                                .background(
                                    if (selected)
                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            listOf(
                                                com.example.email_client.ui.theme.LiquidCyan.copy(alpha = 0.28f),
                                                com.example.email_client.ui.theme.LiquidViolet.copy(alpha = 0.28f),
                                            )
                                        )
                                    else androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(
                                            androidx.compose.ui.graphics.Color.Transparent,
                                            androidx.compose.ui.graphics.Color.Transparent,
                                        )
                                    )
                                )
                                .clickableTab { onTab(tab.route) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.foundation.layout.Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else TextTertiary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    tab.label,
                                    color = if (selected) MaterialTheme.colorScheme.primary else TextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.clickableTab(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
