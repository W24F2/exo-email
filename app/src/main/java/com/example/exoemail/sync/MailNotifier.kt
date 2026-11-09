package com.example.exoemail.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.exoemail.MainActivity
import com.example.exoemail.R

/**
 * Posts new-mail notifications, keeps the launcher badge count in sync with
 * the unread inbox, and deep-links taps into the app.
 */
object MailNotifier {

    const val CHANNEL_ID = "new_mail"
    const val NOTIFICATION_ID = 1

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "New mail", NotificationManager.IMPORTANCE_DEFAULT).apply {
                setShowBadge(true)
            }
        )
    }

    /** True when we may post — granted on <33 by default. */
    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    /** Deep-link tap opens the app's inbox. */
    private fun contentIntent(context: Context): PendingIntent {
        val i = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_INBOX, true)
        }
        return PendingIntent.getActivity(
            context, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Heads-up "N new messages" notification + badge. */
    fun notifyNewMail(context: Context, count: Int, unreadTotal: Int) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_mail)
            .setContentTitle("Exo Mail")
            .setContentText("$count new message${if (count == 1) "" else "s"}")
            .setNumber(unreadTotal) // feeds the launcher badge on most launchers
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()
        nm.notify(NOTIFICATION_ID, n)
    }

    /** Keep the badge honest even without posting a fresh notification. */
    fun updateBadge(context: Context, unreadTotal: Int) {
        if (!canNotify(context)) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (unreadTotal <= 0) {
            nm.cancel(NOTIFICATION_ID)
            return
        }
        // Silent re-post (IMPORTANCE_DEFAULT channel already created) just to
        // refresh the badge number; no new alert because same id + no sound.
        ensureChannel(context)
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_mail)
            .setContentTitle("Exo Mail")
            .setContentText("$unreadTotal unread")
            .setNumber(unreadTotal)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(contentIntent(context))
            .build()
        nm.notify(NOTIFICATION_ID, n)
    }

    const val EXTRA_OPEN_INBOX = "open_inbox"
}
