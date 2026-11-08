package com.example.email_client.ui.screens

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.email_client.LiquidMailApp
import com.example.email_client.core.mail.AuthType
import com.example.email_client.core.mail.ProviderCatalog
import com.example.email_client.ui.components.GlassButton
import com.example.email_client.ui.components.GlassCard
import com.example.email_client.ui.components.GlassShape
import com.example.email_client.ui.components.LiquidBackdrop
import com.example.email_client.ui.onboarding.OnboardingState
import com.example.email_client.ui.onboarding.OnboardingViewModel
import com.example.email_client.ui.onboarding.OnboardingViewModelFactory
import com.example.email_client.ui.theme.GlassFill
import com.example.email_client.ui.theme.GlassStroke
import com.example.email_client.ui.theme.LiquidCyan
import com.example.email_client.ui.theme.LocalAccentGradient
import com.example.email_client.ui.theme.LocalCoolGradient
import com.example.email_client.ui.theme.OnAccent
import com.example.email_client.ui.theme.TextSecondary
import dev.chrisbanes.haze.HazeState

@Composable
fun OnboardingScreen(app: LiquidMailApp, onDone: () -> Unit) {
    val vm: OnboardingViewModel = viewModel(factory = OnboardingViewModelFactory(app))
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val haze = remember { HazeState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var imapHost by remember { mutableStateOf("") }
    var smtpHost by remember { mutableStateOf("") }
    var showManual by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(ProviderCatalog.all.first().id) }

    androidx.compose.runtime.LaunchedEffect(state) {
        if (state is OnboardingState.Done) onDone()
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = GlassFill.copy(alpha = 0.08f),
        unfocusedContainerColor = GlassFill.copy(alpha = 0.05f),
        focusedBorderColor = LiquidCyan.copy(alpha = 0.6f),
        unfocusedBorderColor = GlassStroke.copy(alpha = 0.22f),
        focusedTextColor = MaterialTheme.colorScheme.onBackground,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        focusedLabelColor = LiquidCyan,
        unfocusedLabelColor = TextSecondary,
        cursorColor = LiquidCyan,
    )

    LiquidBackdrop(haze = haze) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Liquid Mail", style = MaterialTheme.typography.titleLarge)
                Text("Add an account", color = TextSecondary)
            }

            // Provider grid - 2 per row via chunked list
            items(ProviderCatalog.all.chunked(2)) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { p ->
                        val isSelected = selected == p.id
                        GlassCard(
                            haze = haze,
                            tintAlpha = if (isSelected) 0.22f else 0.09f,
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
                                        .background(
                                            if (isSelected) LocalAccentGradient.current
                                            else LocalCoolGradient.current
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(p.displayName.first().toString(), color = OnAccent)
                                }
                                Column {
                                    Text(p.displayName, style = MaterialTheme.typography.bodyMedium)
                                    if (p.auth == AuthType.APP_PASSWORD) {
                                        Text("app password", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
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
                            color = LiquidCyan,
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
                            CircularProgressIndicator(color = LiquidCyan, modifier = Modifier.size(18.dp))
                            Text("Testing connection", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        is OnboardingState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                        else -> {}
                    }
                    GlassButton(
                        onClick = { vm.addAccount(email, password, selected, imapHost, smtpHost) },
                        enabled = email.contains('@') && password.isNotEmpty() && state !is OnboardingState.Testing,
                        label = "Connect",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
