package com.example.email_client.ui.shell

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
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.email_client.ui.components.GlassButton
import com.example.email_client.ui.components.GlassSearchBar
import com.example.email_client.ui.navigation.ComposeRoute
import com.example.email_client.ui.navigation.DraftsRoute
import com.example.email_client.ui.navigation.FolderRoute
import com.example.email_client.ui.navigation.FoldersRoute
import com.example.email_client.ui.navigation.InboxRoute
import com.example.email_client.ui.navigation.SentRoute
import com.example.email_client.ui.navigation.StarredRoute

data class ShellTab(val route: Any, val label: String, val icon: ImageVector)

val ShellTabs = listOf(
    ShellTab(InboxRoute, "Inbox", Icons.Filled.Email),
    ShellTab(StarredRoute, "Starred", Icons.Filled.Star),
    ShellTab(SentRoute, "Sent", Icons.AutoMirrored.Filled.Send),
    ShellTab(DraftsRoute, "Drafts", Icons.Filled.Drafts),
    ShellTab(FoldersRoute, "Folders", Icons.Filled.Menu),
)

/**
 * Shared app chrome: top bar (avatar + search), content slot,
 * M3 NavigationBar bottom bar, floating compose FAB.
 * No blur/backdrop — plain themed surfaces (perf-first).
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

    Scaffold(
        topBar = {
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
                        accountInitial.ifEmpty { "•" },
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
        },
        bottomBar = {
            NavigationBar {
                ShellTabs.forEach { tab ->
                    val isSelected = activeTab == tab.route ||
                        (tab.route is FoldersRoute && activeTab is FolderRoute)
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTab(tab.route)
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            if (accountsExist) {
                FloatingActionButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTab(ComposeRoute())
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Filled.Create, contentDescription = "Compose")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            content()
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
}
