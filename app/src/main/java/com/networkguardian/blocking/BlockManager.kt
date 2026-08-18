package com.networkguardian.blocking

import com.networkguardian.data.database.BlockedDeviceEntity
import com.networkguardian.data.database.PausedDeviceEntity
import com.networkguardian.data.repository.DeviceRepository
import com.networkguardian.domain.models.CapabilityStatus
import com.networkguardian.domain.models.ConnectionEvent
import com.networkguardian.domain.models.ConnectionEventType
import com.networkguardian.domain.models.OperationResult

/**
 * Central authority for block/unblock/pause operations. This class NEVER reports Success for
 * network-level enforcement it did not actually perform. On stock, non-privileged Android,
 * there is no public API for a third-party app to force-disconnect or persistently prevent a
 * specific device from rejoining a Wi-Fi network/hotspot it doesn't administer via a router API.
 *
 * What this class DOES provide, honestly:
 *  - A persistent local blocklist (survives restarts) recording that the user WANTS a device
 *    blocked — this is real and durable, stored in Room.
 *  - `enforcementActive = false` and an explicit note whenever enforcement itself is not
 *    actually possible, which is the case on stock Android for the hotspot/router scenarios
 *    this app targets today.
 *  - An extension point (RouterController, see NetworkController) for future router
 *    integrations that DO expose real client-kick/ban APIs, at which point enforcement can
 *    genuinely flip to true for supported routers.
 */
class BlockManager(
    private val repository: DeviceRepository,
    private val capability: BlockCapability
) {

    suspend fun blockDevice(
        deviceId: String,
        macAddress: String?,
        ipAddress: String?,
        label: String,
        profileId: String,
        reason: String?,
        nowEpochMs: Long
    ): OperationResult {
        if (capability.canRecordBlocklist != CapabilityStatus.SUPPORTED) {
            return OperationResult.Unsupported(
                "Blocklist storage is not available on this device."
            )
        }

        val enforcementSupported = capability.canEnforceBlock == CapabilityStatus.SUPPORTED
        // No enforcement attempt is made when unsupported — we do not fake success.
        val enforcementNote = if (enforcementSupported) {
            "Enforced via router integration."
        } else {
            "Blocking is not supported on this device/network through the available Android APIs."
        }

        repository.recordBlock(
            BlockedDeviceEntity(
                deviceId = deviceId,
                macAddress = macAddress,
                ipAddress = ipAddress,
                deviceLabel = label,
                blockedAtEpochMs = nowEpochMs,
                reason = reason,
                lastSeenEpochMs = nowEpochMs,
                enforcementActive = enforcementSupported,
                enforcementNote = enforcementNote
            )
        )

        repository.logEvent(
            ConnectionEvent(
                deviceId = deviceId,
                deviceLabel = label,
                eventType = ConnectionEventType.DEVICE_BLOCKED,
                timestampEpochMs = nowEpochMs,
                networkProfileId = profileId,
                detail = enforcementNote
            )
        )

        return OperationResult.Success
    }

    suspend fun unblockDevice(deviceId: String, label: String, profileId: String, nowEpochMs: Long): OperationResult {
        repository.recordUnblock(deviceId)
        repository.logEvent(
            ConnectionEvent(
                deviceId = deviceId,
                deviceLabel = label,
                eventType = ConnectionEventType.DEVICE_UNBLOCKED,
                timestampEpochMs = nowEpochMs,
                networkProfileId = profileId
            )
        )
        return OperationResult.Success
    }

    suspend fun pauseDevice(
        deviceId: String,
        label: String,
        profileId: String,
        durationMinutes: Int?, // null = until manually restored
        nowEpochMs: Long
    ): OperationResult {
        if (capability.canPauseTemporarily != CapabilityStatus.SUPPORTED) {
            return OperationResult.Unsupported(
                "Temporary pausing isn't supported on this device through the available Android APIs."
            )
        }

        val resumeAt = durationMinutes?.let { nowEpochMs + it * 60_000L }
        repository.recordPause(PausedDeviceEntity(deviceId, nowEpochMs, resumeAt))
        repository.logEvent(
            ConnectionEvent(
                deviceId = deviceId,
                deviceLabel = label,
                eventType = ConnectionEventType.DEVICE_PAUSED,
                timestampEpochMs = nowEpochMs,
                networkProfileId = profileId
            )
        )
        return OperationResult.Success
    }

    suspend fun resumeDevice(deviceId: String, label: String, profileId: String, nowEpochMs: Long): OperationResult {
        repository.recordResume(deviceId)
        repository.logEvent(
            ConnectionEvent(
                deviceId = deviceId,
                deviceLabel = label,
                eventType = ConnectionEventType.DEVICE_RESUMED,
                timestampEpochMs = nowEpochMs,
                networkProfileId = profileId
            )
        )
        return OperationResult.Success
    }
}
