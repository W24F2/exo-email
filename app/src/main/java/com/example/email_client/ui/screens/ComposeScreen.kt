package com.example.email_client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.email_client.LiquidMailApp
import com.example.email_client.ui.components.GlassTopBar
import com.example.email_client.ui.components.LiquidBackdrop
import com.example.email_client.ui.compose.ComposeState
import com.example.email_client.ui.compose.ComposeViewModel
import com.example.email_client.ui.compose.ComposeViewModelFactory

@Composable
fun ComposeScreen(app: LiquidMailApp, replyTo: String?, onBack: () -> Unit) {
    val vm: ComposeViewModel = viewModel(factory = ComposeViewModelFactory(app))
    val state by vm.state.collectAsState()

    var to by remember { mutableStateOf(replyTo ?: "") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(state) {
        if (state is ComposeState.Sent) onBack()
    }

    LiquidBackdrop {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = { GlassTopBar(title = "New message") },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(value = to, onValueChange = { to = it }, label = { Text("To") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth().weight(1f))
                when (val s = state) {
                    is ComposeState.Sending -> CircularProgressIndicator()
                    is ComposeState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                    else -> {}
                }
                Button(
                    onClick = { vm.send(to, subject, body) },
                    enabled = to.contains('@') && state !is ComposeState.Sending,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Send") }
            }
        }
    }
}
