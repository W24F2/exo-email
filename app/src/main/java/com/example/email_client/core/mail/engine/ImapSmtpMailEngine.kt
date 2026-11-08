package com.example.email_client.core.mail.engine

import com.example.email_client.core.mail.AccountConfig
import com.example.email_client.core.mail.AttachmentMeta
import com.example.email_client.core.mail.FolderInfo
import com.example.email_client.core.mail.InlineImage
import com.example.email_client.core.mail.MailBody
import com.example.email_client.core.mail.MailEngine
import com.example.email_client.core.mail.MailMessage
import com.example.email_client.core.mail.OutgoingMail
import jakarta.mail.Flags
import jakarta.mail.FetchProfile
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeUtility
import jakarta.mail.search.FlagTerm
import jakarta.mail.search.MessageIDTerm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImapSmtpMailEngine : MailEngine {

    private fun <T> store(account: AccountConfig, block: (Store) -> T): T {
        val session = Session.getInstance(account.toImapProperties())
        val store = session.getStore("imap")
        store.connect(account.imap.host, account.imap.port, account.username, account.password)
        try {
            return block(store)
        } finally {
            runCatching { store.close() }
        }
    }

    private fun <T> withFolder(
        account: AccountConfig,
        folderName: String,
        mode: Int,
        block: (Store, Folder) -> T,
    ): T = store(account) { s ->
        val folder = s.getFolder(folderName)
        if (!folder.exists()) throw MessagingException("Folder not found: $folderName")
        folder.open(mode)
        try {
            block(s, folder)
        } finally {
            runCatching { folder.close(false) }
        }
    }

    override suspend fun testConnection(account: AccountConfig): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                store(account) { /* connect+close is the test */ }
                val transport = Session.getInstance(account.toSmtpProperties()).getTransport("smtp")
                transport.connect(account.smtp.host, account.smtp.port, account.username, account.password)
                transport.close()
            }
        }

    override suspend fun fetchFolders(account: AccountConfig): Result<List<FolderInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                store(account) { s ->
                    s.defaultFolder.list("*").map { f ->
                        FolderInfo(
                            name = f.fullName,
                            displayName = f.name,
                            unreadCount = runCatching { f.unreadMessageCount }.getOrDefault(0),
                        )
                    }
                }
            }
        }

    override suspend fun fetchMessages(
        account: AccountConfig,
        folder: String,
        sinceUid: Long,
        limit: Int,
    ): Result<List<MailMessage>> = withContext(Dispatchers.IO) {
        runCatching {
            withFolder(account, folder, Folder.READ_ONLY) { _, f ->
                val uids = if (sinceUid > 0) {
                    (f as? jakarta.mail.UIDFolder)
                        ?.getMessagesByUID(sinceUid + 1, jakarta.mail.UIDFolder.LASTUID)
                        ?.toList() ?: emptyList()
                } else {
                    val count = f.messageCount
                    if (count == 0) emptyList()
                    else f.getMessages(maxOf(1, count - limit + 1), count).toList()
                }
                val batch = uids.takeLast(limit)
                // Envelope-only prefetch: headers, flags, sizes. NEVER bodies —
                // downloading bodies here is what timed out big Gmail inboxes.
                if (batch.isNotEmpty()) {
                    val fp = FetchProfile().apply {
                        add(FetchProfile.Item.ENVELOPE)
                        add(FetchProfile.Item.FLAGS)
                        add(FetchProfile.Item.CONTENT_INFO)
                    }
                    f.fetch(batch.toTypedArray(), fp)
                }
                batch.map { it.toMailMessage(f as? jakarta.mail.UIDFolder) }
                    .sortedByDescending { it.date }
            }
        }
    }

    override suspend fun fetchBody(account: AccountConfig, folder: String, uid: Long): Result<MailBody> =
        withContext(Dispatchers.IO) {
            runCatching {
                withFolder(account, folder, Folder.READ_ONLY) { _, f ->
                    val uidFolder = f as? jakarta.mail.UIDFolder
                        ?: throw MessagingException("UIDFolder not supported")
                    val msg = uidFolder.getMessageByUID(uid)
                        ?: throw MessagingException("Message $uid not found")
                    msg.extractBody()
                }
            }
        }

    override suspend fun send(account: AccountConfig, draft: OutgoingMail): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val session = Session.getInstance(account.toSmtpProperties())
                val msg = MimeMessage(session).apply {
                    setFrom(InternetAddress(account.email))
                    setRecipients(Message.RecipientType.TO, draft.to.map { InternetAddress(it) }.toTypedArray())
                    if (draft.cc.isNotEmpty()) {
                        setRecipients(Message.RecipientType.CC, draft.cc.map { InternetAddress(it) }.toTypedArray())
                    }
                    if (draft.bcc.isNotEmpty()) {
                        setRecipients(Message.RecipientType.BCC, draft.bcc.map { InternetAddress(it) }.toTypedArray())
                    }
                    subject = draft.subject
                    setText(draft.body, "utf-8")
                    draft.inReplyTo?.let { setHeader("In-Reply-To", it) }
                    sentDate = java.util.Date()
                }
                val transport = session.getTransport("smtp")
                transport.connect(account.smtp.host, account.smtp.port, account.username, account.password)
                try {
                    transport.sendMessage(msg, msg.allRecipients)
                } finally {
                    transport.close()
                }
            }
        }

    override suspend fun markRead(account: AccountConfig, folder: String, uid: Long, read: Boolean): Result<Unit> =
        setFlag(account, folder, uid, Flags.Flag.SEEN, read)

    override suspend fun setStarred(account: AccountConfig, folder: String, uid: Long, starred: Boolean): Result<Unit> =
        setFlag(account, folder, uid, Flags.Flag.FLAGGED, starred)

    private suspend fun setFlag(
        account: AccountConfig, folder: String, uid: Long, flag: Flags.Flag, value: Boolean,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            withFolder(account, folder, Folder.READ_WRITE) { _, f ->
                val msg = (f as? jakarta.mail.UIDFolder)?.getMessageByUID(uid)
                    ?: throw MessagingException("Message $uid not found")
                msg.setFlag(flag, value)
            }
        }
    }

    private fun Message.toMailMessage(uidFolder: jakarta.mail.UIDFolder?): MailMessage {
        val from = (from?.firstOrNull() as? InternetAddress)
        return MailMessage(
            uid = uidFolder?.getUID(this) ?: 0L,
            messageId = runCatching { (this as? MimeMessage)?.messageID }.getOrNull(),
            subject = subject ?: "(no subject)",
            sender = from?.address ?: "",
            senderName = from?.personal ?: from?.address ?: "",
            recipients = getRecipients(Message.RecipientType.TO)
                ?.joinToString(", ") { (it as? InternetAddress)?.address ?: it.toString() } ?: "",
            date = (sentDate ?: receivedDate)?.time ?: System.currentTimeMillis(),
            snippet = runCatching { description ?: subject ?: "" }.getOrDefault("").take(140),
            isRead = flags.contains(Flags.Flag.SEEN),
            isStarred = flags.contains(Flags.Flag.FLAGGED),
            hasAttachments = hasAttachmentsFromHeaders(),
        )
    }

    /**
     * Header-based attachment check (Content-Type is prefetched with CONTENT_INFO).
     * multipart/mixed almost always carries an attachment; multipart/alternative
     * is just text+html variants. Avoids the old hasAttachments() which touched
     * part.content and forced a full MIME body download per message during sync.
     */
    private fun Message.hasAttachmentsFromHeaders(): Boolean = runCatching {
        contentType?.contains("multipart/mixed", true) == true ||
            contentType?.contains("name=", true) == true
    }.getOrDefault(false)

    private fun Message.extractBody(): MailBody {
        val text = StringBuilder()
        val html = StringBuilder()
        val attachments = mutableListOf<AttachmentMeta>()
        val inlineImages = mutableListOf<InlineImage>()
        var index = 0

        fun walk(part: Part) {
            val disp = part.disposition
            val fileName = runCatching { part.fileName }.getOrNull()
            val isAttachment = disp != null &&
                (disp.equals(Part.ATTACHMENT, true) ||
                    (disp.equals(Part.INLINE, true) && !fileName.isNullOrEmpty()))

            if (isAttachment) {
                attachments += AttachmentMeta(
                    index = index++,
                    fileName = runCatching { MimeUtility.decodeText(fileName ?: "attachment") }.getOrDefault(fileName ?: "attachment"),
                    mimeType = part.contentType?.substringBefore(';')?.trim() ?: "application/octet-stream",
                    size = part.size.toLong().coerceAtLeast(0),
                )
                // Inline images referenced via cid: in HTML are captured too.
                if (part.isMimeType("image/*")) {
                    val cid = runCatching {
                        part.getHeader("Content-ID")?.firstOrNull()?.trim('<', '>')
                    }.getOrNull()
                    val bytes = runCatching {
                        (part.content as? java.io.InputStream)?.readBytes()
                    }.getOrNull()
                    if (cid != null && bytes != null && bytes.isNotEmpty()) {
                        inlineImages += InlineImage(cid, part.contentType?.substringBefore(';')?.trim() ?: "image/png", bytes)
                    }
                }
                return
            }
            when {
                part.isMimeType("text/plain") -> text.append(part.content?.toString() ?: "")
                part.isMimeType("text/html") -> html.append(part.content?.toString() ?: "")
                part.isMimeType("multipart/*") -> {
                    val mp = part.content as? Multipart ?: return
                    for (i in 0 until mp.count) walk(mp.getBodyPart(i))
                }
                part.isMimeType("message/rfc822") -> (part.content as? Part)?.let(::walk)
                // Non-attachment image parts without disposition (inline in related multipart)
                part.isMimeType("image/*") && !fileName.isNullOrEmpty() -> {
                    val cid = runCatching {
                        part.getHeader("Content-ID")?.firstOrNull()?.trim('<', '>')
                    }.getOrNull() ?: fileName.trim('<', '>')
                    val bytes = runCatching { (part.content as? java.io.InputStream)?.readBytes() }.getOrNull()
                    if (bytes != null && bytes.isNotEmpty()) {
                        inlineImages += InlineImage(cid, part.contentType?.substringBefore(';')?.trim() ?: "image/png", bytes)
                    }
                }
            }
        }
        walk(this)
        var htmlOut = html.toString()
        // Rewrite cid: references to a custom scheme the WebView intercepts.
        inlineImages.forEach { img ->
            htmlOut = htmlOut.replace("cid:${img.contentId}", "liquidimg://${img.contentId}")
        }
        return MailBody(
            text = text.toString().ifBlank { htmlOut.replace(Regex("<[^>]+>"), " ") },
            html = htmlOut.ifBlank { null },
            attachments = attachments,
            inlineImages = inlineImages,
        )
    }
}
