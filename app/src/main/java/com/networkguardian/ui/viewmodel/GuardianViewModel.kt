package com.networkguardian.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.networkguardian.AppGraph
import com.networkguardian.data.database.NetworkProfileEntity
import com.networkguardian.domain.models.ConnectionEvent
import com.networkguardian.domain.models.DeviceCapabilities
import com.networkguardian.domain.models.NetworkDevice
import com.networkguardian.domain.models.OperationResult
import com.networkguardian.domain.models.TrustState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.UUID

data class DashboardUiState(
    val activeProfileName: String = "No profile selected",
    val totalDevices: Int = 0,
    val trustedCount: Int = 0,
    val unknownCount: Int = 0,
    val blockedCount: Int = 0,
    val recentActivity: List<ConnectionEvent> = emptyList()
)

class GuardianViewModel(private val graph: AppGraph) : ViewModel() {

    private val _activeProfileId = MutableStateFlow<String?>(null)
    val activeProfileId: StateFlow<String?> = _activeProfileId

    val capabilities: StateFlow<DeviceCapabilities> = MutableStateFlow(graph.currentCapabilities())

    val devices: StateFlow<List<NetworkDevice>> = _activeProfileId
        .flatMapLatest { id -> if (id == null) kotlinx.coroutines.flow.flowOf(emptyList()) else graph.deviceRepository.observeDevices(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<ConnectionEvent>> = _activeProfileId
        .flatMapLatest { id -> if (id == null) kotlinx.coroutines.flow.flowOf(emptyList()) else graph.deviceRepository.observeHistory(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            var profileId = graph.activeProfileProvider.currentProfileId()
            if (profileId == null) {
                profileId = UUID.randomUUID().toString()
                graph.networkProfileRepository.upsertProfile(
                    NetworkProfileEntity(
                        id = profileId,
                        name = "Home Hotspot",
                        createdEpochMs = System.currentTimeMillis(),
                        isHotspotProfile = true,
                        lastActiveEpochMs = System.currentTimeMillis()
                    )
                )
                graph.activeProfileProvider.setActiveProfile(profileId)
            }
            _activeProfileId.value = profileId
        }
    }

    fun trustDevice(device: NetworkDevice) = viewModelScope.launch {
        val profile = _activeProfileId.value ?: return@launch
        graph.trustDeviceUseCase(device.id, device.friendlyName ?: device.id, profile, System.currentTimeMillis())
    }

    fun untrustDevice(device: NetworkDevice) = viewModelScope.launch {
        val profile = _activeProfileId.value ?: return@launch
        graph.untrustDeviceUseCase(device.id, device.friendlyName ?: device.id, profile, System.currentTimeMillis())
    }

    fun blockDevice(device: NetworkDevice, reason: String?, onResult: (OperationResult) -> Unit) = viewModelScope.launch {
        val profile = _activeProfileId.value ?: return@launch
        val result = graph.blockDeviceUseCase(
            device.id, device.macAddress, device.ipAddress,
            device.friendlyName ?: device.id, profile, reason, System.currentTimeMillis()
        )
        onResult(result)
    }

    fun unblockDevice(device: NetworkDevice, onResult: (OperationResult) -> Unit) = viewModelScope.launch {
        val profile = _activeProfileId.value ?: return@launch
        val result = graph.unblockDeviceUseCase(device.id, device.friendlyName ?: device.id, profile, System.currentTimeMillis())
        onResult(result)
    }

    fun pauseDevice(device: NetworkDevice, minutes: Int?, onResult: (OperationResult) -> Unit) = viewModelScope.launch {
        val profile = _activeProfileId.value ?: return@launch
        val result = graph.pauseDeviceUseCase(device.id, device.friendlyName ?: device.id, profile, minutes, System.currentTimeMillis())
        onResult(result)
    }

    fun renameDevice(device: NetworkDevice, newLabel: String) = viewModelScope.launch {
        graph.renameDeviceUseCase(device.id, newLabel)
    }

    fun forgetDevice(device: NetworkDevice) = viewModelScope.launch {
        graph.forgetDeviceUseCase(device.id)
    }

    fun clearHistory() = viewModelScope.launch {
        val profile = _activeProfileId.value ?: return@launch
        graph.deviceRepository.clearHistory(profile)
    }

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    fun scanNow() = viewModelScope.launch {
        val profile = _activeProfileId.value ?: return@launch
        _isScanning.value = true
        try {
            graph.discoveryReconciler.runOnce(profile)
        } finally {
            _isScanning.value = false
        }
    }

    fun dashboardState(): DashboardUiState {
        val list = devices.value
        return DashboardUiState(
            activeProfileName = "Home Hotspot",
            totalDevices = list.size,
            trustedCount = list.count { it.trustState == TrustState.TRUSTED },
            unknownCount = list.count { it.trustState == TrustState.UNKNOWN },
            blockedCount = list.count { it.trustState == TrustState.BLOCKED },
            recentActivity = history.value.take(5)
        )
    }

    companion object {
        fun factory(graph: AppGraph): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GuardianViewModel(graph) as T
            }
        }
    }
}
