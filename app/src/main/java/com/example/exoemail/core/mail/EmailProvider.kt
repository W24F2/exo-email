package com.example.exoemail.core.mail

enum class AuthType { PASSWORD, APP_PASSWORD }

data class ServerSpec(val host: String, val port: Int, val ssl: Boolean)

data class EmailProvider(
    val id: String,
    val displayName: String,
    val domainHints: List<String>,
    val imap: ServerSpec,
    val smtp: ServerSpec,
    val auth: AuthType,
    val helpUrl: String?,
    val passwordHint: String?,
)

object ProviderCatalog {
    val all: List<EmailProvider> = listOf(
        EmailProvider(
            "gmail", "Gmail", listOf("gmail.com", "googlemail.com"),
            ServerSpec("imap.gmail.com", 993, true), ServerSpec("smtp.gmail.com", 465, true),
            AuthType.APP_PASSWORD, "https://support.google.com/accounts/answer/185833",
            "Use a Google App Password (requires 2-Step Verification).",
        ),
        EmailProvider(
            "qq", "QQ Mail", listOf("qq.com", "foxmail.com"),
            ServerSpec("imap.qq.com", 993, true), ServerSpec("smtp.qq.com", 465, true),
            AuthType.APP_PASSWORD, "https://service.mail.qq.com/detail/0/75",
            "Use your QQ Mail authorization code (æŽˆæƒç ), not your QQ password.",
        ),
        EmailProvider(
            "outlook", "Outlook", listOf("outlook.com", "hotmail.com", "live.com", "office365.com"),
            ServerSpec("outlook.office365.com", 993, true), ServerSpec("smtp.office365.com", 587, false),
            AuthType.PASSWORD, null, null,
        ),
        EmailProvider(
            "icloud", "iCloud", listOf("icloud.com", "me.com", "mac.com"),
            ServerSpec("imap.mail.me.com", 993, true), ServerSpec("smtp.mail.me.com", 587, false),
            AuthType.APP_PASSWORD, "https://support.apple.com/102654",
            "Use an Apple app-specific password.",
        ),
        EmailProvider(
            "yahoo", "Yahoo", listOf("yahoo.com", "ymail.com", "rocketmail.com"),
            ServerSpec("imap.mail.yahoo.com", 993, true), ServerSpec("smtp.mail.yahoo.com", 465, true),
            AuthType.APP_PASSWORD, null,
            "Use a Yahoo app password.",
        ),
        EmailProvider(
            "netease163", "163 Mail", listOf("163.com", "126.com", "yeah.net"),
            ServerSpec("imap.163.com", 993, true), ServerSpec("smtp.163.com", 465, true),
            AuthType.APP_PASSWORD, null,
            "Use your 163 Mail authorization code.",
        ),
        EmailProvider(
            "custom", "Other (IMAP/SMTP)", emptyList(),
            ServerSpec("", 993, true), ServerSpec("", 465, true),
            AuthType.PASSWORD, null, null,
        ),
    )

    fun detect(email: String): EmailProvider? {
        val domain = email.substringAfterLast('@', "").lowercase()
        if (domain.isEmpty()) return null
        return all.firstOrNull { p -> domain in p.domainHints }
    }

    fun byId(id: String): EmailProvider? = all.firstOrNull { it.id == id }
}
