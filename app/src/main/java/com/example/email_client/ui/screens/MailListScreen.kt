package com.example.email_client.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.email_client.LiquidMailApp
import com.example.email_client.ui.inbox.InboxViewModel
import com.example.email_client.ui.inbox.InboxViewModelFactory
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData
import com.example.email_client.data.db.MessageEntity

/**
 * Generic paged mail list used by Starred / Sent / Drafts / Folder screens.
 * Reuses [InboxRow] for visual consistency.
 */
@Composable
fun MailListScreen(
    app: LiquidMailApp,
    haze: HazeState,
    messages: Flow<PagingData<MessageEntity>>,
    onOpenMessage: (String) -> Unit,
    onComposeDraft: ((MessageEntity) -> Unit)? = null,
    emptyText: String = "Nothing here yet",
) {
    val vm: InboxViewModel = viewModel(factory = InboxViewModelFactory(app))
    val items = messages.collectAsLazyPagingItems()
    val refreshing by vm.refreshing.collectAsState()
    val syncError by vm.syncError.collectAsState()

    if (items.itemCount == 0 && !refreshing) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            com.example.email_client.ui.components.GlassCard(haze = haze, tintAlpha = 0.08f) {
                androidx.compose.material3.Text(
                    syncError ?: emptyText,
                    modifier = Modifier.padding(24.dp),
                    color = com.example.email_client.ui.theme.TextSecondary,
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = rememberLazyListState(),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
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
                onClick = {
                    if (m.isDraft && onComposeDraft != null) onComposeDraft(m)
                    else { vm.markRead(m.id); onOpenMessage(m.id) }
                },
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

// thin wrappers so each tab has its own composable name for NavDisplay clarity
@Composable
fun StarredScreen(app: LiquidMailApp, haze: HazeState, onOpenMessage: (String) -> Unit) {
    val vm: InboxViewModel = viewModel(factory = InboxViewModelFactory(app))
    val account by vm.resolvedAccount.collectAsState()
    if (account == null) return
    MailListScreen(app, haze, vm.starred(account!!.id), onOpenMessage, emptyText = "No starred messages")
}

@Composable
fun SentScreen(app: LiquidMailApp, haze: HazeState, onOpenMessage: (String) -> Unit) {
    val vm: InboxViewModel = viewModel(factory = InboxViewModelFactory(app))
    val account by vm.resolvedAccount.collectAsState()
    if (account == null) return
    MailListScreen(app, haze, vm.sent(account!!.id), onOpenMessage, emptyText = "No sent messages")
}

@Composable
fun DraftsScreen(app: LiquidMailApp, haze: HazeState, onOpenMessage: (String) -> Unit) {
    val vm: InboxViewModel = viewModel(factory = InboxViewModelFactory(app))
    val account by vm.resolvedAccount.collectAsState()
    if (account == null) return
    MailListScreen(app, haze, vm.drafts(account!!.id), onOpenMessage, emptyText = "No drafts")
}
