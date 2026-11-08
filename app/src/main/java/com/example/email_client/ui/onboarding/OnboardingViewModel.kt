package com.example.email_client.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.email_client.LiquidMailApp
import com.example.email_client.core.mail.AccountConfig
import com.example.email_client.core.mail.ProviderCatalog
import com.example.email_client.core.mail.ServerSpec
import com.example.email_client.core.mail.engine.MailEngineFactory
import com.example.email_client.core.security.CredentialStore
import com.example.email_client.core.security.Credentials
import com.example.email_client.data.db.AccountEntity
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface OnboardingState {
    data object Idle : OnboardingState
    data object Testing : OnboardingState
    data object Done : OnboardingState
    data class Error(val message: String) : OnboardingState
}

class OnboardingViewModel(
    private val app: LiquidMailApp,
    private val credentials: CredentialStore = CredentialStore(app),
    private val engine: com.example.email_client.core.mail.MailEngine = MailEngineFactory.create(),
) : ViewModel() {

    private val _state = MutableStateFlow<OnboardingState>(OnboardingState.Idle)
    val state: StateFlow<OnboardingState> = _state

    fun addAccount(email: String, password: String, providerId: String, imapHost: String, smtpHost: String) {
        val provider = ProviderCatalog.byId(providerId) ?: ProviderCatalog.all.last()
        viewModelScope.launch {
            _state.value = OnboardingState.Testing
            // Dedupe: reuse an existing account row for this email instead of
            // creating a second one (which splits credentials/messages across ids).
            val existing = app.database.accountDao().byAddress(email.trim())
            val account = existing?.copy(
                providerId = provider.id,
                displayName = provider.displayName,
                imapHost = imapHost.ifBlank { provider.imap.host }.ifBlank { existing.imapHost },
                smtpHost = smtpHost.ifBlank { provider.smtp.host }.ifBlank { existing.smtpHost },
            ) ?: AccountEntity(
                id = UUID.randomUUID().toString(),
                email = email.trim(),
                providerId = provider.id,
                displayName = provider.displayName,
                imapHost = imapHost.ifBlank { provider.imap.host },
                imapPort = provider.imap.port,
                imapSsl = provider.imap.ssl,
                smtpHost = smtpHost.ifBlank { provider.smtp.host },
                smtpPort = provider.smtp.port,
                smtpSsl = provider.smtp.ssl,
                authType = provider.auth.name,
            )
            val config = AccountConfig(
                accountId = account.id, email = account.email, username = account.email,
                password = password,
                imap = ServerSpec(account.imapHost, account.imapPort, account.imapSsl),
                smtp = ServerSpec(account.smtpHost, account.smtpPort, account.smtpSsl),
            )
            engine.testConnection(config)
                .onSuccess {
                    credentials.save(account.id, Credentials(account.email, password, provider.auth.name))
                    app.database.accountDao().upsert(account)
                    app.settings.setActiveAccount(account.id)
                    _state.value = OnboardingState.Done
                }
                .onFailure { _state.value = OnboardingState.Error(it.message ?: "Connection failed") }
        }
    }
}

class OnboardingViewModelFactory(private val app: LiquidMailApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = OnboardingViewModel(app) as T
}
