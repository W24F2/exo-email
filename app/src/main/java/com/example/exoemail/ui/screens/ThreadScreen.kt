package com.example.exoemail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import com.example.exoemail.ui.thread.AttachmentState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import com.example.exoemail.LiquidMailApp
import com.example.exoemail.core.mail.InlineImage
import com.example.exoemail.core.mail.MailBody
import com.example.exoemail.data.db.MessageEntity
import com.example.exoemail.ui.components.GlassBackButton
import com.example.exoemail.ui.components.GlassButton
import com.example.exoemail.ui.components.GlassTopBar
import com.example.exoemail.ui.thread.ThreadViewModel
import com.example.exoemail.ui.thread.ThreadViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ThreadScreen(
    app: LiquidMailApp,
    messageId: String,
    onBack: () -> Unit,
    onReply: (MessageEntity) -> Unit,
    onForward: (MessageEntity) -> Unit,
) {
    val vm: ThreadViewModel = viewModel(factory = ThreadViewModelFactory(app))
    val message by vm.message.collectAsState()
    val body by vm.body.collectAsState()
    val attachments by vm.attachments.collectAsState()
    val inlineImages by vm.inlineImages.collectAsState()
    val attachmentStates by vm.attachmentStates.collectAsState()
    val conversation by vm.conversation.collectAsState()
    val notice by vm.notice.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    // One-shot notices (saved to Downloads, move/delete results).
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    androidx.compose.runtime.LaunchedEffect(notice) {
        notice?.let { snackbar.showSnackbar(it); vm.consumeNotice() }
    }
    var showMenu by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    val folders by vm.folders.collectAsState()

    // Switch the shared VM to this message (cancels stale in-flight fetches)
    androidx.compose.runtime.LaunchedEffect(messageId) { vm.open(messageId) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
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
                            DropdownMenuItem(
                                text = { Text(if (message?.isStarred == true) "Unstar" else "Star") },
                                onClick = { vm.star(); showMenu = false },
                            )
                            DropdownMenuItem(
                                text = { Text(if (message?.isRead == true) "Mark as unread" else "Mark as read") },
                                onClick = { vm.toggleRead(); showMenu = false },
                            )
                            DropdownMenuItem(
                                text = { Text("Archive") },
                                onClick = { vm.archive(); showMenu = false },
                            )
                            DropdownMenuItem(
                                text = { Text("Move to folder") },
                                onClick = { showFolderPicker = true; showMenu = false },
                            )
                            DropdownMenuItem(
                                text = { Text("Report spam") },
                                onClick = { vm.reportSpam(); showMenu = false },
                            )
                            DropdownMenuItem(
                                text = { Text("Snooze 1 hour") },
                                onClick = { vm.snooze(System.currentTimeMillis() + 3_600_000); showMenu = false },
                            )
                            DropdownMenuItem(
                                text = { Text("Snooze until tomorrow") },
                                onClick = {
                                    vm.snooze(System.currentTimeMillis() + 24 * 3_600_000)
                                    showMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = { vm.delete(); showMenu = false },
                            )
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
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            message?.let { m ->
                // Conversation header - other messages sharing this threadId.
                if (conversation.size > 1) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "${conversation.size} messages in this conversation",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            conversation.filter { it.id != m.id }.take(4).forEach { other ->
                                Text(
                                    "- ${other.senderName.ifBlank { other.sender }}: ${other.subject}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                // Sender header with avatar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            (m.senderName.ifBlank { m.sender }).take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(m.senderName.ifBlank { m.sender }, style = MaterialTheme.typography.titleMedium)
                        Text(
                            m.sender,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            SimpleDateFormat("EEE, MMM d HH:mm", Locale.getDefault()).format(Date(m.date)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                // Body â€” HTML in WebView (images + formatting), plain text fallback
                MailBodyView(body = body, message = m)

                // Attachments — tap to open, trailing button saves to Downloads.
                if (attachments.isNotEmpty()) {
                    Text("Attachments (${attachments.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    attachments.forEach { att ->
                        val st = attachmentStates[att.index] ?: AttachmentState.Idle
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = st !is AttachmentState.Downloading) {
                                    vm.downloadAttachment(att.index, open = true, context = context)
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (st is AttachmentState.Downloading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(att.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                    Text(
                                        if (st is AttachmentState.Failed) st.message
                                        else "${att.mimeType} - ${formatSize(att.size)}",
                                        color = if (st is AttachmentState.Failed) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                IconButton(
                                    onClick = { vm.downloadAttachment(att.index, open = false, context = context) },
                                    enabled = st !is AttachmentState.Downloading,
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = "Save to Downloads", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                }

                    }

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(onClick = { onReply(m) }, label = "Reply")
                    GlassButton(onClick = { onForward(m) }, label = "Forward")
                    GlassButton(onClick = { vm.star() }, label = if (m.isStarred) "Unstar" else "Star")
                }

            }
        }
    }

    // "Move to folder" picker — lists server folders from the last folder sync.
    if (showFolderPicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showFolderPicker = false },
            title = { Text("Move to folder") },
            text = {
                Column {
                    if (folders.isEmpty()) Text("No folders synced yet")
                    folders.forEach { f ->
                        Text(
                            f.displayName.ifBlank { f.name },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.moveTo(f.name); showFolderPicker = false }
                                .padding(vertical = 10.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showFolderPicker = false }) { Text("Cancel") }
            },
        )
    }

}
/**
 * Renders the message body. HTML mail goes through a WebView so formatting,
 * remote images, and cid: inline images all display like a real mail client.
 * Plain-text-only mail falls back to a styled Text block.
 */
@Composable
private fun MailBodyView(body: MailBody?, message: MessageEntity) {
    val html = body?.html
    if (html != null) {
        val dark = MaterialTheme.colorScheme.background == androidx.compose.ui.graphics.Color(0xFF0E1116) ||
            androidx.compose.foundation.isSystemInDarkTheme()
        val bg = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
        val fg = MaterialTheme.colorScheme.onSurface.toArgb()
        val imgMap = remember(body) {
            body?.inlineImages?.associateBy { it.contentId } ?: emptyMap()
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = false
                        settings.loadsImagesAutomatically = true
                        settings.blockNetworkImage = false
                        setBackgroundColor(bg)
                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest,
                            ): android.webkit.WebResourceResponse? {
                                val url = request.url.toString()
                                if (url.startsWith("liquidimg://")) {
                                    val cid = url.removePrefix("liquidimg://")
                                    val img = imgMap[cid]
                                    if (img != null) {
                                        return android.webkit.WebResourceResponse(
                                            img.mimeType, "utf-8",
                                            java.io.ByteArrayInputStream(img.bytes),
                                        )
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                        }
                    }
                },
                update = { web ->
                    web.setBackgroundColor(bg)
                    val styled = """
                        <!DOCTYPE html><html><head>
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                        <style>
                          body { color: #${Integer.toHexString(fg).substring(2)}; font-family: sans-serif; font-size: 15px; line-height: 1.5; padding: 0; margin: 0; word-wrap: break-word; }
                          img { max-width: 100%; height: auto; }
                          a { color: #5B8CFF; }
                          blockquote { border-left: 3px solid #888; margin-left: 0; padding-left: 12px; color: #888; }
                        </style></head><body>${html}</body></html>
                    """.trimIndent()
                    web.loadDataWithBaseURL("https://mail.local/", styled, "text/html", "utf-8", null)
                },
                modifier = Modifier.fillMaxWidth().height(400.dp),
            )
        }
    } else {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                body?.text ?: message.snippet,
                modifier = Modifier.padding(18.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.Color.toArgb(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
    )

/** Human-readable attachment size (B / KB / MB). */
private fun formatSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "${bytes} B"
}
