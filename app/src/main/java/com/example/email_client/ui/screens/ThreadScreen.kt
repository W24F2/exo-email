package com.example.email_client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.email_client.LiquidMailApp
import com.example.email_client.ui.components.GlassButton
import com.example.email_client.ui.components.GlassCard
import com.example.email_client.ui.components.GlassTopBar
import com.example.email_client.ui.components.LiquidBackdrop
import com.example.email_client.ui.thread.ThreadViewModel
import com.example.email_client.ui.thread.ThreadViewModelFactory
import com.example.email_client.ui.theme.TextSecondary
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
    val haze = remember { HazeState() }

    LiquidBackdrop(haze = haze) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(
                    title = message?.subject ?: "",
                    modifier = Modifier.statusBarsPadding(),
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
                    Text(m.senderName.ifBlank { m.sender }, style = MaterialTheme.typography.titleMedium)
                    Text(
                        SimpleDateFormat("EEE, MMM d HH:mm", Locale.getDefault()).format(Date(m.date)),
                        color = TextSecondary, style = MaterialTheme.typography.bodySmall,
                    )
                    GlassCard(haze = haze, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            body?.text ?: m.snippet,
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassButton(onClick = { onReply(m.sender) }, label = "Reply")
                        GlassButton(onClick = { vm.star() }, label = if (m.isStarred) "Unstar" else "Star")
                    }
                }
            }
        }
    }
}
