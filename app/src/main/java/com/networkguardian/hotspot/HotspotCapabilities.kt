package com.networkguardian.hotspot

import com.networkguardian.domain.models.CapabilityStatus

/**
 * Snapshot of what NetworkGuardian can actually do with respect to the device's own hotspot,
 * on THIS Android build. See CapabilityDetector for the reasoning behind each value — this
 * class exists so hotspot-specific UI can read a focused subset without pulling in the full
 * DeviceCapabilities model.
 */
data class HotspotCapabilities(
    val canDetectHotspotState: CapabilityStatus,
    val canEnumerateClients: CapabilityStatus,
    val canControlHotspot: CapabilityStatus
)
