package com.networkguardian.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.networkguardian.AppGraph
import com.networkguardian.domain.models.ConnectionEvent
import com.networkguardian.domain.models.ConnectionEventType
import com.networkguardian.domain.models.DeviceType
import com.networkguardian.domain.models.TrustState
import com.networkguardian.data.database.DeviceEntity
import com.networkguardian.network.identification.DeviceIdentifier
import com.networkguardian.notifications.NotificationCategory
import java.util.UUID

/**
 * Periodic (WorkManager-scheduled, respects Doze/battery restrictions) discovery pass:
 * runs one NetworkDiscoveryEngine sweep, reconciles results against the known device table,
 * logs join/leave events, and fires notifications for genuinely new/unknown devices.
 *
 * Never invents devices: only hosts actually observed as reachable or present in the kernel
 * neighbor table are recorded.
 */
class NetworkMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val graph = AppGraph.get(applicationContext)
        val profile = graph.activeProfileProvider.currentProfileId() ?: return Result.success()

        val discovered = graph.discoveryEngine.discoverHosts()
        val now = System.currentTimeMillis()

        for (host in discovered) {
            val deviceId = host.macAddress ?: "ip:${host.ipAddress}"
            val existingDevice = graph.deviceRepository.getDevice(deviceId)

            if (existingDevice == null) {
                val type = DeviceIdentifier.classify(null)
                graph.deviceRepository.upsertDevice(
                    DeviceEntity(
                        id = deviceId,
                        networkProfileId = profile,
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
                graph.deviceRepository.logEvent(
                    ConnectionEvent(
                        deviceId = deviceId,
                        deviceLabel = host.ipAddress,
                        eventType = ConnectionEventType.DEVICE_DISCOVERED,
                        timestampEpochMs = now,
                        networkProfileId = profile
                    )
                )
                graph.notificationManager.notify(
                    NotificationCategory.NEW_DEVICE,
                    "New device detected",
                    "A new device (${host.ipAddress}) joined the network."
                )
            } else if (!existingDevice.isCurrentlyReachable && host.reachable) {
                graph.deviceRepository.upsertDevice(existingDevice.copy(isCurrentlyReachable = true, lastSeenEpochMs = now, ipAddress = host.ipAddress))
                graph.deviceRepository.logEvent(
                    ConnectionEvent(
                        deviceId = deviceId,
                        deviceLabel = existingDevice.userLabel ?: existingDevice.friendlyName ?: host.ipAddress,
                        eventType = ConnectionEventType.DEVICE_CONNECTED,
                        timestampEpochMs = now,
                        networkProfileId = profile
                    )
                )
            } else {
                graph.deviceRepository.upsertDevice(existingDevice.copy(lastSeenEpochMs = now, isCurrentlyReachable = true))
            }
        }

        // Expire any pause windows that have elapsed.
        val expired = graph.deviceRepository.expiredPauses(now)
        for (pause in expired) {
            graph.deviceRepository.recordResume(pause.deviceId)
            graph.deviceRepository.logEvent(
                ConnectionEvent(
                    deviceId = pause.deviceId,
                    deviceLabel = pause.deviceId,
                    eventType = ConnectionEventType.DEVICE_RESUMED,
                    timestampEpochMs = now,
                    networkProfileId = profile,
                    detail = "Pause window elapsed"
                )
            )
        }

        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "network_monitor_periodic"
    }
}
