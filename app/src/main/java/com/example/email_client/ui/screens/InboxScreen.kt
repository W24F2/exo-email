package com.example.email_client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.email_client.LiquidMailApp
import com.example.email_client.ui.components.GlassCard
import com.example.email_client.ui.components.GlassFab
import com.example.email_client.ui.components.GlassTopBar
import com.example.email_client.ui.components.LiquidBackdrop
import com.example.email_client.ui.inbox.InboxViewModel
import com.example.email_client.ui.inbox.InboxViewModelFactory
import com.example.email_client.ui.theme.GlassStroke
import com.example.email_client.ui.theme.LiquidPink
import com.example.email_client.ui.theme.LocalCoolGradient
import com.example.email_client.ui.theme.OnAccent
import com.example.email_client.ui.theme.TextSecondary
import com.example.email_client.ui.theme.TextTertiary
import dev.chrisbanes.haze.HazeState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    app: LiquidMailApp,
    onOpenMessage: (String) -> Unit,
    onCompose: () -> Unit,
    onAccounts: () -> Unit,
) {
    val vm: InboxViewModel = viewModel(factory = InboxViewModelFactory(app))
    val items = vm.messages.collectAsLazyPagingItems()
    val refreshing by vm.refreshing.collectAsState()
    val account by vm.activeAccount.collectAsState()
    val haze = remember { HazeState() }

    LiquidBackdrop(haze = haze) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(
                    title = "Inbox",
                    subtitle = account?.email,
                    actions = {
                        IconButton(onClick = onAccounts) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = "Accounts")
                        }
                    },
                    modifier = Modifier.statusBarsPadding(),
                )
            },
            floatingActionButton = { GlassFab(onClick = onCompose) },
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { vm.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (items.itemCount == 0 && !refreshing) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GlassCard(haze = haze) {
                            Text(
                                "All caught up",
                                modifier = Modifier.padding(28.dp),
                                color = TextSecondary,
                            )
                        }
                    }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 4.dp, bottom = 96.dp,
                    ),
                ) {
                    items(
                        count = items.itemCount,
                        key = items.itemKey { it.id },
                    ) { i ->
                        val m = items[i] ?: return@items
                        GlassCard(
                            haze = haze,
                            tintAlpha = if (m.isRead) 0.06f else 0.14f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable {
                                    vm.markRead(m.id)
                                    onOpenMessage(m.id)
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // gradient-ringed specular avatar
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(LocalCoolGradient.current)
                                        .padding(2.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = 0.35f),
                                                        Color.White.copy(alpha = 0.08f),
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            m.senderName.take(1).uppercase(),
                                            color = OnAccent,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            m.senderName.ifBlank { m.sender },
                                            fontWeight = if (m.isRead) FontWeight.Normal else FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(formatDate(m.date), color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        m.subject,
                                        fontWeight = if (m.isRead) FontWeight.Normal else FontWeight.Medium,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        m.snippet,
                                        color = TextSecondary, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                IconButton(onClick = { vm.toggleStar(m.id, !m.isStarred) }) {
                                    Icon(
                                        if (m.isStarred) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = "Star",
                                        tint = if (m.isStarred) LiquidPink else TextTertiary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(ts: Long): String {
    val now = System.currentTimeMillis()
    val fmt = if (now - ts < 24 * 60 * 60 * 1000) SimpleDateFormat("HH:mm", Locale.getDefault())
    else SimpleDateFormat("MMM d", Locale.getDefault())
    return fmt.format(Date(ts))
}
