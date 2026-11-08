package com.example.email_client.data

import android.content.Context
import com.example.email_client.data.db.AccountEntity
import com.example.email_client.data.db.AppDatabase
import com.example.email_client.data.db.FolderEntity
import com.example.email_client.data.db.MessageEntity
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * Debug-only demo data seeder. Inserts a fake account + sample messages so every
 * screen is reachable without real IMAP credentials. Never runs in release builds
 * and never enables sync for the demo account.
 */
object DemoSeeder {

    suspend fun seedIfEmpty(context: Context, db: AppDatabase) {
        val existing = db.accountDao().observeAll().first()
        if (existing.isNotEmpty()) return

        val accountId = "demo-account"
        db.accountDao().upsert(
            AccountEntity(
                id = accountId,
                email = "demo@liquidmail.app",
                providerId = "custom",
                displayName = "Demo",
                imapHost = "", imapPort = 993, imapSsl = true,
                smtpHost = "", smtpPort = 465, smtpSsl = true,
                authType = "PASSWORD",
                syncEnabled = false, // never hit the network
                lastSyncAt = System.currentTimeMillis(),
                lastUid = 0,
            )
        )

        db.folderDao().upsertAll(
            listOf(
                FolderEntity(accountId, "INBOX", "Inbox", 3),
                FolderEntity(accountId, "Sent", "Sent", 0),
                FolderEntity(accountId, "Drafts", "Drafts", 0),
                FolderEntity(accountId, "Archive", "Archive", 0),
            )
        )

        val now = System.currentTimeMillis()
        data class Sample(val sender: String, val subject: String, val snippet: String, val date: Long)
        val samples = listOf(
            Sample("Alice Chen", "Welcome to Liquid Mail", "Thanks for trying the new build — here's what's changed in this release.", now - 3600_000),
            Sample("GitHub", "[liquidmail] PR #42: fix sync race", "Review requested on the folder-sync refactor.", now - 7200_000),
            Sample("Mom", "Dinner on Sunday?", "Are you still coming over this weekend? Let me know.", now - 86_400_000),
            Sample("Netflix", "New arrivals this week", "Top picks for you based on your watch history.", now - 172_800_000),
            Sample("Bank", "Your monthly statement", "Your statement for November is now available.", now - 259_200_000),
        )
        db.messageDao().upsertAll(
            samples.mapIndexed { i, s ->
                MessageEntity(
                    id = "$accountId:INBOX:${1000 + i}",
                    accountId = accountId,
                    folder = "INBOX",
                    uid = (1000 + i).toLong(),
                    threadId = "thread-$i",
                    subject = s.subject,
                    sender = s.sender.lowercase().replace(" ", ".") + "@example.com",
                    senderName = s.sender,
                    recipients = "demo@liquidmail.app",
                    date = s.date,
                    snippet = s.snippet,
                    isRead = i > 1,
                    isStarred = i == 0,
                    hasAttachments = i == 2,
                    isDraft = false,
                    bodyPath = null,
                    body = s.snippet,
                )
            }
        )

        // One draft so the Drafts tab isn't empty
        db.messageDao().upsertAll(
            listOf(
                MessageEntity(
                    id = "$accountId:Drafts:${now - 1800_000}",
                    accountId = accountId,
                    folder = "Drafts",
                    uid = 0,
                    threadId = "",
                    subject = "Re: Dinner on Sunday?",
                    sender = "",
                    senderName = "Draft",
                    recipients = "mom@example.com",
                    date = now - 1800_000,
                    snippet = "Yes, I'll be there! Should I bring anything?",
                    isRead = true,
                    isStarred = false,
                    hasAttachments = false,
                    isDraft = true,
                    bodyPath = null,
                    body = "Yes, I'll be there! Should I bring anything?",
                )
            )
        )

        // One sent item
        db.messageDao().upsertAll(
            listOf(
                MessageEntity(
                    id = "$accountId:Sent:${now - 900_000}",
                    accountId = accountId,
                    folder = "Sent",
                    uid = 0,
                    threadId = "",
                    subject = "Project update",
                    sender = "demo@liquidmail.app",
                    senderName = "Demo",
                    recipients = "team@example.com",
                    date = now - 900_000,
                    snippet = "Here's the latest status on the release.",
                    isRead = true,
                    isStarred = false,
                    hasAttachments = false,
                    isDraft = false,
                    bodyPath = null,
                    body = "Here's the latest status on the release.",
                )
            )
        )
    }
}
