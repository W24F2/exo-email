package com.example.email_client.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val email: String,
    val providerId: String,
    val displayName: String,
    val imapHost: String,
    val imapPort: Int,
    val imapSsl: Boolean,
    val smtpHost: String,
    val smtpPort: Int,
    val smtpSsl: Boolean,
    val authType: String,
    val syncEnabled: Boolean = true,
    val lastSyncAt: Long = 0,
    val lastUid: Long = 0,
)

@Entity(tableName = "folders", primaryKeys = ["accountId", "name"])
data class FolderEntity(
    val accountId: String,
    val name: String,
    val displayName: String,
    val unreadCount: Int,
)

@Entity(
    tableName = "messages",
    indices = [Index("accountId"), Index("folder"), Index("date"), Index("threadId"), Index(value = ["accountId", "folder", "uid"], unique = true)],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val folder: String,
    val uid: Long,
    val threadId: String,
    val subject: String,
    val sender: String,
    val senderName: String,
    val recipients: String,
    val date: Long,
    val snippet: String,
    val isRead: Boolean,
    val isStarred: Boolean,
    val hasAttachments: Boolean,
    val isDraft: Boolean = false,
    val bodyPath: String?,
)

@Entity(tableName = "attachments", primaryKeys = ["messageId", "idx"])
data class AttachmentEntity(
    val messageId: String,
    val idx: Int,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val localPath: String?,
)
