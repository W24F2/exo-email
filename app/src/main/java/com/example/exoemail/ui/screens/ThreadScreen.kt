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
import androidx.compose.ui.viewinterop.AndroidView
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
    onReply: (String) -> Unit,
) {
    val vm: ThreadViewModel = viewModel(factory = ThreadViewModelFactory(app))
    val message by vm.message.collectAsState()
    val body by vm.body.collectAsState()
    val attachments by vm.attachments.collectAsState()
    val inlineImages by vm.inlineImages.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    // Switch the shared VM to this message (cancels stale in-flight fetches)
    androidx.compose.runtime.LaunchedEffect(messageId) { vm.open(messageId) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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

                // Attachments
                if (attachments.isNotEmpty()) {
                    Text("Attachments", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    attachments.forEach { att ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Icons.Filled.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(att.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                    Text("${att.mimeType} Â· ${att.size}B", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
