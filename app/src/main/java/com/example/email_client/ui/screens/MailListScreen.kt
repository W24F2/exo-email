package com.example.email_client.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.email_client.LiquidMailApp
import com.example.email_client.data.db.MessageEntity
import com.example.email_client.ui.inbox.InboxViewModel
import com.example.email_client.ui.inbox.InboxViewModelFactory
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData

/**
 * Generic paged mail list used by Starred / Sent / Drafts / Folder screens.
 * Reuses [InboxRow] for visual consistency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailListScreen(
    app: LiquidMailApp,
    messages: Flow<PagingData<MessageEntity>>,
    onOpenMessage: (String) -> Unit,
    onOpenDraft: ((String) -> Unit)? = null,
    emptyText: String = "Nothing here yet",
    onRefresh: (() -> Unit)? = null,
    showStar: Boolean = true,
    title: String? = null,
) {
    val vm: InboxViewModel = viewModel(factory = InboxViewModelFactory(app))
    val items = messages.collectAsLazyPagingItems()
    val refreshing by vm.refreshing.collectAsState()
    val syncError by vm.syncError.collectAsState()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { onRefresh?.invoke() ?: vm.refresh() },
        modifier = Modifier.fillMaxSize(),
    ) {
        if (items.itemCount == 0 && !refreshing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        syncError ?: emptyText,
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            return@PullToRefreshBox
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = rememberLazyListState(),
            contentPadding = PaddingValues(bottom = 104.dp),
        ) {
            if (title != null) {
                item {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    )
                }
            }
            items(
                count = items.itemCount,
                key = items.itemKey { it.id },
            ) { i ->
                val m = items[i] ?: return@items
                InboxRow(
                    sender = if (m.isDraft) "Draft" else m.senderName.ifBlank { m.sender },
                    subject = m.subject,
                    snippet = m.snippet,
                    date = m.date,
                    read = m.isRead,
                    starred = m.isStarred,
                    hasAttachments = m.hasAttachments,
                    showStar = showStar && !m.isDraft,
                    onClick = {
                        if (m.isDraft && onOpenDraft != null) onOpenDraft(m.id)
                        else { vm.markRead(m.id); onOpenMessage(m.id) }
                    },
                    onStar = { vm.toggleStar(m.id, !m.isStarred) },
                )
                if (i < items.itemCount - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

// thin wrappers so each tab has its own composable name for NavDisplay clarity
@Composable
fun StarredScreen(app: LiquidMailApp, onOpenMessage: (String) -> Unit) {
    val vm: InboxViewModel = viewModel(factory = InboxViewModelFactory(app))
    val account by vm.resolvedAccount.collectAsState()
    if (account == null) return
    MailListScreen(app, vm.starred(account!!.id), onOpenMessage, emptyText = "No starred messages", title = "Starred")
}

@Composable
fun SentScreen(app: LiquidMailApp, onOpenMessage: (String) -> Unit) {
    val vm: InboxViewModel = viewModel(factory = InboxViewModelFactory(app))
    val account by vm.resolvedAccount.collectAsState()
    if (account == null) return
    MailListScreen(app, vm.sent(account!!.id), onOpenMessage, emptyText = "No sent messages", showStar = false, title = "Sent")
}

@Composable
fun DraftsScreen(app: LiquidMailApp, onOpenDraft: (String) -> Unit) {
    val vm: InboxViewModel = viewModel(factory = InboxViewModelFactory(app))
    val account by vm.resolvedAccount.collectAsState()
    if (account == null) return
    MailListScreen(app, vm.drafts(account!!.id), onOpenMessage = {}, onOpenDraft = onOpenDraft, emptyText = "No drafts", showStar = false, title = "Drafts")
}
