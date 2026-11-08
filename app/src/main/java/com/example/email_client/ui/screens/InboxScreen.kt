package com.example.email_client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.email_client.LiquidMailApp
import com.example.email_client.data.db.MessageEntity
import com.example.email_client.ui.components.GlassCard
import com.example.email_client.ui.inbox.InboxViewModel
import com.example.email_client.ui.inbox.InboxViewModelFactory
import com.example.email_client.ui.theme.LiquidPink
import com.example.email_client.ui.theme.LocalAccentGradient
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
    haze: HazeState,
    query: String,
    accountsExist: Boolean,
    onOpenMessage: (String) -> Unit,
) {
    val vm: InboxViewModel = viewModel(factory = InboxViewModelFactory(app))
    // vm.messages() returns empty paging when no resolved account exists; accountsExist
    // flag only drives the empty-state label so we surface a friendly CTA.
    val items = vm.messages(query).collectAsLazyPagingItems()
    val refreshing by vm.refreshing.collectAsState()
    val syncError by vm.syncError.collectAsState()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { vm.refresh() },
        modifier = Modifier.fillMaxSize(),
    ) {
        if (items.itemCount == 0 && refreshing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Loading your mail", color = TextSecondary)
                }
            }
        }
        if (items.itemCount == 0 && !refreshing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassCard(haze = haze) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            when {
                                !accountsExist -> "Add an account to get started"
                                syncError != null -> "Couldn't load mail"
                                query.isBlank() -> "You're all caught up"
                                else -> "No matching messages"
                            },
                            color = TextSecondary,
                        )
                        if (syncError != null) {
                            Text(
                                syncError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            TextButton(onClick = { vm.refresh() }) { Text("Try again") }
                        } else if (accountsExist && items.itemCount == 0 && query.isBlank()) {
                            TextButton(onClick = { vm.refresh() }) { Text("Sync now") }
                        }
                    }
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            item { GlassCard(haze = haze, tintAlpha = 0.07f) {} }
            items(
                count = items.itemCount,
                key = items.itemKey { it.id },
            ) { i ->
                val m = items[i] ?: return@items
                InboxRow(
                    sender = m.senderName.ifBlank { m.sender },
                    subject = m.subject,
                    snippet = m.snippet,
                    date = m.date,
                    read = m.isRead,
                    starred = m.isStarred,
                    onClick = { vm.markRead(m.id); onOpenMessage(m.id) },
                    onStar = { vm.toggleStar(m.id, !m.isStarred) },
                )
                if (i < items.itemCount - 1) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

private fun emptyPaging(): PagingData<MessageEntity> = PagingData.empty()

@Composable
internal fun InboxRow(
    sender: String,
    subject: String,
    snippet: String,
    date: Long,
    read: Boolean,
    starred: Boolean,
    onClick: () -> Unit,
    onStar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (!read) LiquidPink else Color.Transparent),
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(LocalAccentGradient.current)
                .padding(1.5.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.07f))
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(sender.take(1).uppercase(), color = OnAccent, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    sender,
                    fontWeight = if (!read) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    formatDate(date),
                    color = if (!read) MaterialTheme.colorScheme.primary else TextTertiary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                subject.ifBlank { snippet },
                fontWeight = if (!read) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (subject.isNotBlank() && snippet.isNotBlank()) {
                Text(
                    snippet, color = TextSecondary, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        IconButton(onClick = onStar, modifier = Modifier.size(32.dp)) {
            Icon(
                if (starred) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "Star",
                tint = if (starred) LiquidPink else TextTertiary,
            )
        }
    }
}

private fun formatDate(ts: Long): String {
    val now = System.currentTimeMillis()
    val fmt = if (now - ts < 24 * 60 * 60 * 1000) SimpleDateFormat("HH:mm", Locale.getDefault())
    else SimpleDateFormat("MMM d", Locale.getDefault())
    return fmt.format(Date(ts))
}
