package com.networkguardian.domain.models

/** What THIS Android device/build actually allows NetworkGuardian to do — detected, never assumed. */
enum class CapabilityStatus {
    SUPPORTED,
    LIMITED,
    NOT_AVAILABLE
}

data class DeviceCapabilities(
    val canDiscoverDevices: CapabilityStatus,
    val canReadNetworkInfo: CapabilityStatus,
    val canMonitorNetwork: CapabilityStatus,
    val canControlHotspot: CapabilityStatus,
    val canBlockDevice: CapabilityStatus,
    val canUnblockDevice: CapabilityStatus,
    val canEnumerateHotspotClients: CapabilityStatus
) {
    companion object {
        fun unknown() = DeviceCapabilities(
            canDiscoverDevices = CapabilityStatus.NOT_AVAILABLE,
            canReadNetworkInfo = CapabilityStatus.NOT_AVAILABLE,
            canMonitorNetwork = CapabilityStatus.NOT_AVAILABLE,
            canControlHotspot = CapabilityStatus.NOT_AVAILABLE,
            canBlockDevice = CapabilityStatus.NOT_AVAILABLE,
            canUnblockDevice = CapabilityStatus.NOT_AVAILABLE,
            canEnumerateHotspotClients = CapabilityStatus.NOT_AVAILABLE
        )
    }
}
