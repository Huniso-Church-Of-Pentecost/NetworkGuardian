package com.networkguardian

import android.content.Context
import com.networkguardian.blocking.AndroidHotspotController
import com.networkguardian.blocking.BlockCapability
import com.networkguardian.blocking.BlockManager
import com.networkguardian.data.database.NetworkGuardianDatabase
import com.networkguardian.data.repository.DeviceRepository
import com.networkguardian.data.repository.NetworkProfileRepository
import com.networkguardian.data.repository.SettingsRepository
import com.networkguardian.domain.models.CapabilityStatus
import com.networkguardian.domain.usecases.BlockDeviceUseCase
import com.networkguardian.domain.usecases.ForgetDeviceUseCase
import com.networkguardian.domain.usecases.PauseDeviceUseCase
import com.networkguardian.domain.usecases.RenameDeviceUseCase
import com.networkguardian.domain.usecases.TrustDeviceUseCase
import com.networkguardian.domain.usecases.UnblockDeviceUseCase
import com.networkguardian.domain.usecases.UntrustDeviceUseCase
import com.networkguardian.hotspot.HotspotManager
import com.networkguardian.network.discovery.CapabilityDetector
import com.networkguardian.network.discovery.NetworkDiscoveryEngine
import com.networkguardian.network.monitoring.NetworkMonitor
import com.networkguardian.notifications.DeviceNotificationManager
import com.networkguardian.security.AppLockManager

/** Tracks which network profile is currently active. Backed by settings so it persists. */
class ActiveProfileProvider(private val settings: SettingsRepository) {
    private var cached: String? = null

    suspend fun currentProfileId(): String? {
        cached?.let { return it }
        val stored = settings.getString("active_profile_id")
        cached = stored
        return stored
    }

    suspend fun setActiveProfile(profileId: String) {
        cached = profileId
        settings.setString("active_profile_id", profileId)
    }
}

/**
 * Minimal hand-rolled dependency graph (no Hilt/Dagger, to keep the dependency surface small).
 * Exposes a single process-wide instance via [get].
 */
class AppGraph private constructor(context: Context) {
    private val appContext = context.applicationContext

    val database: NetworkGuardianDatabase = NetworkGuardianDatabase.getInstance(appContext)

    val settingsRepository = SettingsRepository(database.appSettingsDao())
    val networkProfileRepository = NetworkProfileRepository(database.networkProfileDao())
    val deviceRepository = DeviceRepository(
        deviceDao = database.deviceDao(),
        trustedDeviceDao = database.trustedDeviceDao(),
        blockedDeviceDao = database.blockedDeviceDao(),
        pausedDeviceDao = database.pausedDeviceDao(),
        connectionEventDao = database.connectionEventDao()
    )

    val capabilityDetector = CapabilityDetector(appContext)
    val discoveryEngine = NetworkDiscoveryEngine(appContext)
    val networkMonitor = NetworkMonitor(appContext)
    val hotspotManager = HotspotManager(appContext)

    private val capabilities = capabilityDetector.detect()

    val blockCapability = BlockCapability(
        canRecordBlocklist = CapabilityStatus.SUPPORTED, // local storage always works
        canEnforceBlock = capabilities.canBlockDevice,
        canPauseTemporarily = capabilities.canBlockDevice // temporary pause requires the same enforcement path
    )

    private val networkController = AndroidHotspotController()
    val blockManager = BlockManager(deviceRepository, blockCapability)

    val notificationManager = DeviceNotificationManager(appContext, settingsRepository)
    val appLockManager = AppLockManager(settingsRepository)
    val activeProfileProvider = ActiveProfileProvider(settingsRepository)

    // Use cases
    val trustDeviceUseCase = TrustDeviceUseCase(deviceRepository)
    val untrustDeviceUseCase = UntrustDeviceUseCase(deviceRepository)
    val blockDeviceUseCase = BlockDeviceUseCase(blockManager)
    val unblockDeviceUseCase = UnblockDeviceUseCase(blockManager)
    val pauseDeviceUseCase = PauseDeviceUseCase(blockManager)
    val renameDeviceUseCase = RenameDeviceUseCase(deviceRepository)
    val forgetDeviceUseCase = ForgetDeviceUseCase(deviceRepository)

    fun currentCapabilities() = capabilities

    companion object {
        @Volatile private var INSTANCE: AppGraph? = null

        fun get(context: Context): AppGraph = INSTANCE ?: synchronized(this) {
            INSTANCE ?: AppGraph(context).also { INSTANCE = it }
        }
    }
}
