package com.networkguardian.network.discovery

import com.networkguardian.data.database.DeviceEntity
import com.networkguardian.data.repository.DeviceRepository
import com.networkguardian.domain.models.ConnectionEvent
import com.networkguardian.domain.models.ConnectionEventType
import com.networkguardian.network.identification.DeviceIdentifier
import com.networkguardian.notifications.DeviceNotificationManager
import com.networkguardian.notifications.NotificationCategory

/**
 * Runs one discovery pass and reconciles the result against the known device table: records
 * newly-seen hosts, flips reachability on hosts that reappeared, refreshes last-seen for hosts
 * still present, expires elapsed pause windows, and logs/notifies accordingly.
 *
 * Shared by the periodic background worker (NetworkMonitorWorker, WorkManager-scheduled, at
 * least every 15 minutes per WorkManager's minimum periodic interval) and the in-app "Scan now"
 * action (immediate, foreground-triggered, no WorkManager floor).
 */
class DiscoveryReconciler(
    private val discoveryEngine: NetworkDiscoveryEngine,
    private val deviceRepository: DeviceRepository,
    private val notificationManager: DeviceNotificationManager
) {
    suspend fun runOnce(profileId: String) {
        val discovered = discoveryEngine.discoverHosts()
        val now = System.currentTimeMillis()

        for (host in discovered) {
            val deviceId = host.macAddress ?: "ip:${host.ipAddress}"
            val existingDevice = deviceRepository.getDevice(deviceId)

            if (existingDevice == null) {
                val type = DeviceIdentifier.classify(null)
                deviceRepository.upsertDevice(
                    DeviceEntity(
                        id = deviceId,
                        networkProfileId = profileId,
                        friendlyName = null,
                        userLabel = null,
                        ipAddress = host.ipAddress,
                        macAddress = host.macAddress,
                        deviceType = type.name,
                        firstSeenEpochMs = now,
                        lastSeenEpochMs = now,
                        isCurrentlyReachable = host.reachable
                    )
                )
                deviceRepository.logEvent(
                    ConnectionEvent(
                        deviceId = deviceId,
                        deviceLabel = host.ipAddress,
                        eventType = ConnectionEventType.DEVICE_DISCOVERED,
                        timestampEpochMs = now,
                        networkProfileId = profileId
                    )
                )
                notificationManager.notify(
                    NotificationCategory.NEW_DEVICE,
                    "New device detected",
                    "A new device (${host.ipAddress}) joined the network."
                )
            } else if (!existingDevice.isCurrentlyReachable && host.reachable) {
                deviceRepository.upsertDevice(
                    existingDevice.copy(isCurrentlyReachable = true, lastSeenEpochMs = now, ipAddress = host.ipAddress)
                )
                deviceRepository.logEvent(
                    ConnectionEvent(
                        deviceId = deviceId,
                        deviceLabel = existingDevice.userLabel ?: existingDevice.friendlyName ?: host.ipAddress,
                        eventType = ConnectionEventType.DEVICE_CONNECTED,
                        timestampEpochMs = now,
                        networkProfileId = profileId
                    )
                )
            } else {
                deviceRepository.upsertDevice(existingDevice.copy(lastSeenEpochMs = now, isCurrentlyReachable = true))
            }
        }

        val expired = deviceRepository.expiredPauses(now)
        for (pause in expired) {
            deviceRepository.recordResume(pause.deviceId)
            deviceRepository.logEvent(
                ConnectionEvent(
                    deviceId = pause.deviceId,
                    deviceLabel = pause.deviceId,
                    eventType = ConnectionEventType.DEVICE_RESUMED,
                    timestampEpochMs = now,
                    networkProfileId = profileId,
                    detail = "Pause window elapsed"
                )
            )
        }
    }
}
