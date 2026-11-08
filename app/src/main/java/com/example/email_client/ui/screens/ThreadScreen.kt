package com.example.email_client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.email_client.LiquidMailApp
import com.example.email_client.ui.components.GlassBackButton
import com.example.email_client.ui.components.GlassButton
import com.example.email_client.ui.components.GlassCard
import com.example.email_client.ui.components.GlassTopBar
import com.example.email_client.ui.components.LiquidBackdrop
import com.example.email_client.ui.thread.ThreadViewModel
import com.example.email_client.ui.thread.ThreadViewModelFactory
import com.example.email_client.ui.theme.LocalTextSecondary
import com.example.email_client.ui.theme.LocalTextTertiary
import dev.chrisbanes.haze.HazeState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ThreadScreen(
    app: LiquidMailApp,
    messageId: String,
    onBack: () -> Unit,
    onReply: (String) -> Unit,
) {
    val vm: ThreadViewModel = viewModel(factory = ThreadViewModelFactory(app, messageId))
    val message by vm.message.collectAsState()
    val body by vm.body.collectAsState()
    val attachments by vm.attachments.collectAsState()
    val haze = remember { HazeState() }
    var showMenu by remember { mutableStateOf(false) }
    val textSecondary = LocalTextSecondary.current
    val textTertiary = LocalTextTertiary.current

    LiquidBackdrop(haze = haze) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(
                    title = message?.subject ?: "",
                    modifier = Modifier.statusBarsPadding(),
                    navigationIcon = { GlassBackButton(onBack) },
                    actions = {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onBackground)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text("Star") }, onClick = { vm.star(); showMenu = false })
                                DropdownMenuItem(text = { Text("Archive") }, onClick = { showMenu = false })
                                DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false })
                            }
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                message?.let { m ->
                    // Sender header with avatar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(com.example.email_client.ui.theme.LocalAccentGradient.current),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                (m.senderName.ifBlank { m.sender }).take(1).uppercase(),
                                color = com.example.email_client.ui.theme.LocalOnAccent.current,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(m.senderName.ifBlank { m.sender }, style = MaterialTheme.typography.titleMedium)
                            Text(
                                SimpleDateFormat("EEE, MMM d HH:mm", Locale.getDefault()).format(Date(m.date)),
                                color = textSecondary, style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    // Body
                    GlassCard(haze = haze, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            body?.text ?: m.snippet,
                            modifier = Modifier.padding(18.dp),
                        )
                    }

                    // Attachments
                    if (attachments.isNotEmpty()) {
                        Text("Attachments", style = MaterialTheme.typography.titleSmall, color = textSecondary)
                        attachments.forEach { att ->
                            GlassCard(haze = haze, tintAlpha = 0.06f, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(Icons.Filled.AttachFile, contentDescription = null, tint = textTertiary, modifier = Modifier.size(20.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(att.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                        Text("${att.mimeType} · ${att.size}B", color = textSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    // Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassButton(onClick = { onReply(m.sender) }, label = "Reply")
                        GlassButton(onClick = { vm.star() }, label = if (m.isStarred) "Unstar" else "Star")
                    }
                }
            }
        }
    }
}
