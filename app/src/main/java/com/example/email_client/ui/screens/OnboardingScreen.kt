package com.example.email_client.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.email_client.LiquidMailApp
import com.example.email_client.core.mail.ProviderCatalog
import com.example.email_client.ui.components.GlassCard
import com.example.email_client.ui.components.LiquidBackdrop
import com.example.email_client.ui.onboarding.OnboardingState
import com.example.email_client.ui.onboarding.OnboardingViewModel
import com.example.email_client.ui.onboarding.OnboardingViewModelFactory
import com.example.email_client.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(app: LiquidMailApp, onDone: () -> Unit) {
    val vm: OnboardingViewModel = viewModel(factory = OnboardingViewModelFactory(app))
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var imapHost by remember { mutableStateOf("") }
    var smtpHost by remember { mutableStateOf("") }
    var showManual by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(ProviderCatalog.all.first().id) }

    LaunchedEffectState(state, onDone)

    LiquidBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Liquid Mail", style = MaterialTheme.typography.titleLarge)
            Text("Add your first account", color = TextSecondary, modifier = Modifier.padding(bottom = 16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ProviderCatalog.all) { p ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = p.id
                                imapHost = p.imap.host
                                smtpHost = p.smtp.host
                                showManual = p.id == "custom"
                            },
                        tintAlpha = if (selected == p.id) 0.22f else 0.10f,
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(p.displayName.first().toString())
                            }
                            Column {
                                Text(p.displayName)
                                if (p.auth == com.example.email_client.core.mail.AuthType.APP_PASSWORD) {
                                    Text(p.passwordHint ?: "App password required", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            val provider = ProviderCatalog.byId(selected)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 14.dp)) {
                OutlinedTextField(
                    value = email, onValueChange = {
                        email = it
                        ProviderCatalog.detect(it)?.let { p ->
                            selected = p.id
                            imapHost = p.imap.host
                            smtpHost = p.smtp.host
                        }
                    },
                    label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text(if (provider?.auth == com.example.email_client.core.mail.AuthType.APP_PASSWORD) "App password / 授权码" else "Password") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                provider?.helpUrl?.let { url ->
                    Text(
                        "How to get a password",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                    )
                }
                if (showManual) {
                    OutlinedTextField(value = imapHost, onValueChange = { imapHost = it }, label = { Text("IMAP host") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = smtpHost, onValueChange = { smtpHost = it }, label = { Text("SMTP host") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                when (val s = state) {
                    is OnboardingState.Testing -> CircularProgressIndicator()
                    is OnboardingState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                    else -> {}
                }
                Button(
                    onClick = {
                        vm.addAccount(email, password, selected, imapHost, smtpHost)
                    },
                    enabled = email.contains('@') && password.isNotEmpty() && state !is OnboardingState.Testing,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Connect") }
            }
        }
    }
}

@Composable
private fun LaunchedEffectState(state: OnboardingState, onDone: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(state) {
        if (state is OnboardingState.Done) onDone()
    }
}
