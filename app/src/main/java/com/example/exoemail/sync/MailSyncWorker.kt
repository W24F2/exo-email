package com.example.exoemail.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.exoemail.LiquidMailApp
import com.example.exoemail.core.mail.engine.MailEngineFactory
import com.example.exoemail.core.security.CredentialStore
import com.example.exoemail.data.MailRepository
import kotlinx.coroutines.flow.first

class MailSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as LiquidMailApp
        val repo = MailRepository(app.database, MailEngineFactory.create())
        val credentials = CredentialStore(app)
        val accounts = app.database.accountDao().observeAll().first().filter { it.syncEnabled }
        var synced = 0
        var failed = false
        for (account in accounts) {
            val secret = credentials.get(account.id)?.secret ?: continue
            repo.refreshInbox(account, secret)
                .onSuccess { synced += it }
                .onFailure { failed = true }
        }
        if (synced > 0) notifyNewMail(synced)
        return if (failed && synced == 0) Result.retry() else Result.success()
    }

    private fun notifyNewMail(count: Int) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("new_mail", "New mail", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val n = NotificationCompat.Builder(applicationContext, "new_mail")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("Liquid Mail")
            .setContentText("$count new message${if (count == 1) "" else "s"}")
            .setAutoCancel(true)
            .build()
        nm.notify(1, n)
    }
}
