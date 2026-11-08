package com.example.email_client.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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

/** Folder message list - same dense rows as inbox, different paging source. */
@Composable
fun FolderScreen(
    app: LiquidMailApp,
    haze: HazeState,
    folder: String,
    onOpenMessage: (String) -> Unit,
) {
    val vm: InboxViewModel = viewModel(factory = InboxViewModelFactory(app))
    val account by vm.resolvedAccount.collectAsState()
    if (account == null) return
    val items = vm.folderMessages(account!!.id, folder).collectAsLazyPagingItems()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
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
