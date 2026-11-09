package com.example.exoemail.sync

import android.content.Context
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
        // Keep the launcher badge in sync with total unread across accounts,
        // and only alert when there is genuinely new mail.
        val unread = accounts.sumOf { acc ->
            app.database.messageDao().unreadInboxCount(acc.id)
        }
        if (synced > 0) MailNotifier.notifyNewMail(applicationContext, synced, unread)
        else MailNotifier.updateBadge(applicationContext, unread)
        return if (failed && synced == 0) Result.retry() else Result.success()
    }
}
