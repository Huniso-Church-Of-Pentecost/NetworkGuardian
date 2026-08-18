package com.networkguardian.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.networkguardian.R
import com.networkguardian.data.repository.SettingsRepository

enum class NotificationCategory {
    NEW_DEVICE, UNKNOWN_DEVICE, DISCONNECT, BLOCKED_DEVICE_SEEN
}

/**
 * Sends local notifications for device events, respecting both the OS-level POST_NOTIFICATIONS
 * permission (Android 13+) and the user's per-category preferences in Settings.
 */
class DeviceNotificationManager(
    private val context: Context,
    private val settings: SettingsRepository
) {
    private var nextId = 1000

    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun notify(category: NotificationCategory, title: String, text: String) {
        if (!hasPermission()) return

        val enabled = when (category) {
            NotificationCategory.NEW_DEVICE -> settings.getBoolean(SettingsRepository.Keys.NOTIFY_NEW_DEVICE, true)
            NotificationCategory.UNKNOWN_DEVICE -> settings.getBoolean(SettingsRepository.Keys.NOTIFY_UNKNOWN_DEVICE, true)
            NotificationCategory.DISCONNECT -> settings.getBoolean(SettingsRepository.Keys.NOTIFY_DISCONNECT, false)
            NotificationCategory.BLOCKED_DEVICE_SEEN -> settings.getBoolean(SettingsRepository.Keys.NOTIFY_BLOCKED_SEEN, true)
        }
        if (!enabled) return

        val notification = NotificationCompat.Builder(context, NotificationChannels.EVENTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(nextId++, notification)
    }
}
