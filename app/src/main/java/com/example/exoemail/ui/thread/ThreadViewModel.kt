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
import com.example.exoemail.data.db.AccountEntity
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
            val m = app.database.messageDao().get(id)
            if (currentId != id) return@launch
            _message.value = m
            if (m == null) return@launch
            app.database.messageDao().setRead(id, true)
            // Load the rest of the conversation in parallel with the body.
            _conversation.value = runCatching {
                app.database.messageDao().conversation(m.accountId, m.threadId)
            }.getOrDefault(emptyList())
            // Snapshot folder list for the "Move to" picker (last folder sync).
            _folders.value = runCatching {
                repo.observeFolders(m.accountId).first()
            }.getOrDefault(emptyList())
            val account = app.database.accountDao().get(m.accountId)
            val secret = account?.let { credentials.get(it.id)?.secret }
            if (account != null && secret != null) {
                val fetched = engine.fetchBody(repo.accountConfig(account, secret), m.folder, m.uid).getOrNull()
                if (currentId != id) return@launch
                _body.value = fetched
                _attachments.value = fetched?.attachments ?: emptyList()
                _inlineImages.value = fetched?.inlineImages ?: emptyList()
                // Mark read on the server too (list tap already does this for inbox).
                if (m.uid > 0) {
                    runCatching { engine.markRead(repo.accountConfig(account, secret), m.folder, m.uid, true) }
                }
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

    /** Toggle read/unread, local + best-effort server. */
    fun toggleRead() {
        viewModelScope.launch {
            val m = _message.value ?: return@launch
            val nowRead = !m.isRead
            app.database.messageDao().setRead(m.id, nowRead)
            _message.value = m.copy(isRead = nowRead)
            if (m.uid > 0) {
                withAccount(m) { account, secret ->
                    runCatching { engine.markRead(repo.accountConfig(account, secret), m.folder, m.uid, nowRead) }
                }
            }
        }
    }

    /** Archive: move to the account's Archive folder. */
    fun archive() = moveTo("Archive")

    /** Report spam: move to the Spam/Junk folder. */
    fun reportSpam() = moveTo("Spam")

    fun moveTo(targetFolder: String) {
        viewModelScope.launch {
            val m = _message.value ?: return@launch
            withAccount(m) { account, secret ->
                val r = repo.moveMessage(account, secret, m.id, targetFolder)
                if (r.isSuccess) {
                    _notice.value = "Moved to $targetFolder"
                    _message.value = m.copy(folder = targetFolder, id = "${m.accountId}:$targetFolder:${m.uid}")
                } else {
                    _notice.value = "Couldn't move: ${r.exceptionOrNull()?.message ?: "unknown error"}"
                }
            }
        }
    }

    /** Permanently delete (moves to Trash then expunges on the server). */
    fun delete() {
        viewModelScope.launch {
            val m = _message.value ?: return@launch
            withAccount(m) { account, secret ->
                val r = repo.deleteMessageRemote(account, secret, m.id)
                if (r.isSuccess) {
                    _notice.value = "Message deleted"
                    _message.value = null
                } else {
                    _notice.value = "Couldn't delete: ${r.exceptionOrNull()?.message ?: "unknown error"}"
                }
            }
        }
    }

    /** Snooze locally: hide from the inbox until [until]. */
    fun snooze(until: Long) {
        viewModelScope.launch {
            val m = _message.value ?: return@launch
            app.database.messageDao().setSnoozedUntil(m.id, until)
            _notice.value = "Snoozed"
        }
    }

    /**
     * Download one attachment. [action] decides what happens with the bytes:
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
            val result: Result<ByteArray> = withAccountResult(m) { account, secret ->
                repo.fetchAttachment(repo.accountConfig(account, secret), m.folder, m.uid, index)
            }.fold(
                onSuccess = { it },              // already Result<ByteArray>
                onFailure = { Result.failure(it) },
            )
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

    // ---- helpers ----

    private suspend fun withAccount(
        m: MessageEntity,
        block: suspend (AccountEntity, String) -> Unit,
    ) {
        withAccountResult(m) { a, s -> block(a, s); Result.success(Unit) }
    }

    private suspend fun <T> withAccountResult(
        m: MessageEntity,
        block: suspend (AccountEntity, String) -> T,
    ): Result<T> {
        val account = app.database.accountDao().get(m.accountId)
            ?: return Result.failure(IllegalStateException("Account missing"))
        val secret = runCatching { credentials.get(account.id)?.secret }.getOrNull()
            ?: return Result.failure(IllegalStateException("No stored credential"))
        return runCatching { block(account, secret) }
    }
}

class ThreadViewModelFactory(
    private val app: LiquidMailApp,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ThreadViewModel(app) as T
}
