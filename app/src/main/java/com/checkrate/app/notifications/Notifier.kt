package com.checkrate.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.checkrate.app.MainActivity
import com.checkrate.app.R

class Notifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    private fun contentPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun ensureChannels() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alerts = NotificationChannel(
            CHANNEL_ALERTS,
            "Rate alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val summary = NotificationChannel(
            CHANNEL_SUMMARY,
            "Daily summary",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val live = NotificationChannel(
            CHANNEL_LIVE,
            "FX and Gold/Silver Rate",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(alerts)
        notificationManager.createNotificationChannel(summary)
        notificationManager.createNotificationChannel(live)
    }

    fun sendRateAlert(title: String, message: String, notificationId: Int) {
        ensureChannels()
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(contentPendingIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(notificationId, notification)
    }

    fun sendDailySummary(message: String) {
        ensureChannels()
        val notification = NotificationCompat.Builder(context, CHANNEL_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Daily FX Summary")
            .setContentText(message)
            .setContentIntent(contentPendingIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(SUMMARY_NOTIFICATION_ID, notification)
    }

    fun buildLiveModeNotification(message: String): android.app.Notification {
        ensureChannels()
        return NotificationCompat.Builder(context, CHANNEL_LIVE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("FX and Gold/Silver Rate")
            .setContentText(message)
            .setContentIntent(contentPendingIntent())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updateLiveModeNotification(message: String) {
        ensureChannels()
        val notification = buildLiveModeNotification(message)
        manager.notify(LIVE_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ALERTS = "rate_alerts"
        private const val CHANNEL_SUMMARY = "daily_summary"
        private const val CHANNEL_LIVE = "live_mode"
        private const val SUMMARY_NOTIFICATION_ID = 2001
        private const val LIVE_NOTIFICATION_ID = 3001
    }
}