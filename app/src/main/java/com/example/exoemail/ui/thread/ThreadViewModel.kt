package com.example.exoemail.ui.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.exoemail.LiquidMailApp
import com.example.exoemail.core.mail.AttachmentMeta
import com.example.exoemail.core.mail.MailBody
import com.example.exoemail.core.mail.engine.MailEngineFactory
import com.example.exoemail.core.security.CredentialStore
import com.example.exoemail.data.MailRepository
import com.example.exoemail.data.db.MessageEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Per-attachment download state for the thread screen. */
sealed interface AttachmentState {
    data object Idle : AttachmentState
    data object Downloading : AttachmentState
    data class Failed(val message: String) : AttachmentState
}

class ThreadViewModel(
    private val app: LiquidMailApp,
    private val repo: MailRepository = MailRepository(app.database, MailEngineFactory.create(), CredentialStore(app)),
) : ViewModel() {

    private val _message = MutableStateFlow<MessageEntity?>(null)
    val message: StateFlow<MessageEntity?> = _message
    private val _body = MutableStateFlow<MailBody?>(null)
    val body: StateFlow<MailBody?> = _body
    private val _attachments = MutableStateFlow<List<AttachmentMeta>>(emptyList())
    val attachments: StateFlow<List<AttachmentMeta>> = _attachments
    private val _inlineImages = MutableStateFlow<List<com.example.exoemail.core.mail.InlineImage>>(emptyList())
    val inlineImages: StateFlow<List<com.example.exoemail.core.mail.InlineImage>> = _inlineImages

    /** Other messages in the same conversation (by threadId), oldest first. */
    private val _conversation = MutableStateFlow<List<MessageEntity>>(emptyList())
    val conversation: StateFlow<List<MessageEntity>> = _conversation

    /** Server folders for the "Move to" picker. */
    private val _folders = MutableStateFlow<List<com.example.exoemail.data.db.FolderEntity>>(emptyList())
    val folders: StateFlow<List<com.example.exoemail.data.db.FolderEntity>> = _folders

    /** Download state keyed by attachment index. */
    private val _attachmentStates = MutableStateFlow<Map<Int, AttachmentState>>(emptyMap())
    val attachmentStates: StateFlow<Map<Int, AttachmentState>> = _attachmentStates

    /** One-shot user-facing message (saved to Downloads, errors). */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice

    private var currentId: String? = null
    private var loadJob: Job? = null

    fun consumeNotice() { _notice.value = null }

    /** Switch to a different message; cancels any in-flight fetch of the old one. */
    fun open(id: String) {
        if (currentId == id) return
        currentId = id
        _message.value = null
        _body.value = null
        _attachments.value = emptyList()
        _inlineImages.value = emptyList()
        _conversation.value = emptyList()
        _attachmentStates.value = emptyMap()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val m = repo.message(id)
            if (currentId != id) return@launch
            _message.value = m
            if (m == null) return@launch
            repo.markRead(id, true)
            _conversation.value = runCatching { repo.conversation(m.accountId, m.threadId) }
                .getOrDefault(emptyList())
            _folders.value = runCatching { repo.observeFolders(m.accountId).first() }
                .getOrDefault(emptyList())
            val fetched = repo.body(id).getOrNull()
            if (currentId != id) return@launch
            _body.value = fetched
            _attachments.value = fetched?.attachments ?: emptyList()
            _inlineImages.value = fetched?.inlineImages ?: emptyList()
        }
    }

    fun star() {
        viewModelScope.launch {
            val m = _message.value ?: return@launch
            repo.setStarred(m.id, !m.isStarred)
            _message.value = m.copy(isStarred = !m.isStarred)
        }
    }

    /** Toggle read/unread, local + best-effort server (repo owns the ordering). */
    fun toggleRead() {
        viewModelScope.launch {
            val m = _message.value ?: return@launch
            val nowRead = !m.isRead
            repo.markRead(m.id, nowRead)
            _message.value = m.copy(isRead = nowRead)
        }
    }

    /** Archive: move to the account's Archive folder. */
    fun archive() = moveTo("Archive")

    /** Report spam: move to the Spam/Junk folder. */
    fun reportSpam() = moveTo("Spam")

    fun moveTo(targetFolder: String) {
        viewModelScope.launch {
            val m = _message.value ?: return@launch
            val r = repo.move(m.id, targetFolder)
            if (r.isSuccess) {
                _notice.value = "Moved to $targetFolder"
                _message.value = m.copy(folder = targetFolder, id = "${m.accountId}:$targetFolder:${m.uid}")
            } else {
                _notice.value = "Couldn't move: ${r.exceptionOrNull()?.message ?: "unknown error"}"
            }
        }
    }

    /** Permanently delete (Room first; repo pushes the server delete). */
    fun delete() {
        viewModelScope.launch {
            val m = _message.value ?: return@launch
            val r = repo.delete(m.id)
            if (r.isSuccess) {
                _notice.value = "Message deleted"
                _message.value = null
            } else {
                _notice.value = "Couldn't delete: ${r.exceptionOrNull()?.message ?: "unknown error"}"
            }
        }
    }

    /** Snooze locally: hide from the inbox until [until]. */
    fun snooze(until: Long) {
        viewModelScope.launch {
            val m = _message.value ?: return@launch
            repo.setSnoozedUntil(m.id, until)
            _notice.value = "Snoozed"
        }
    }

    /**
     * Download one attachment. [open] decides what happens with the bytes:
     * open in an external viewer, or save into Downloads.
     */
    fun downloadAttachment(
        index: Int,
        open: Boolean,
        context: android.content.Context,
    ) {
        if (_attachmentStates.value[index] is AttachmentState.Downloading) return
        viewModelScope.launch {
            val m = _message.value ?: return@launch
            val meta = _attachments.value.firstOrNull { it.index == index } ?: return@launch
            _attachmentStates.value = _attachmentStates.value + (index to AttachmentState.Downloading)
            val result = repo.attachment(m.id, index)
            val bytes = result.getOrNull()
            if (bytes == null) {
                _attachmentStates.value = _attachmentStates.value + (
                    index to AttachmentState.Failed(result.exceptionOrNull()?.message ?: "download failed")
                    )
                return@launch
            }
            val acted = if (open) {
                com.example.exoemail.core.file.AttachmentOpener.open(context, meta.fileName, meta.mimeType, bytes)
            } else {
                com.example.exoemail.core.file.AttachmentOpener.saveToDownloads(context, meta.fileName, meta.mimeType, bytes)
                    .map { }
            }
            _attachmentStates.value = _attachmentStates.value + (index to AttachmentState.Idle)
            if (acted.isSuccess) {
                if (!open) _notice.value = "Saved ${meta.fileName} to Downloads"
            } else {
                _notice.value = "Couldn't ${if (open) "open" else "save"}: ${acted.exceptionOrNull()?.message ?: "error"}"
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
