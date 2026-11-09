package com.example.exoemail

import android.app.Application
import android.os.StrictMode
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.exoemail.data.db.AppDatabase
import com.example.exoemail.data.settings.SettingsRepository
import com.example.exoemail.sync.MailSyncWorker
import java.util.concurrent.TimeUnit

class LiquidMailApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var settings: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build()
            )
        }
        database = AppDatabase.create(this)
        settings = SettingsRepository(this)
        if (BuildConfig.DEBUG) {
            kotlinx.coroutines.runBlocking {
                com.example.exoemail.data.DemoSeeder.seedIfEmpty(this@LiquidMailApp, database)
            }
        }
        // One-off dedupe from older builds — almost always a no-op, so run it
        // off the startup critical path instead of blocking onCreate. Room's
        // query executor runs DB work on background threads; this just has to
        // not hold up first-frame. A single daemon thread is enough.
        Thread { dedupeAccounts() }.apply { isDaemon = true; start() }
        scheduleSync()
    }

    /**
     * Older builds could insert the same email twice (no dedupe on add), which
     * split synced messages across two account ids â€” half of them pointed at an
     * account with no stored credential, so bodies never loaded. Keep the
     * account that actually has a credential (fall back to most-recently-synced)
     * and drop the orphan's rows; the next sync repopulates under the keeper.
     */
    private fun dedupeAccounts() {
        kotlinx.coroutines.runBlocking {
            val accounts = database.accountDao().all()
            val byEmail = accounts.groupBy { it.email.lowercase() }.filterValues { it.size > 1 }
            if (byEmail.isEmpty()) return@runBlocking
            val credentials = com.example.exoemail.core.security.CredentialStore(this@LiquidMailApp)
            for ((_, group) in byEmail) {
                val keeper = group.firstOrNull { runCatching { credentials.get(it.id) }.getOrNull() != null }
                    ?: group.maxByOrNull { it.lastSyncAt }
                    ?: continue
                for (loser in group.filter { it.id != keeper.id }) {
                    database.messageDao().deleteForAccount(loser.id)
                    database.folderDao().deleteForAccount(loser.id)
                    database.attachmentDao().deleteForAccount(loser.id)
                    database.accountDao().delete(loser.id)
                }
            }
        }
    }

    private fun scheduleSync() {
        val request = PeriodicWorkRequestBuilder<MailSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("mail_sync", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
