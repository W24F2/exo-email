package com.example.exoemail.core.mail

import java.util.Properties

/** Resolved server credentials + endpoints for one account. */
data class AccountConfig(
    val accountId: String,
    val email: String,
    val username: String,
    val password: String,
    val imap: ServerSpec,
    val smtp: ServerSpec,
) {
    fun toImapProperties(): Properties = Properties().apply {
        put("mail.store.protocol", "imap")
        put("mail.imap.host", imap.host)
        put("mail.imap.port", imap.port.toString())
        put("mail.imap.ssl.enable", imap.ssl.toString())
        if (!imap.ssl) put("mail.imap.starttls.enable", "true")
        put("mail.imap.connectiontimeout", "15000")
        put("mail.imap.timeout", "15000")
        put("mail.imap.writetimeout", "15000")
        put("mail.imap.connectionpoolsize", "1")
        // partial fetch keeps envelope sync cheap
        put("mail.imap.fetchsize", "65536")
        put("mail.imap.partialfetch", "true")
    }

    fun toSmtpProperties(): Properties = Properties().apply {
        put("mail.transport.protocol", "smtp")
        put("mail.smtp.host", smtp.host)
        put("mail.smtp.port", smtp.port.toString())
        put("mail.smtp.auth", "true")
        if (smtp.ssl) {
            put("mail.smtp.ssl.enable", "true")
        } else {
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.starttls.required", "true")
        }
        put("mail.smtp.connectiontimeout", "15000")
        put("mail.smtp.timeout", "15000")
        put("mail.smtp.writetimeout", "15000")
    }
}
