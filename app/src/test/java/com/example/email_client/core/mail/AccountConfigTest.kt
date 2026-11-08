package com.example.email_client.core.mail

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountConfigTest {

    private fun config(imapSsl: Boolean, smtpSsl: Boolean) = AccountConfig(
        accountId = "id", email = "a@b.com", username = "a@b.com", password = "pw",
        imap = ServerSpec("imap.test", 993, imapSsl),
        smtp = ServerSpec("smtp.test", if (smtpSsl) 465 else 587, smtpSsl),
    )

    @Test fun sslImapProps() {
        val p = config(imapSsl = true, smtpSsl = true).toImapProperties()
        assertEquals("imap.test", p.getProperty("mail.imap.host"))
        assertEquals("993", p.getProperty("mail.imap.port"))
        assertEquals("true", p.getProperty("mail.imap.ssl.enable"))
    }

    @Test fun starttlsImapProps() {
        val p = config(imapSsl = false, smtpSsl = false).toImapProperties()
        assertEquals("true", p.getProperty("mail.imap.starttls.enable"))
    }

    @Test fun sslSmtpProps() {
        val p = config(imapSsl = true, smtpSsl = true).toSmtpProperties()
        assertEquals("smtp.test", p.getProperty("mail.smtp.host"))
        assertEquals("465", p.getProperty("mail.smtp.port"))
        assertEquals("true", p.getProperty("mail.smtp.ssl.enable"))
        assertEquals("true", p.getProperty("mail.smtp.auth"))
    }

    @Test fun starttlsSmtpProps() {
        val p = config(imapSsl = true, smtpSsl = false).toSmtpProperties()
        assertEquals("587", p.getProperty("mail.smtp.port"))
        assertEquals("true", p.getProperty("mail.smtp.starttls.enable"))
        assertEquals("true", p.getProperty("mail.smtp.starttls.required"))
    }
}
