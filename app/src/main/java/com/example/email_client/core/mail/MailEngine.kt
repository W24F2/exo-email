package com.example.email_client.core.mail

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

data class MailBody(val text: String, val html: String?, val attachments: List<AttachmentMeta>)

data class AttachmentMeta(val index: Int, val fileName: String, val mimeType: String, val size: Long)

data class OutgoingMail(
    val to: List<String>,
    val cc: List<String> = emptyList(),
    val bcc: List<String> = emptyList(),
    val subject: String,
    val body: String,
    val inReplyTo: String? = null,
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
    suspend fun send(account: AccountConfig, draft: OutgoingMail): Result<Unit>
    suspend fun markRead(account: AccountConfig, folder: String, uid: Long, read: Boolean): Result<Unit>
    suspend fun setStarred(account: AccountConfig, folder: String, uid: Long, starred: Boolean): Result<Unit>
}
