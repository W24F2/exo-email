package com.example.exoemail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
fun ComposeScreen(app: LiquidMailApp, replyTo: String?, draftId: String?, onBack: () -> Unit) {
    val vm: ComposeViewModel = viewModel(factory = ComposeViewModelFactory(app, draftId))
    val state by vm.state.collectAsState()
    val draft by vm.draft.collectAsState()

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
    // For replies (not drafts), prefill To
    LaunchedEffect(replyTo) {
        if (replyTo != null && to.isEmpty()) to = replyTo
    }

    LaunchedEffect(state) {
        if (state is ComposeState.Sent || state is ComposeState.Saved) onBack()
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GlassTopBar(
                title = if (draftId != null) "Edit draft" else "New message",
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = { GlassBackButton(onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(value = to, onValueChange = { to = it }, label = { Text("To") }, singleLine = true, shape = GlassShape.Field, colors = fieldColors, modifier = Modifier.fillMaxWidth())
            if (!showCcBcc) {
                TextButton(onClick = { showCcBcc = true }) {
                    Text("Cc / Bcc", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                OutlinedTextField(value = cc, onValueChange = { cc = it }, label = { Text("Cc") }, singleLine = true, shape = GlassShape.Field, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bcc, onValueChange = { bcc = it }, label = { Text("Bcc") }, singleLine = true, shape = GlassShape.Field, colors = fieldColors, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, singleLine = true, shape = GlassShape.Field, colors = fieldColors, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Message") }, shape = GlassShape.Field, colors = fieldColors, modifier = Modifier.fillMaxWidth().weight(1f))
            when (val s = state) {
                is ComposeState.Sending -> Text("Sending", color = MaterialTheme.colorScheme.onSurfaceVariant)
                is ComposeState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                else -> {}
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { vm.saveDraft(to, subject, body) },
                    enabled = to.isNotBlank() || subject.isNotBlank() || body.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = GlassShape.Pill,
                ) {
                    Text("Save draft")
                }
                Button(
                    onClick = { vm.send(to, cc, bcc, subject, body) },
                    enabled = to.contains('@') && state !is ComposeState.Sending,
                    modifier = Modifier.weight(1f),
                    shape = GlassShape.Pill,
                ) {
                    Text("Send")
                }
            }
        }
    }
}
