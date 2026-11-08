package com.example.email_client.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class Credentials(val username: String, val secret: String, val authType: String)

/** Keystore-backed credential storage. Falls back to throwing on get() if Keystore unavailable. */
class CredentialStore(context: Context) {

    private val prefs by lazy {
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "mail_credentials", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun save(accountId: String, credentials: Credentials) {
        prefs.edit()
            .putString("$accountId.user", credentials.username)
            .putString("$accountId.secret", credentials.secret)
            .putString("$accountId.auth", credentials.authType)
            .apply()
    }

    fun get(accountId: String): Credentials? {
        val u = prefs.getString("$accountId.user", null) ?: return null
        val s = prefs.getString("$accountId.secret", null) ?: return null
        val a = prefs.getString("$accountId.auth", "PASSWORD") ?: "PASSWORD"
        return Credentials(u, s, a)
    }

    fun delete(accountId: String) {
        prefs.edit()
            .remove("$accountId.user").remove("$accountId.secret").remove("$accountId.auth")
            .apply()
    }
}
