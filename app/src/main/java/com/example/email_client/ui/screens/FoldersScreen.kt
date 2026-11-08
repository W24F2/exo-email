package com.example.email_client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.email_client.LiquidMailApp
import com.example.email_client.data.db.FolderEntity
import com.example.email_client.ui.components.GlassCard
import com.example.email_client.ui.inbox.InboxViewModel
import com.example.email_client.ui.inbox.InboxViewModelFactory
import com.example.email_client.ui.theme.LiquidCyan
import com.example.email_client.ui.theme.TextSecondary
import dev.chrisbanes.haze.HazeState

@Composable
fun FoldersScreen(
    app: LiquidMailApp,
    haze: HazeState,
    onOpenFolder: (String) -> Unit,
    accountsExist: Boolean = true,
) {
    val vm: InboxViewModel = viewModel(factory = InboxViewModelFactory(app))
    val account by vm.resolvedAccount.collectAsState()

    if (!accountsExist || account == null) {
        EmptyHint(haze = haze, text = if (!accountsExist) "Add an account to see folders" else "No account configured")
        return
    }
    val acc = account!!
    val folders by vm.folders(acc.id).collectAsState(emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (folders.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    Text("Pull to sync folders", color = TextSecondary)
                }
            }
        }
        items(folders, key = { it.name }) { f ->
            FolderRow(folder = f, onClick = { onOpenFolder(f.name) }, haze = haze)
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun EmptyHint(haze: HazeState, text: String) {
    GlassCard(
        haze = haze, tintAlpha = 0.08f,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
    ) {
        Text(text, modifier = Modifier.padding(20.dp), color = TextSecondary)
    }
}

@Composable
private fun FolderRow(folder: FolderEntity, onClick: () -> Unit, haze: HazeState) {
    GlassCard(
        haze = haze, tintAlpha = 0.08f,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(folder.displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (folder.unreadCount > 0) {
                Text(folder.unreadCount.toString(), color = LiquidCyan, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
