package com.networkguardian.domain.models

/**
 * Domain-level representation of a device seen on the local network. Any field NetworkGuardian
 * cannot verify is left null and rendered in the UI as "Unavailable" rather than fabricated.
 */
data class NetworkDevice(
    val id: String,
    val friendlyName: String?,
    val ipAddress: String?,
    val macAddress: String?,
    val deviceType: DeviceType,
    val trustState: TrustState,
    val firstSeenEpochMs: Long,
    val lastSeenEpochMs: Long,
    val isCurrentlyReachable: Boolean,
    val networkProfileId: String
)
