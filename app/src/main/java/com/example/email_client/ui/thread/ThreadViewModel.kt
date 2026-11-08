package com.example.email_client.ui.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.email_client.LiquidMailApp
import com.example.email_client.core.mail.MailBody
import com.example.email_client.core.mail.engine.MailEngineFactory
import com.example.email_client.core.security.CredentialStore
import com.example.email_client.data.MailRepository
import com.example.email_client.data.db.MessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ThreadViewModel(
    private val app: LiquidMailApp,
    private val messageId: String,
    private val repo: MailRepository = MailRepository(app.database, MailEngineFactory.create()),
    private val credentials: CredentialStore = CredentialStore(app),
    private val engine: com.example.email_client.core.mail.MailEngine = MailEngineFactory.create(),
) : ViewModel() {

    private val _message = MutableStateFlow<MessageEntity?>(null)
    val message: StateFlow<MessageEntity?> = _message
    private val _body = MutableStateFlow<MailBody?>(null)
    val body: StateFlow<MailBody?> = _body

    init {
        viewModelScope.launch {
            val m = app.database.messageDao().get(messageId)
            _message.value = m
            if (m != null) {
                app.database.messageDao().setRead(messageId, true)
                val account = app.database.accountDao().get(m.accountId)
                val secret = account?.let { credentials.get(it.id)?.secret }
                if (account != null && secret != null) {
                    engine.fetchBody(repo.accountConfig(account, secret), m.folder, m.uid)
                        .onSuccess { _body.value = it }
                }
            }
        }
    }

    fun star() {
        viewModelScope.launch {
            _message.value?.let {
                app.database.messageDao().setStarred(it.id, !it.isStarred)
                _message.value = it.copy(isStarred = !it.isStarred)
            }
        }
    }
}

class ThreadViewModelFactory(
    private val app: LiquidMailApp,
    private val messageId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ThreadViewModel(app, messageId) as T
}
