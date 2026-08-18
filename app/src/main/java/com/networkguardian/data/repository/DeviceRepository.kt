package com.networkguardian.data.repository

import com.networkguardian.data.database.BlockedDeviceEntity
import com.networkguardian.data.database.ConnectionEventEntity
import com.networkguardian.data.database.DeviceDao
import com.networkguardian.data.database.DeviceEntity
import com.networkguardian.data.database.PausedDeviceEntity
import com.networkguardian.data.database.TrustedDeviceDao
import com.networkguardian.data.database.BlockedDeviceDao
import com.networkguardian.data.database.PausedDeviceDao
import com.networkguardian.data.database.ConnectionEventDao
import com.networkguardian.data.database.TrustedDeviceEntity
import com.networkguardian.domain.models.ConnectionEvent
import com.networkguardian.domain.models.ConnectionEventType
import com.networkguardian.domain.models.DeviceType
import com.networkguardian.domain.models.NetworkDevice
import com.networkguardian.domain.models.TrustState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Single source of truth for device state. Combines the raw device table with trust/block
 * status to produce the domain model consumed by the UI and use cases.
 */
class DeviceRepository(
    private val deviceDao: DeviceDao,
    private val trustedDeviceDao: TrustedDeviceDao,
    private val blockedDeviceDao: BlockedDeviceDao,
    private val pausedDeviceDao: PausedDeviceDao,
    private val connectionEventDao: ConnectionEventDao
) {
    fun observeDevices(profileId: String): Flow<List<NetworkDevice>> {
        return combine(
            deviceDao.observeDevices(profileId),
            trustedDeviceDao.observeAll(),
            blockedDeviceDao.observeAll()
        ) { devices, trusted, blocked ->
            val trustedIds = trusted.map { it.deviceId }.toSet()
            val blockedIds = blocked.map { it.deviceId }.toSet()
            devices.map { it.toDomain(trustedIds, blockedIds) }
        }
    }

    fun observeDevice(deviceId: String): Flow<NetworkDevice?> = deviceDao.observeDevice(deviceId).map { entity ->
        entity?.let {
            val trusted = trustedDeviceDao.isTrusted(deviceId)
            val blocked = blockedDeviceDao.isBlocked(deviceId)
            it.toDomain(
                trustedIds = if (trusted) setOf(deviceId) else emptySet(),
                blockedIds = if (blocked) setOf(deviceId) else emptySet()
            )
        }
    }

    suspend fun upsertDevice(entity: DeviceEntity) = deviceDao.upsert(entity)

    suspend fun getDevice(id: String) = deviceDao.getDevice(id)

    suspend fun renameDevice(deviceId: String, newLabel: String) {
        deviceDao.getDevice(deviceId)?.let { existing ->
            deviceDao.update(existing.copy(userLabel = newLabel))
        }
    }

    suspend fun forgetDevice(deviceId: String) {
        deviceDao.forget(deviceId)
        trustedDeviceDao.untrust(deviceId)
        blockedDeviceDao.unblock(deviceId)
        pausedDeviceDao.resume(deviceId)
    }

    suspend fun trustDevice(deviceId: String, atEpochMs: Long) =
        trustedDeviceDao.trust(TrustedDeviceEntity(deviceId, atEpochMs))

    suspend fun untrustDevice(deviceId: String) = trustedDeviceDao.untrust(deviceId)

    suspend fun recordBlock(entity: BlockedDeviceEntity) = blockedDeviceDao.block(entity)

    suspend fun recordUnblock(deviceId: String) = blockedDeviceDao.unblock(deviceId)

    suspend fun getBlockRecord(deviceId: String) = blockedDeviceDao.get(deviceId)

    fun observeBlockedDevices() = blockedDeviceDao.observeAll()

    suspend fun recordPause(entity: PausedDeviceEntity) = pausedDeviceDao.pause(entity)

    suspend fun recordResume(deviceId: String) = pausedDeviceDao.resume(deviceId)

    fun observePausedDevices() = pausedDeviceDao.observeAll()

    suspend fun expiredPauses(nowEpochMs: Long) = pausedDeviceDao.expiredPauses(nowEpochMs)

    suspend fun logEvent(event: ConnectionEvent) {
        connectionEventDao.insert(
            ConnectionEventEntity(
                deviceId = event.deviceId,
                deviceLabel = event.deviceLabel,
                eventType = event.eventType.name,
                timestampEpochMs = event.timestampEpochMs,
                networkProfileId = event.networkProfileId,
                detail = event.detail
            )
        )
    }

    fun observeHistory(profileId: String, limit: Int = 200): Flow<List<ConnectionEvent>> =
        connectionEventDao.observeRecent(profileId, limit).map { list ->
            list.map {
                ConnectionEvent(
                    id = it.id,
                    deviceId = it.deviceId,
                    deviceLabel = it.deviceLabel,
                    eventType = ConnectionEventType.valueOf(it.eventType),
                    timestampEpochMs = it.timestampEpochMs,
                    networkProfileId = it.networkProfileId,
                    detail = it.detail
                )
            }
        }

    suspend fun clearHistory(profileId: String) = connectionEventDao.clearHistory(profileId)

    private fun DeviceEntity.toDomain(trustedIds: Set<String>, blockedIds: Set<String>): NetworkDevice {
        val trustState = when {
            blockedIds.contains(id) -> TrustState.BLOCKED
            trustedIds.contains(id) -> TrustState.TRUSTED
            else -> TrustState.UNKNOWN
        }
        return NetworkDevice(
            id = id,
            friendlyName = userLabel ?: friendlyName,
            ipAddress = ipAddress,
            macAddress = macAddress,
            deviceType = runCatching { DeviceType.valueOf(deviceType) }.getOrDefault(DeviceType.UNKNOWN),
            trustState = trustState,
            firstSeenEpochMs = firstSeenEpochMs,
            lastSeenEpochMs = lastSeenEpochMs,
            isCurrentlyReachable = isCurrentlyReachable,
            networkProfileId = networkProfileId
        )
    }
}
