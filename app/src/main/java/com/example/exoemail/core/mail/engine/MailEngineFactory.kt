package com.example.exoemail.core.mail.engine

import com.example.exoemail.core.mail.MailEngine

object MailEngineFactory {
    fun create(): MailEngine = ImapSmtpMailEngine()
}
