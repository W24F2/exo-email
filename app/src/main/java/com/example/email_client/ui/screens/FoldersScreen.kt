package com.example.email_client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
) {
    val vm: InboxViewModel = viewModel(factory = InboxViewModelFactory(app))
    val account by vm.activeAccount.collectAsState()
    val folders = account?.let { vm.folders(it.id).collectAsState(initial = emptyList()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(folders?.value ?: emptyList(), key = { it.name }) { f ->
            FolderRow(folder = f, onClick = { onOpenFolder(f.name) }, haze = haze)
        }
        if (folders?.value.isNullOrEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Text("Pull to sync folders", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun FolderRow(folder: FolderEntity, onClick: () -> Unit, haze: HazeState) {
    GlassCard(
        haze = haze,
        tintAlpha = 0.08f,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                folder.displayName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (folder.unreadCount > 0) {
                Text(
                    folder.unreadCount.toString(),
                    color = LiquidCyan,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
