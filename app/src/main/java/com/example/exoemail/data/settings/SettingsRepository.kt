package com.example.exoemail.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val ACTIVE_ACCOUNT = stringPreferencesKey("active_account")
    }

    val isOnboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDED] ?: false }
    val activeAccountId: Flow<String?> = context.dataStore.data.map { it[Keys.ACTIVE_ACCOUNT] }

    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDED] = value }
    }

    suspend fun setActiveAccount(id: String) {
        context.dataStore.edit {
            it[Keys.ACTIVE_ACCOUNT] = id
            it[Keys.ONBOARDED] = true
        }
    }
}
