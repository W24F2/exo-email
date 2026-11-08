package com.example.exoemail.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.example.exoemail.core.mail.AccountConfig
import com.example.exoemail.core.mail.MailEngine
import com.example.exoemail.core.mail.ServerSpec
import com.example.exoemail.data.db.AccountEntity
import com.example.exoemail.data.db.AppDatabase
import com.example.exoemail.data.db.FolderEntity
import com.example.exoemail.data.db.MessageEntity

class MailRepository(
    private val db: AppDatabase,
    private val engine: MailEngine,
) {
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
    suspend fun message(id: String) = db.messageDao().get(id)
    suspend fun addAccount(account: AccountEntity) = db.accountDao().upsert(account)

    suspend fun markRead(id: String, read: Boolean) = db.messageDao().setRead(id, read)
    suspend fun setStarred(id: String, starred: Boolean) = db.messageDao().setStarred(id, starred)

    suspend fun saveDraft(accountId: String, to: String, subject: String, body: String) {
        val id = "${accountId}:Drafts:${System.currentTimeMillis()}"
        db.messageDao().upsertAll(listOf(
            MessageEntity(
                id = id, accountId = accountId, folder = "Drafts", uid = 0,
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

    // ---- rich email features ----

    /** Fetch a message body (lazy, on open). */
    suspend fun fetchBody(config: AccountConfig, folder: String, uid: Long) =
        engine.fetchBody(config, folder, uid)

    /** Download a single attachment's bytes. */
    suspend fun fetchAttachment(config: AccountConfig, folder: String, uid: Long, index: Int) =
        engine.fetchAttachment(config, folder, uid, index)

    /**
     * Server-side move + local row update. Optimistic: update Room first, then
     * best-effort IMAP move. Returns the engine Result for caller to surface errors.
     */
    suspend fun moveMessage(account: AccountEntity, password: String, messageId: String, targetFolder: String): Result<Unit> {
        val m = db.messageDao().get(messageId) ?: return Result.failure(IllegalArgumentException("not found"))
        // local: rewrite id (it embeds accountId:folder:uid) and set folder
        val newId = "${m.accountId}:${targetFolder}:${m.uid}"
        db.messageDao().delete(messageId)
        db.messageDao().upsertAll(listOf(m.copy(id = newId, folder = targetFolder)))
        // Re-key attachments so they still resolve under the new message id.
        val atts = db.attachmentDao().forMessage(messageId)
        if (atts.isNotEmpty()) {
            db.attachmentDao().deleteForMessage(messageId)
            db.attachmentDao().upsertAll(atts.map { it.copy(messageId = newId) })
        }
        return if (m.uid > 0) {
            engine.moveMessage(accountConfig(account, password), m.folder, m.uid, targetFolder)
        } else Result.success(Unit)
    }

    /**
     * Server-side delete (to Trash + expunge) + local remove. Optimistic local
     * delete first; engine call is best-effort.
     */
    suspend fun deleteMessageRemote(account: AccountEntity, password: String, messageId: String): Result<Unit> {
        val m = db.messageDao().get(messageId) ?: return Result.failure(IllegalArgumentException("not found"))
        db.messageDao().delete(messageId)
        db.attachmentDao().deleteForMessage(messageId)
        return if (m.uid > 0) {
            engine.deleteMessage(accountConfig(account, password), m.folder, m.uid)
        } else Result.success(Unit)
    }

    fun accountConfig(account: AccountEntity, password: String) = AccountConfig(
        accountId = account.id,
        email = account.email,
        username = account.email,
        password = password,
        imap = ServerSpec(account.imapHost, account.imapPort, account.imapSsl),
        smtp = ServerSpec(account.smtpHost, account.smtpPort, account.smtpSsl),
    )

    /** Sync INBOX envelopes since the last known UID and upsert into Room. */
    suspend fun refreshInbox(account: AccountEntity, password: String): Result<Int> =
        kotlinx.coroutines.withTimeoutOrNull(60_000) { refreshInboxInner(account, password) }
            ?: Result.failure(java.util.concurrent.TimeoutException("IMAP sync timed out"))

    private suspend fun refreshInboxInner(account: AccountEntity, password: String): Result<Int> = runCatching {
        val config = accountConfig(account, password)
        val sinceUid = db.messageDao().maxUid(account.id) ?: 0L
        val fetched = engine.fetchMessages(config, "INBOX", sinceUid = sinceUid, limit = 50)
            .getOrThrow()
        val entities = fetched.map { m ->
            MessageEntity(
                id = "${account.id}:INBOX:${m.uid}",
                accountId = account.id,
                folder = "INBOX",
                uid = m.uid,
                threadId = m.messageId ?: "${account.id}:${m.uid}",
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
}
