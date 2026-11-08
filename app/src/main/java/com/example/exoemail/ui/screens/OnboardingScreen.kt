package com.example.exoemail.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.exoemail.LiquidMailApp
import com.example.exoemail.core.mail.AuthType
import com.example.exoemail.core.mail.ProviderCatalog
import com.example.exoemail.ui.components.GlassShape
import com.example.exoemail.ui.onboarding.OnboardingState
import com.example.exoemail.ui.onboarding.OnboardingViewModel
import com.example.exoemail.ui.onboarding.OnboardingViewModelFactory

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

    androidx.compose.runtime.LaunchedEffect(state) {
        if (state is OnboardingState.Done) onDone()
    }

    val fieldColors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Liquid Mail", style = MaterialTheme.typography.headlineSmall)
            Text("Add an account", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Provider grid - 2 per row via chunked list
        items(ProviderCatalog.all.chunked(2)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { p ->
                    val isSelected = selected == p.id
                    Surface(
                        shape = GlassShape.Surface,
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selected = p.id
                                imapHost = p.imap.host
                                smtpHost = p.smtp.host
                                showManual = p.id == "custom"
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    p.displayName.first().toString(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Column {
                                Text(p.displayName, style = MaterialTheme.typography.bodyMedium)
                                if (p.auth == AuthType.APP_PASSWORD) {
                                    Text("app password", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }

        item {
            val provider = ProviderCatalog.byId(selected)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        ProviderCatalog.detect(it)?.let { p ->
                            selected = p.id
                            imapHost = p.imap.host
                            smtpHost = p.smtp.host
                        }
                    },
                    label = { Text("Email") },
                    singleLine = true,
                    shape = GlassShape.Field,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (provider?.auth == AuthType.APP_PASSWORD) "App password / auth code" else "Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = GlassShape.Field,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                provider?.helpUrl?.let { url ->
                    Text(
                        "How to get a password",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                    )
                }
                if (showManual) {
                    OutlinedTextField(
                        value = imapHost, onValueChange = { imapHost = it },
                        label = { Text("IMAP host") }, singleLine = true,
                        shape = GlassShape.Field, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = smtpHost, onValueChange = { smtpHost = it },
                        label = { Text("SMTP host") }, singleLine = true,
                        shape = GlassShape.Field, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                when (val s = state) {
                    is OnboardingState.Testing -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        Text("Testing connection", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    is OnboardingState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                    else -> {}
                }
                Button(
                    onClick = { vm.addAccount(email, password, selected, imapHost, smtpHost) },
                    enabled = email.contains('@') && password.isNotEmpty() && state !is OnboardingState.Testing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = GlassShape.Pill,
                ) {
                    Text("Connect")
                }
            }
        }
    }
}
