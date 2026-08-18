package com.networkguardian.notifications

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val EVENTS_CHANNEL_ID = "networkguardian_events"
    const val MONITORING_CHANNEL_ID = "networkguardian_monitoring"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                EVENTS_CHANNEL_ID,
                "Device activity",
                AndroidNotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "New, unknown, and disconnected device alerts"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                MONITORING_CHANNEL_ID,
                "Active monitoring",
                AndroidNotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shown while NetworkGuardian is actively monitoring your network"
            }
        )
    }
}
