package com.example.email_client.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.email_client.LiquidMailApp
import com.example.email_client.core.mail.MailEngine
import com.example.email_client.core.mail.OutgoingMail
import com.example.email_client.core.mail.engine.MailEngineFactory
import com.example.email_client.core.security.CredentialStore
import com.example.email_client.data.MailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ComposeState {
    data object Idle : ComposeState
    data object Sending : ComposeState
    data object Sent : ComposeState
    data class Error(val message: String) : ComposeState
}

class ComposeViewModel(
    private val app: LiquidMailApp,
    private val repo: MailRepository = MailRepository(app.database, MailEngineFactory.create()),
    private val credentials: CredentialStore = CredentialStore(app),
    private val engine: MailEngine = MailEngineFactory.create(),
) : ViewModel() {

    private val _state = MutableStateFlow<ComposeState>(ComposeState.Idle)
    val state: StateFlow<ComposeState> = _state

    fun send(to: String, subject: String, body: String) {
        viewModelScope.launch {
            _state.value = ComposeState.Sending
            val activeId = app.settings.activeAccountId.first()
            val account = activeId?.let { app.database.accountDao().get(it) }
                ?: app.database.accountDao().first()
            val secret = account?.let { credentials.get(it.id)?.secret }
            if (account == null || secret == null) {
                _state.value = ComposeState.Error("No account configured")
                return@launch
            }
            engine.send(
                repo.accountConfig(account, secret),
                OutgoingMail(to = listOf(to), subject = subject, body = body),
            ).onSuccess { _state.value = ComposeState.Sent }
                .onFailure { _state.value = ComposeState.Error(it.message ?: "Send failed") }
        }
    }
}

class ComposeViewModelFactory(private val app: LiquidMailApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ComposeViewModel(app) as T
}
