package com.example.exoemail.core.mail

data class FolderInfo(val name: String, val displayName: String, val unreadCount: Int)

data class MailMessage(
    val uid: Long,
    val messageId: String?,
    val subject: String,
    val sender: String,
    val senderName: String,
    val recipients: String,
    val date: Long,
    val snippet: String,
    val isRead: Boolean,
    val isStarred: Boolean,
    val hasAttachments: Boolean,
)

data class MailBody(
    val text: String,
    val html: String?,
    val attachments: List<AttachmentMeta>,
    val inlineImages: List<InlineImage> = emptyList(),
    /** RFC 5322 References header — prior Message-IDs in this thread (for reply threading). */
    val references: String? = null,
)

/** CID-referenced inline image extracted from a MIME part (cid: URL in HTML). */
data class InlineImage(val contentId: String, val mimeType: String, val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean = other is InlineImage && contentId == other.contentId
    override fun hashCode(): Int = contentId.hashCode()
}

data class AttachmentMeta(val index: Int, val fileName: String, val mimeType: String, val size: Long)

data class OutgoingMail(
    val to: List<String>,
    val cc: List<String> = emptyList(),
    val bcc: List<String> = emptyList(),
    val subject: String,
    val body: String,
    val inReplyTo: String? = null,
    val references: String? = null,
    val isHtml: Boolean = false,
    val attachments: List<OutgoingAttachment> = emptyList(),
)

/** A file attached to an outgoing message (bytes + name + mime). */
data class OutgoingAttachment(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)

interface MailEngine {
    suspend fun testConnection(account: AccountConfig): Result<Unit>
    suspend fun fetchFolders(account: AccountConfig): Result<List<FolderInfo>>
    suspend fun fetchMessages(
        account: AccountConfig,
        folder: String = "INBOX",
        sinceUid: Long = 0,
        limit: Int = 50,
    ): Result<List<MailMessage>>
    suspend fun fetchBody(account: AccountConfig, folder: String, uid: Long): Result<MailBody>

    /** Download a single attachment's bytes by its index within the message's attachment list. */
    suspend fun fetchAttachment(account: AccountConfig, folder: String, uid: Long, index: Int): Result<ByteArray>

    suspend fun send(account: AccountConfig, draft: OutgoingMail): Result<Unit>
    suspend fun markRead(account: AccountConfig, folder: String, uid: Long, read: Boolean): Result<Unit>
    suspend fun setStarred(account: AccountConfig, folder: String, uid: Long, starred: Boolean): Result<Unit>

    /**
     * Move a message to another folder (IMAP MOVE if supported, else COPY+STORE+EXPUNGE).
     * Used for Archive/Move/Spam. Does NOT delete — the message lives on in targetFolder.
     */
    suspend fun moveMessage(account: AccountConfig, folder: String, uid: Long, targetFolder: String): Result<Unit>

    /**
     * Delete a message: move to Trash/deleted folder then expunge, or expunge in place
     * when the folder is already Trash. Permanently removes from the server.
     */
    suspend fun deleteMessage(account: AccountConfig, folder: String, uid: Long): Result<Unit>
}
