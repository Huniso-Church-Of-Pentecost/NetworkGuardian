package com.networkguardian.hotspot

import android.content.Context
import android.net.wifi.WifiManager
import com.networkguardian.domain.models.CapabilityStatus

/**
 * Reads whatever hotspot-adjacent state Android legitimately exposes to a normal app.
 *
 * IMPORTANT: WifiManager does not expose a public, stable "is my hotspot currently active"
 * boolean on modern Android (the old reflection-based `isWifiApEnabled` was never public API
 * and was removed/blocked on later versions). NetworkGuardian does not use reflection to call
 * hidden methods. As a result, hotspot on/off state is reported as NOT_AVAILABLE unless the
 * OS surfaces it through a documented channel (e.g. the LOCAL_ONLY_HOTSPOT callback path when
 * *this app itself* starts a local-only hotspot via WifiManager.startLocalOnlyHotspot).
 *
 * This class intentionally does NOT implement client enumeration or client control — no public
 * API exists for either on stock Android for third-party apps. HotspotCapabilities always
 * reports those as NOT_AVAILABLE, and the UI must not offer controls that would silently no-op.
 */
class HotspotManager(private val context: Context) {

    private val wifiManager: WifiManager? by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    }

    fun capabilities(): HotspotCapabilities = HotspotCapabilities(
        canDetectHotspotState = CapabilityStatus.NOT_AVAILABLE,
        canEnumerateClients = CapabilityStatus.NOT_AVAILABLE,
        canControlHotspot = CapabilityStatus.NOT_AVAILABLE
    )

    /**
     * Whether Wi-Fi itself is enabled on the device (this IS public API). This is distinct
     * from "is a hotspot active" and is only useful as supporting context in the UI.
     */
    fun isWifiEnabled(): Boolean = wifiManager?.isWifiEnabled ?: false
}
