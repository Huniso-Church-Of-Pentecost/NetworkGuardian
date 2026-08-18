package com.networkguardian.domain.usecases

import com.networkguardian.blocking.BlockManager
import com.networkguardian.data.repository.DeviceRepository
import com.networkguardian.domain.models.ConnectionEvent
import com.networkguardian.domain.models.ConnectionEventType
import com.networkguardian.domain.models.OperationResult

class TrustDeviceUseCase(private val repository: DeviceRepository) {
    suspend operator fun invoke(deviceId: String, label: String, profileId: String, nowEpochMs: Long): OperationResult {
        repository.trustDevice(deviceId, nowEpochMs)
        repository.logEvent(
            ConnectionEvent(
                deviceId = deviceId,
                deviceLabel = label,
                eventType = ConnectionEventType.DEVICE_TRUSTED,
                timestampEpochMs = nowEpochMs,
                networkProfileId = profileId
            )
        )
        return OperationResult.Success
    }
}

class UntrustDeviceUseCase(private val repository: DeviceRepository) {
    suspend operator fun invoke(deviceId: String, label: String, profileId: String, nowEpochMs: Long): OperationResult {
        repository.untrustDevice(deviceId)
        repository.logEvent(
            ConnectionEvent(
                deviceId = deviceId,
                deviceLabel = label,
                eventType = ConnectionEventType.DEVICE_UNTRUSTED,
                timestampEpochMs = nowEpochMs,
                networkProfileId = profileId
            )
        )
        return OperationResult.Success
    }
}

class BlockDeviceUseCase(private val blockManager: BlockManager) {
    suspend operator fun invoke(
        deviceId: String,
        macAddress: String?,
        ipAddress: String?,
        label: String,
        profileId: String,
        reason: String?,
        nowEpochMs: Long
    ): OperationResult = blockManager.blockDevice(deviceId, macAddress, ipAddress, label, profileId, reason, nowEpochMs)
}

class UnblockDeviceUseCase(private val blockManager: BlockManager) {
    suspend operator fun invoke(deviceId: String, label: String, profileId: String, nowEpochMs: Long): OperationResult =
        blockManager.unblockDevice(deviceId, label, profileId, nowEpochMs)
}

class PauseDeviceUseCase(private val blockManager: BlockManager) {
    suspend operator fun invoke(
        deviceId: String,
        label: String,
        profileId: String,
        durationMinutes: Int?,
        nowEpochMs: Long
    ): OperationResult = blockManager.pauseDevice(deviceId, label, profileId, durationMinutes, nowEpochMs)
}

class RenameDeviceUseCase(private val repository: DeviceRepository) {
    suspend operator fun invoke(deviceId: String, newLabel: String) = repository.renameDevice(deviceId, newLabel)
}

class ForgetDeviceUseCase(private val repository: DeviceRepository) {
    suspend operator fun invoke(deviceId: String) = repository.forgetDevice(deviceId)
}
