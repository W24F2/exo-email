package com.example.exoemail.ui.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.exoemail.LiquidMailApp
import com.example.exoemail.core.mail.MailBody
import com.example.exoemail.core.mail.AttachmentMeta
import com.example.exoemail.core.mail.engine.MailEngineFactory
import com.example.exoemail.core.security.CredentialStore
import com.example.exoemail.data.MailRepository
import com.example.exoemail.data.db.MessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ThreadViewModel(
    private val app: LiquidMailApp,
    private val repo: MailRepository = MailRepository(app.database, MailEngineFactory.create()),
    private val credentials: CredentialStore = CredentialStore(app),
    private val engine: com.example.exoemail.core.mail.MailEngine = MailEngineFactory.create(),
) : ViewModel() {

    private val _message = MutableStateFlow<MessageEntity?>(null)
    val message: StateFlow<MessageEntity?> = _message
    private val _body = MutableStateFlow<MailBody?>(null)
    val body: StateFlow<MailBody?> = _body
    private val _attachments = MutableStateFlow<List<AttachmentMeta>>(emptyList())
    val attachments: StateFlow<List<AttachmentMeta>> = _attachments
    private val _inlineImages = MutableStateFlow<List<com.example.exoemail.core.mail.InlineImage>>(emptyList())
    val inlineImages: StateFlow<List<com.example.exoemail.core.mail.InlineImage>> = _inlineImages

    private var currentId: String? = null
    private var loadJob: kotlinx.coroutines.Job? = null

    /** Switch to a different message; cancels any in-flight fetch of the old one. */
    fun open(id: String) {
        if (currentId == id) return
        currentId = id
        _message.value = null
        _body.value = null
        _attachments.value = emptyList()
        _inlineImages.value = emptyList()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val m = app.database.messageDao().get(id)
            if (currentId != id) return@launch
            _message.value = m
            if (m == null) return@launch
            app.database.messageDao().setRead(id, true)
            val account = app.database.accountDao().get(m.accountId)
            val secret = account?.let { credentials.get(it.id)?.secret }
            if (account != null && secret != null) {
                val fetched = engine.fetchBody(repo.accountConfig(account, secret), m.folder, m.uid).getOrNull()
                if (currentId != id) return@launch
                _body.value = fetched
                _attachments.value = fetched?.attachments ?: emptyList()
                _inlineImages.value = fetched?.inlineImages ?: emptyList()
            }
        }
    }

    fun star() {
        viewModelScope.launch {
            val m = _message.value ?: return@launch
            app.database.messageDao().setStarred(m.id, !m.isStarred)
            _message.value = m.copy(isStarred = !m.isStarred)
            // best-effort server flag sync (same policy as inbox star)
            if (m.uid > 0) {
                val account = app.database.accountDao().get(m.accountId)
                val secret = account?.let { runCatching { credentials.get(it.id)?.secret }.getOrNull() }
                if (account != null && secret != null) {
                    runCatching { engine.setStarred(repo.accountConfig(account, secret), m.folder, m.uid, !m.isStarred) }
                }
            }
        }
    }
}

class ThreadViewModelFactory(
    private val app: LiquidMailApp,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ThreadViewModel(app) as T
}
