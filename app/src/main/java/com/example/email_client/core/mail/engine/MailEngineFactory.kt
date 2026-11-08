package com.example.email_client.core.mail.engine

import com.example.email_client.core.mail.MailEngine

object MailEngineFactory {
    fun create(): MailEngine = ImapSmtpMailEngine()
}
