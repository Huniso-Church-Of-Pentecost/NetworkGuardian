package com.networkguardian.data.repository

import com.networkguardian.data.database.AppSettingsDao
import com.networkguardian.data.database.AppSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Simple key/value settings store backed by Room, covering notification/security/monitoring prefs. */
class SettingsRepository(private val dao: AppSettingsDao) {

    object Keys {
        const val AUTO_LOCK_MINUTES = "auto_lock_minutes" // -1 = never, 0 = immediately
        const val BIOMETRIC_ENABLED = "biometric_enabled"
        const val PIN_HASH = "pin_hash"
        const val PIN_SALT = "pin_salt"
        const val NOTIFY_NEW_DEVICE = "notify_new_device"
        const val NOTIFY_UNKNOWN_DEVICE = "notify_unknown_device"
        const val NOTIFY_DISCONNECT = "notify_disconnect"
        const val NOTIFY_BLOCKED_SEEN = "notify_blocked_seen"
        const val MONITOR_INTERVAL_SECONDS = "monitor_interval_seconds"
        const val THEME_MODE = "theme_mode" // "system" | "light" | "dark"
    }

    suspend fun getString(key: String, default: String? = null): String? =
        dao.get(key)?.value ?: default

    suspend fun setString(key: String, value: String) = dao.set(AppSettingsEntity(key, value))

    suspend fun getBoolean(key: String, default: Boolean): Boolean =
        dao.get(key)?.value?.toBooleanStrictOrNull() ?: default

    suspend fun setBoolean(key: String, value: Boolean) = setString(key, value.toString())

    suspend fun getInt(key: String, default: Int): Int =
        dao.get(key)?.value?.toIntOrNull() ?: default

    suspend fun setInt(key: String, value: Int) = setString(key, value.toString())

    fun observeAll(): Flow<Map<String, String>> = dao.observeAll().map { list ->
        list.associate { it.key to it.value }
    }
}
