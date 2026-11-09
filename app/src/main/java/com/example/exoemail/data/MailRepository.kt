package com.example.exoemail.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.example.exoemail.core.mail.AccountConfig
import com.example.exoemail.core.mail.MailBody
import com.example.exoemail.core.mail.MailEngine
import com.example.exoemail.core.mail.OutgoingMail
import com.example.exoemail.core.mail.ServerSpec
import com.example.exoemail.core.security.CredentialStore
import com.example.exoemail.data.db.AccountEntity
import com.example.exoemail.data.db.AppDatabase
import com.example.exoemail.data.db.FolderEntity
import com.example.exoemail.data.db.MessageEntity

/**
 * Deep seam for everything a screen needs to do with mail.
 *
 * Callers speak in messageId and the account entity they already hold — the
 * module resolves the credential, builds the [AccountConfig], and picks the
 * engine op. Flag/delete/move write Room first (instant UI) then push to the
 * server best-effort. [MailEngine] is an internal seam: ViewModels never hold
 * one, which keeps the account→secret→config plumbing in exactly one place.
 */
class MailRepository(
    private val db: AppDatabase,
    private val engine: MailEngine,
    private val credentials: CredentialStore,
) {
    // ---- read side: paged lists + plain Room queries ----

    fun observeAccounts() = db.accountDao().observeAll()
    fun observeFolders(accountId: String) = db.folderDao().observe(accountId)

    private fun pager(factory: () -> androidx.paging.PagingSource<Int, MessageEntity>) = Pager(
        config = PagingConfig(pageSize = 40, prefetchDistance = 15, enablePlaceholders = false),
        pagingSourceFactory = factory,
    ).flow

    fun inboxPager(accountId: String) = pager { db.messageDao().pagingFolder(accountId, "INBOX") }
    fun folderPager(accountId: String, folder: String) = pager { db.messageDao().pagingFolder(accountId, folder) }
    fun searchAll(accountId: String, query: String) = pager { db.messageDao().searchAll(accountId, query) }
    fun starredPager(accountId: String) = pager { db.messageDao().pagingStarred(accountId) }
    fun sentPager(accountId: String) = pager { db.messageDao().pagingSent(accountId) }
    fun draftsPager(accountId: String) = pager { db.messageDao().pagingDrafts(accountId) }
    fun unreadPager(accountId: String) = pager { db.messageDao().pagingUnread(accountId) }

    suspend fun account(id: String) = db.accountDao().get(id)
    suspend fun firstAccount() = db.accountDao().first()
    suspend fun message(id: String) = db.messageDao().get(id)
    suspend fun conversation(accountId: String, threadId: String) = db.messageDao().conversation(accountId, threadId)
    suspend fun addAccount(account: AccountEntity) = db.accountDao().upsert(account)
    suspend fun setReadLocal(id: String, read: Boolean) = db.messageDao().setRead(id, read)
    suspend fun setSnoozedUntil(id: String, until: Long?) = db.messageDao().setSnoozedUntil(id, until)

    suspend fun saveDraft(accountId: String, to: String, subject: String, body: String) {
        val uid = -System.currentTimeMillis()
        val id = "${accountId}:Drafts:$uid"
        db.messageDao().upsertAll(listOf(
            MessageEntity(
                id = id, accountId = accountId, folder = "Drafts", uid = uid,
                threadId = "", subject = subject, sender = "",
                senderName = "Draft", recipients = to, date = System.currentTimeMillis(),
                snippet = body.take(140), isRead = true, isStarred = false,
                hasAttachments = false, isDraft = true, bodyPath = null,
                body = body,
            )
        ))
    }

    suspend fun updateDraft(id: String, to: String, subject: String, body: String) {
        val existing = db.messageDao().get(id) ?: return
        db.messageDao().upsertAll(listOf(existing.copy(
            recipients = to, subject = subject, body = body,
            snippet = body.take(140), date = System.currentTimeMillis(),
        )))
    }

    suspend fun deleteMessage(id: String) = db.messageDao().delete(id)

    // ---- deep ops: callers pass what they already hold, module does the rest ----

    /** Fetch a message body (lazy, on open). No-op when there's no credential. */
    suspend fun body(messageId: String): Result<MailBody> =
        withRemote(messageId) { cfg, m -> engine.fetchBody(cfg, m.folder, m.uid) }

    /** Download a single attachment's bytes. */
    suspend fun attachment(messageId: String, index: Int): Result<ByteArray> =
        withRemote(messageId) { cfg, m -> engine.fetchAttachment(cfg, m.folder, m.uid, index) }

    /** Mark read locally, then push the flag to the server best-effort. */
    suspend fun markRead(messageId: String, read: Boolean): Result<Unit> {
        db.messageDao().setRead(messageId, read)
        return withRemote(messageId) { cfg, m -> engine.markRead(cfg, m.folder, m.uid, read) }
            .remoteOrSuccess()
    }

    /** Star locally, then push the flag to the server best-effort. */
    suspend fun setStarred(messageId: String, starred: Boolean): Result<Unit> {
        db.messageDao().setStarred(messageId, starred)
        return withRemote(messageId) { cfg, m -> engine.setStarred(cfg, m.folder, m.uid, starred) }
            .remoteOrSuccess()
    }

    /**
     * Move to another folder: optimistic Room rewrite (id embeds account:folder:uid),
     * attachment re-key, then best-effort IMAP move. Server failure converges on sync.
     */
    suspend fun move(messageId: String, targetFolder: String): Result<Unit> {
        val m = db.messageDao().get(messageId) ?: return Result.failure(IllegalArgumentException("not found"))
        val newId = "${m.accountId}:${targetFolder}:${m.uid}"
        db.messageDao().delete(messageId)
        db.messageDao().upsertAll(listOf(m.copy(id = newId, folder = targetFolder)))
        val atts = db.attachmentDao().forMessage(messageId)
        if (atts.isNotEmpty()) {
            db.attachmentDao().deleteForMessage(messageId)
            db.attachmentDao().upsertAll(atts.map { it.copy(messageId = newId) })
        }
        return withRemote(newId) { cfg, mm -> engine.moveMessage(cfg, mm.folder, mm.uid, targetFolder) }
            .remoteOrSuccess()
    }

    /** Delete: Room + attachments first, then best-effort server delete. */
    suspend fun delete(messageId: String): Result<Unit> {
        val m = db.messageDao().get(messageId) ?: return Result.failure(IllegalArgumentException("not found"))
        db.messageDao().delete(messageId)
        db.attachmentDao().deleteForMessage(messageId)
        return withRemote(messageId) { cfg, mm -> engine.deleteMessage(cfg, mm.folder, mm.uid) }
            .remoteOrSuccess()
    }

    /** Send a draft from [account]. Caller supplies the account it already resolved.
     *  Records the sent copy in Room (Sent folder) on success. */
    suspend fun send(account: AccountEntity, draft: OutgoingMail): Result<Unit> = runCatching {
        engine.send(requireConfig(account), draft).getOrThrow()
        // Record the sent copy locally so Sent tab shows it immediately.
        val sentId = "${account.id}:Sent:${System.currentTimeMillis()}"
        db.messageDao().upsertAll(listOf(
            MessageEntity(
                id = sentId, accountId = account.id, folder = "Sent", uid = 0,
                threadId = draft.references ?: draft.inReplyTo ?: "",
                subject = draft.subject, sender = account.email,
                senderName = account.displayName, recipients = draft.to.joinToString(", "),
                date = System.currentTimeMillis(), snippet = draft.body.take(140),
                isRead = true, isStarred = false, hasAttachments = draft.attachments.isNotEmpty(),
                isDraft = false, bodyPath = null, body = draft.body,
            )
        ))
    }

    /** Sync INBOX envelopes since the last known UID and upsert into Room. */
    suspend fun refreshInbox(account: AccountEntity): Result<Int> =
        kotlinx.coroutines.withTimeoutOrNull(60_000) { refreshInboxInner(account) }
            ?: Result.failure(java.util.concurrent.TimeoutException("IMAP sync timed out"))

    private suspend fun refreshInboxInner(account: AccountEntity): Result<Int> = runCatching {
        val config = requireConfig(account)
        val sinceUid = db.messageDao().maxUid(account.id) ?: 0L
        val fetched = engine.fetchMessages(config, "INBOX", sinceUid = sinceUid, limit = 50)
            .getOrThrow()
        val entities = fetched.map { m ->
            // threadId = root of References chain (first Message-ID), else own Message-ID
            val threadRoot = m.references?.split(" ")?.firstOrNull() ?: m.messageId ?: "${account.id}:${m.uid}"
            MessageEntity(
                id = "${account.id}:INBOX:${m.uid}",
                accountId = account.id,
                folder = "INBOX",
                uid = m.uid,
                threadId = threadRoot,
                subject = m.subject,
                sender = m.sender,
                senderName = m.senderName,
                recipients = m.recipients,
                date = m.date,
                snippet = m.snippet,
                isRead = m.isRead,
                isStarred = m.isStarred,
                hasAttachments = m.hasAttachments,
                bodyPath = null,
            )
        }
        db.messageDao().upsertAll(entities)
        val folders = engine.fetchFolders(config).getOrDefault(emptyList())
        db.folderDao().upsertAll(folders.map {
            FolderEntity(account.id, it.name, it.displayName, it.unreadCount)
        })
        val maxUid = db.messageDao().maxUid(account.id) ?: sinceUid
        db.accountDao().updateSyncState(account.id, System.currentTimeMillis(), maxUid)
        entities.size
    }

    // ---- internal: the account→secret→config plumbing, exactly once ----

    /**
     * Resolve [messageId] to (config, message) and run the engine op. Returns a
     * failure when the message/account/credential is gone or the message has no
     * server counterpart (uid == 0) — callers treat all of these as "can't sync".
     */
    private suspend fun <T> withRemote(
        messageId: String,
        op: suspend (AccountConfig, MessageEntity) -> Result<T>,
    ): Result<T> {
        val m = db.messageDao().get(messageId)
            ?: return Result.failure(IllegalArgumentException("Message not found: $messageId"))
        if (m.uid <= 0L) return Result.failure(NoServerCopy())
        val account = db.accountDao().get(m.accountId)
            ?: return Result.failure(IllegalStateException("Account missing"))
        val cfg = accountConfig(account)
            ?: return Result.failure(NoCredential())
        return op(cfg, m)
    }

    /** A local-only row (draft / sent placeholder) — no server message to act on. */
    class NoServerCopy : Exception("message has no server counterpart")
    class NoCredential : Exception("no stored credential for account")

    /**
     * Best-effort flag/move/delete semantics: a local-only message or a missing
     * credential isn't a user-visible failure — the Room write already happened.
     */
    private fun <T> Result<T>.remoteOrSuccess(): Result<T> =
        if (isSuccess || exceptionOrNull() is NoServerCopy || exceptionOrNull() is NoCredential) {
            @Suppress("UNCHECKED_CAST")
            (if (isSuccess) this else Result.success(Unit) as Result<T>)
        } else this

    /** Build the engine config, or null when the account has no stored credential. */
    private fun accountConfig(account: AccountEntity): AccountConfig? {
        val secret = credentials.get(account.id)?.secret ?: return null
        return AccountConfig(
            accountId = account.id,
            email = account.email,
            username = account.email,
            password = secret,
            imap = ServerSpec(account.imapHost, account.imapPort, account.imapSsl),
            smtp = ServerSpec(account.smtpHost, account.smtpPort, account.smtpSsl),
        )
    }

    /**
     * Send + refresh paths always need a real credential; missing means a hard
     * failure with a clear message, not a silent no-op.
     */
    private fun requireConfig(account: AccountEntity): AccountConfig =
        accountConfig(account) ?: throw NoCredential()
}
