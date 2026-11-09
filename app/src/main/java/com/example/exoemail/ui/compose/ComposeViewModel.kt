package com.example.exoemail.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.exoemail.LiquidMailApp
import com.example.exoemail.core.mail.OutgoingMail
import com.example.exoemail.core.mail.engine.MailEngineFactory
import com.example.exoemail.core.security.CredentialStore
import com.example.exoemail.data.MailRepository
import com.example.exoemail.data.db.MessageEntity
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
    val inReplyTo: String? = null,
    val references: String? = null,
)

class ComposeViewModel(
    private val app: LiquidMailApp,
    private val draftId: String? = null,
    private val replyTo: String? = null,
    private val inReplyTo: String? = null,
    private val references: String? = null,
    private val replySubject: String? = null,
    private val quotedBody: String? = null,
    private val forward: Boolean = false,
    private val repo: MailRepository = MailRepository(app.database, MailEngineFactory.create(), CredentialStore(app)),
) : ViewModel() {

    private val _state = MutableStateFlow<ComposeState>(ComposeState.Idle)
    val state: StateFlow<ComposeState> = _state

    private val _draft = MutableStateFlow<ComposeDraft?>(null)
    val draft: StateFlow<ComposeDraft?> = _draft

    /** Files staged for send (multipart/mixed). */
    private val _attachments = MutableStateFlow<List<com.example.exoemail.core.mail.OutgoingAttachment>>(emptyList())
    val attachments: StateFlow<List<com.example.exoemail.core.mail.OutgoingAttachment>> = _attachments

    fun addAttachment(fileName: String, mimeType: String, bytes: ByteArray) {
        _attachments.value = _attachments.value +
            com.example.exoemail.core.mail.OutgoingAttachment(fileName, mimeType, bytes)
    }

    /** Read the picked file off the main thread, then stage it. */
    fun addAttachment(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val resolver = context.contentResolver
            val name = run {
                var n = "attachment"
                resolver.query(uri, null, null, null, null)?.use { c ->
                    val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (i >= 0 && c.moveToFirst()) n = c.getString(i) ?: n
                }
                n
            }
            val mime = resolver.getType(uri) ?: "application/octet-stream"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
            addAttachment(name, mime, bytes)
        }
    }

    fun removeAttachment(index: Int) {
        _attachments.value = _attachments.value.filterIndexed { i, _ -> i != index }
    }

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
        } else if (forward) {
            // Forward: Fwd: subject, empty To, original body quoted below.
            _draft.value = ComposeDraft(
                to = "",
                subject = replySubject?.let { if (it.startsWith("Fwd: ", true) || it.startsWith("Fw: ", true)) it else "Fwd: $it" } ?: "",
                body = quotedBody?.let { "\n\n---------- Forwarded message ----------\n$it" } ?: "",
                // Keep References so the forward threads where it belongs.
                references = references,
            )
        } else if (replyTo != null) {
            _draft.value = ComposeDraft(
                to = replyTo,
                subject = replySubject?.let { if (it.startsWith("Re: ", true)) it else "Re: $it" } ?: "",
                body = quotedBody?.let { "\n\n---\nOriginal message:\n$it" } ?: "",
                inReplyTo = inReplyTo,
                references = references,
            )
        }
    }

    private suspend fun activeAccount(): com.example.exoemail.data.db.AccountEntity? {
        val activeId = app.settings.activeAccountId.first()
        return activeId?.let { repo.account(it) } ?: repo.firstAccount()
    }

    fun send(to: String, cc: String, bcc: String, subject: String, body: String) {
        viewModelScope.launch {
            _state.value = ComposeState.Sending
            val account = activeAccount()
                ?: run { _state.value = ComposeState.Error("No account configured"); return@launch }
            val toList = to.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val ccList = cc.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val bccList = bcc.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val currentDraft = _draft.value ?: ComposeDraft()
            repo.send(
                account,
                OutgoingMail(
                    to = toList,
                    cc = ccList,
                    bcc = bccList,
                    subject = subject,
                    body = body,
                    inReplyTo = currentDraft.inReplyTo,
                    references = currentDraft.references,
                    isHtml = false,
                    attachments = _attachments.value,
                ),
            ).onSuccess {
                _state.value = ComposeState.Sent
                // Remove draft if this was one
                draftId?.let { runCatching { repo.deleteMessage(it) } }
            }
                .onFailure { _state.value = ComposeState.Error(it.message ?: "Send failed") }
        }
    }

    fun saveDraft(to: String, subject: String, body: String) {
        viewModelScope.launch {
            val account = activeAccount() ?: return@launch
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
    private val replyTo: String? = null,
    private val inReplyTo: String? = null,
    private val references: String? = null,
    private val replySubject: String? = null,
    private val quotedBody: String? = null,
    private val forward: Boolean = false,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ComposeViewModel(app, draftId, replyTo, inReplyTo, references, replySubject, quotedBody, forward) as T
}
