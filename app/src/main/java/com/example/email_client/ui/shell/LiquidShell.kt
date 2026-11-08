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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Drafts
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.email_client.ui.components.GlassButton
import com.example.email_client.ui.components.GlassCard
import com.example.email_client.ui.components.GlassSearchBar
import com.example.email_client.ui.components.GlassShape
import com.example.email_client.ui.components.LiquidBackdrop
import com.example.email_client.ui.theme.LocalAccentGradient
import com.example.email_client.ui.theme.LiquidCyan
import com.example.email_client.ui.theme.LocalOnAccent
import com.example.email_client.ui.theme.LocalTextSecondary
import com.example.email_client.ui.theme.LocalTextTertiary
import dev.chrisbanes.haze.HazeState

data class ShellTab(val route: Any, val label: String, val icon: ImageVector)

val ShellTabs = listOf(
    ShellTab(com.example.email_client.ui.navigation.InboxRoute, "Inbox", Icons.Filled.Email),
    ShellTab(com.example.email_client.ui.navigation.StarredRoute, "Starred", Icons.Filled.Star),
    ShellTab(com.example.email_client.ui.navigation.SentRoute, "Sent", Icons.Filled.Send),
    ShellTab(com.example.email_client.ui.navigation.DraftsRoute, "Drafts", Icons.Filled.Drafts),
    ShellTab(com.example.email_client.ui.navigation.FoldersRoute, "Folders", Icons.Filled.Menu),
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
    accountsExist: Boolean,
    onAddAccount: () -> Unit,
    accountInitial: String,
    content: @Composable BoxScope.() -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val textSecondary = LocalTextSecondary.current
    val textTertiary = LocalTextTertiary.current
    val onAccent = LocalOnAccent.current
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
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAvatar()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(accountInitial.ifEmpty { "•" }, color = onAccent)
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

        // Content
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

        // Floating compose button (above bottom nav, right side)
        if (accountsExist) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 24.dp, bottom = 96.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(LocalAccentGradient.current)
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTab(com.example.email_client.ui.navigation.ComposeRoute())
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Create,
                        contentDescription = "Compose",
                        tint = onAccent,
                        modifier = Modifier.size(24.dp),
                    )
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
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTab(tab.route)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    tab.icon, contentDescription = tab.label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else textTertiary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    tab.label,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else textSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
