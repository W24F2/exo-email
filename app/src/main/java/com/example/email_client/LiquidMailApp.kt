package com.example.email_client

import android.app.Application
import android.os.StrictMode
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.email_client.data.db.AppDatabase
import com.example.email_client.data.settings.SettingsRepository
import com.example.email_client.sync.MailSyncWorker
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
                com.example.email_client.data.DemoSeeder.seedIfEmpty(this@LiquidMailApp, database)
            }
        }
        scheduleSync()
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
