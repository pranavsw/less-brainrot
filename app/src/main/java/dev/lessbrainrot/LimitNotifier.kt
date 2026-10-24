package dev.lessbrainrot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDate

class LimitNotifier(private val context: Context) {

    fun maybeNotify(total: Int) {
        val prefs = context.getSharedPreferences(Prefs.SETTINGS, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val should = shouldNotify(
            total = total,
            limit = prefs.getInt(Prefs.KEY_LIMIT, 0),
            remindersEnabled = prefs.getBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, true),
            lastNotifiedDate = prefs.getString(Prefs.KEY_LIMIT_NOTIFIED_DATE, null),
            today = today
        )
        if (!should) return
        prefs.edit().putString(Prefs.KEY_LIMIT_NOTIFIED_DATE, today).apply()
        postNotification(total, prefs.getInt(Prefs.KEY_LIMIT, 0))
    }

    private fun postNotification(total: Int, limit: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.limit_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.limit_channel_description) }
        manager.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_scroll)
            .setContentTitle(context.getString(R.string.limit_reached_title))
            .setContentText(context.getString(R.string.limit_reached_text, total, limit))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.limit_reached_text, total, limit))
            )
            .setAutoCancel(true)
            .build()
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the post.
        }
    }

    companion object {
        const val CHANNEL_ID = "daily_limit"
        const val NOTIFICATION_ID = 1001

        // One notification per day, only once the combined limit is reached, and only
        // if reminders are enabled. A limit of 0 means no limit is set.
        internal fun shouldNotify(
            total: Int,
            limit: Int,
            remindersEnabled: Boolean,
            lastNotifiedDate: String?,
            today: String
        ): Boolean =
            remindersEnabled && limit > 0 && total >= limit && lastNotifiedDate != today
    }
}
