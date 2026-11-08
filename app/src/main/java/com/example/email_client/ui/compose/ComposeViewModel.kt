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
import com.example.email_client.data.db.MessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ComposeState {
    data object Idle : ComposeState
    data object Sending : ComposeState
    data object Sent : ComposeState
    data object Saved : ComposeState
    data class Error(val message: String) : ComposeState
}

data class ComposeDraft(
    val to: String = "",
    val cc: String = "",
    val bcc: String = "",
    val subject: String = "",
    val body: String = "",
)

class ComposeViewModel(
    private val app: LiquidMailApp,
    private val draftId: String? = null,
    private val repo: MailRepository = MailRepository(app.database, MailEngineFactory.create()),
    private val credentials: CredentialStore = CredentialStore(app),
    private val engine: MailEngine = MailEngineFactory.create(),
) : ViewModel() {

    private val _state = MutableStateFlow<ComposeState>(ComposeState.Idle)
    val state: StateFlow<ComposeState> = _state

    private val _draft = MutableStateFlow<ComposeDraft?>(null)
    val draft: StateFlow<ComposeDraft?> = _draft

    init {
        if (draftId != null) {
            viewModelScope.launch {
                val m = app.database.messageDao().get(draftId)
                if (m != null && m.isDraft) {
                    _draft.value = ComposeDraft(
                        to = m.recipients,
                        subject = m.subject,
                        body = m.body ?: "",
                    )
                }
            }
        }
    }

    private suspend fun activeAccount(): Pair<com.example.email_client.data.db.AccountEntity, String>? {
        val activeId = app.settings.activeAccountId.first()
        val account = activeId?.let { app.database.accountDao().get(it) }
            ?: app.database.accountDao().first()
        val secret = account?.let { credentials.get(it.id)?.secret }
        return if (account != null && secret != null) account to secret else null
    }

    fun send(to: String, cc: String, bcc: String, subject: String, body: String) {
        viewModelScope.launch {
            _state.value = ComposeState.Sending
            val (account, secret) = activeAccount()
                ?: run { _state.value = ComposeState.Error("No account configured"); return@launch }
            val toList = to.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val ccList = cc.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val bccList = bcc.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            engine.send(
                repo.accountConfig(account, secret),
                OutgoingMail(
                    to = toList,
                    cc = ccList,
                    bcc = bccList,
                    subject = subject,
                    body = body,
                ),
            ).onSuccess {
                _state.value = ComposeState.Sent
                // Remove draft if this was one
                draftId?.let { runCatching { repo.deleteMessage(it) } }
                // also save to Sent folder locally so Sent tab shows it
                runCatching {
                    val id = "${account.id}:Sent:${System.currentTimeMillis()}"
                    app.database.messageDao().upsertAll(listOf(
                        MessageEntity(
                            id = id, accountId = account.id, folder = "Sent", uid = 0,
                            threadId = "", subject = subject, sender = account.email,
                            senderName = account.displayName, recipients = to,
                            date = System.currentTimeMillis(), snippet = body.take(140),
                            isRead = true, isStarred = false, hasAttachments = false,
                            isDraft = false, bodyPath = null, body = body,
                        )
                    ))
                }
            }
                .onFailure { _state.value = ComposeState.Error(it.message ?: "Send failed") }
        }
    }

    fun saveDraft(to: String, subject: String, body: String) {
        viewModelScope.launch {
            val (account, _) = activeAccount() ?: return@launch
            if (draftId != null) {
                repo.updateDraft(draftId, to, subject, body)
            } else {
                repo.saveDraft(account.id, to, subject, body)
            }
            _state.value = ComposeState.Saved
        }
    }
}

class ComposeViewModelFactory(
    private val app: LiquidMailApp,
    private val draftId: String? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ComposeViewModel(app, draftId) as T
}
