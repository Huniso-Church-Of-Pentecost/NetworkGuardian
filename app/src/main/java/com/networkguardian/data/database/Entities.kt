package com.networkguardian.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_profiles")
data class NetworkProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdEpochMs: Long,
    val isHotspotProfile: Boolean,
    val lastActiveEpochMs: Long?
)

/**
 * A device observed on the network. Nullable fields reflect information Android genuinely
 * did not expose — they are never backfilled with guesses.
 */
@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val networkProfileId: String,
    val friendlyName: String?,
    val userLabel: String?,
    val ipAddress: String?,
    val macAddress: String?,
    val deviceType: String,
    val firstSeenEpochMs: Long,
    val lastSeenEpochMs: Long,
    val isCurrentlyReachable: Boolean
)

@Entity(tableName = "trusted_devices")
data class TrustedDeviceEntity(
    @PrimaryKey val deviceId: String,
    val trustedAtEpochMs: Long
)

@Entity(tableName = "blocked_devices")
data class BlockedDeviceEntity(
    @PrimaryKey val deviceId: String,
    val macAddress: String?,
    val ipAddress: String?,
    val deviceLabel: String,
    val blockedAtEpochMs: Long,
    val reason: String?,
    val lastSeenEpochMs: Long?,
    /** Whether the underlying platform actually enforced the block, vs. list-only. */
    val enforcementActive: Boolean,
    val enforcementNote: String
)

@Entity(tableName = "paused_devices")
data class PausedDeviceEntity(
    @PrimaryKey val deviceId: String,
    val pausedAtEpochMs: Long,
    /** Null means "until manually restored". */
    val resumeAtEpochMs: Long?
)

@Entity(tableName = "connection_events")
data class ConnectionEventEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String?,
    val deviceLabel: String,
    val eventType: String,
    val timestampEpochMs: Long,
    val networkProfileId: String,
    val detail: String?
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val key: String,
    val value: String
)
