package com.example.email_client.ui.shell

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.email_client.ui.components.GlassButton
import com.example.email_client.ui.components.GlassCard
import com.example.email_client.ui.components.GlassSearchBar
import com.example.email_client.ui.components.GlassShape
import com.example.email_client.ui.components.LiquidBackdrop
import com.example.email_client.ui.theme.LocalAccentGradient
import com.example.email_client.ui.theme.LiquidCyan
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
 *
 * When [accountsExist] is false, shows a prominent "Add Account" CTA inside the content area.
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
    accountsExist: Boolean,
    onAddAccount: () -> Unit,
    accountInitial: String,
    content: @Composable BoxScope.() -> Unit,
) {
    LiquidBackdrop(haze = haze) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(LocalAccentGradient.current)
                    .clickable { onAvatar() },
                contentAlignment = Alignment.Center,
            ) {
                Text(accountInitial.ifEmpty { "•" }, color = OnAccent)
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

        // Content — wrap with optional Add Account CTA overlay when no accounts
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, bottom = 96.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize(), content = content)
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
                        val isSelected = activeTab == tab.route ||
                            (tab.route is com.example.email_client.ui.navigation.FoldersRoute &&
                                activeTab is com.example.email_client.ui.navigation.FolderRoute)
                        Box(
                            modifier = Modifier
                                .clip(if (isSelected) GlassShape.Pill else CircleShape)
                                .background(
                                    if (isSelected) Brush.horizontalGradient(
                                        listOf(LiquidCyan.copy(alpha = 0.28f), com.example.email_client.ui.theme.LiquidViolet.copy(alpha = 0.28f)))
                                    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .clickable { onTab(tab.route) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    tab.icon, contentDescription = tab.label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else TextTertiary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    tab.label,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary,
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
