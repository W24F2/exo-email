package com.example.exoemail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.exoemail.LiquidMailApp
import com.example.exoemail.ui.components.GlassBackButton
import com.example.exoemail.ui.components.GlassShape
import com.example.exoemail.ui.components.GlassTopBar
import com.example.exoemail.ui.compose.ComposeState
import com.example.exoemail.ui.compose.ComposeViewModel
import com.example.exoemail.ui.compose.ComposeViewModelFactory

@Composable
fun ComposeScreen(app: LiquidMailApp, replyTo: String?, draftId: String?, inReplyTo: String? = null, references: String? = null, subject: String? = null, quotedBody: String? = null, forward: Boolean = false, onBack: () -> Unit) {
    val vm: ComposeViewModel = viewModel(factory = ComposeViewModelFactory(app, draftId, replyTo, inReplyTo, references, subject, quotedBody, forward))
    val state by vm.state.collectAsState()
    val draft by vm.draft.collectAsState()
    val attachments by vm.attachments.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // File picker for attach-on-send.
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val resolver = context.contentResolver
        val name = run {
            var n = "attachment"
            resolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && c.moveToFirst()) n = c.getString(i) ?: n
            }
            n
        }
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        vm.addAttachment(name, mime, bytes)
    }

    var to by remember { mutableStateOf("") }
    var cc by remember { mutableStateOf("") }
    var bcc by remember { mutableStateOf("") }
    var showCcBcc by remember { mutableStateOf(false) }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var hydrated by remember { mutableStateOf(false) }

    // Hydrate fields once when editing an existing draft
    LaunchedEffect(draft) {
        if (!hydrated && draft != null) {
            to = draft!!.to
            subject = draft!!.subject
            body = draft!!.body
            hydrated = true
        }
    }
    // For replies (not drafts/forwards), prefill To — forwards leave it empty.
    LaunchedEffect(replyTo) {
        if (replyTo != null && !forward && to.isEmpty()) to = replyTo
    }

    LaunchedEffect(state) {
        if (state is ComposeState.Sent || state is ComposeState.Saved) onBack()
    }

    val sending = state is ComposeState.Sending
    val canSend = to.contains('@') && !sending
    val hasContent = to.isNotBlank() || cc.isNotBlank() || bcc.isNotBlank() ||
        subject.isNotBlank() || body.isNotBlank()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GlassTopBar(
                title = if (draftId != null) "Edit draft" else "New message",
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = { GlassBackButton(onBack) },
            )
        },
        bottomBar = {
            ComposeActionBar(
                sending = sending,
                canSend = canSend,
                hasContent = hasContent,
                onSend = { vm.send(to, cc, bcc, subject, body) },
                onSaveDraft = { vm.saveDraft(to, subject, body) },
                onAttach = { picker.launch(arrayOf("*/*")) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Recipients — compact labeled rows
            ComposeRecipientRow("To", to, { to = it }, enabled = !sending)
            if (showCcBcc) {
                ComposeRecipientRow("Cc", cc, { cc = it }, enabled = !sending)
                ComposeRecipientRow("Bcc", bcc, { bcc = it }, enabled = !sending)
            }
            TextButton(
                onClick = { showCcBcc = !showCcBcc },
                enabled = !sending,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            ) {
                Text(
                    if (showCcBcc) "Hide Cc/Bcc" else "Cc/Bcc",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Subject — quiet, single line
            TextField(
                value = subject,
                onValueChange = { subject = it },
                placeholder = { Text("Subject", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                enabled = !sending,
                textStyle = MaterialTheme.typography.titleMedium,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                colors = composeFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Staged attachments — removable chips.
            if (attachments.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                ) {
                    attachments.forEachIndexed { i, att ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 10.dp),
                            ) {
                                Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    " ${att.fileName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                )
                                IconButton(onClick = { vm.removeAttachment(i) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Body — fills remaining space, comfortable text
            TextField(
                value = body,
                onValueChange = { body = it },
                placeholder = { Text("Write your message…", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                enabled = !sending,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Default,
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                colors = composeFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            // Inline status / error
            when (val s = state) {
                is ComposeState.Sending -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Sending…", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                is ComposeState.Error -> Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    Text(
                        s.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                else -> {}
            }
        }
    }
}

/** A Gmail-style recipient row: fixed-width label + underline-free text field. */
@Composable
private fun ComposeRecipientRow(label: String, value: String, onChange: (String) -> Unit, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp),
        )
        TextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text("name@example.com", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
            singleLine = true,
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = if (label == "To") ImeAction.Next else ImeAction.Done,
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            colors = composeFieldColors(),
            modifier = Modifier.weight(1f),
        )
        if (value.isNotEmpty() && enabled) {
            IconButton(onClick = { onChange("") }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Clear $label", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Bottom action bar: tonal "Save draft" + prominent "Send". */
@Composable
private fun ComposeActionBar(
    sending: Boolean,
    canSend: Boolean,
    hasContent: Boolean,
    onSend: () -> Unit,
    onSaveDraft: () -> Unit,
    onAttach: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(onClick = onAttach, enabled = !sending) {
                Icon(
                    Icons.Filled.AttachFile, contentDescription = "Attach file",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onSaveDraft,
                enabled = hasContent && !sending,
                shape = GlassShape.Pill,
            ) {
                Text("Save draft")
            }
            Button(
                onClick = onSend,
                enabled = canSend,
                shape = GlassShape.Pill,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(if (sending) "Sending" else "Send", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

/** Borderless field colors — flat inputs that separate via dividers. */
@Composable
private fun composeFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
)
