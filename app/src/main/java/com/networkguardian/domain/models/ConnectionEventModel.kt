package com.networkguardian.domain.models

enum class ConnectionEventType {
    DEVICE_DISCOVERED,
    DEVICE_CONNECTED,
    DEVICE_DISCONNECTED,
    DEVICE_TRUSTED,
    DEVICE_UNTRUSTED,
    DEVICE_BLOCKED,
    DEVICE_UNBLOCKED,
    DEVICE_PAUSED,
    DEVICE_RESUMED,
    NETWORK_CHANGED,
    HOTSPOT_ENABLED,
    HOTSPOT_DISABLED
}

data class ConnectionEvent(
    val id: Long = 0,
    val deviceId: String?,
    val deviceLabel: String,
    val eventType: ConnectionEventType,
    val timestampEpochMs: Long,
    val networkProfileId: String,
    val detail: String? = null
)
