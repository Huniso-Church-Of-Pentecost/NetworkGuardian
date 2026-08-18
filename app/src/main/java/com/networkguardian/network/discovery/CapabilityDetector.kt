package com.networkguardian.network.discovery

import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.networkguardian.domain.models.CapabilityStatus
import com.networkguardian.domain.models.DeviceCapabilities

/**
 * Determines, at runtime, exactly what THIS device/Android build/app permission state allows.
 * This is the single source of truth the rest of the app must defer to — no feature is ever
 * assumed available. Values are honest about limitations rather than optimistic.
 *
 * Ground truth this class encodes:
 *  - Ordinary (non-privileged, non-device-owner) Android apps cannot enumerate Wi-Fi AP /
 *    hotspot clients through a public API. There is no public WifiManager method for this.
 *  - Ordinary apps cannot force-disconnect another device from a hotspot/AP they did not
 *    create at the OS level. There is no public "kick client" API.
 *  - Apps CAN read local network state (ACCESS_NETWORK_STATE/ACCESS_WIFI_STATE), observe
 *    connectivity changes (ConnectivityManager.NetworkCallback), and perform local-subnet
 *    reachability checks (e.g. ARP table / ping sweep) to build a best-effort device list —
 *    this is "LIMITED" discovery, not full client enumeration.
 *  - Apps CAN detect whether the device's own Wi-Fi is connected/hotspot-adjacent state via
 *    WifiManager where exposed, subject to OS version differences.
 */
class CapabilityDetector(private val context: Context) {

    fun detect(): DeviceCapabilities {
        val hasNetworkState = hasPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
        val hasWifiState = hasPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager

        val canReadNetworkInfo = if (hasNetworkState) CapabilityStatus.SUPPORTED else CapabilityStatus.NOT_AVAILABLE

        // Local-subnet reachability discovery is possible for any app with network access;
        // it is NOT full client enumeration, so it is always reported as LIMITED, not SUPPORTED.
        val canDiscoverDevices = if (hasWifiState && hasNetworkState) {
            CapabilityStatus.LIMITED
        } else {
            CapabilityStatus.NOT_AVAILABLE
        }

        val canMonitorNetwork = if (hasNetworkState) CapabilityStatus.SUPPORTED else CapabilityStatus.NOT_AVAILABLE

        // No public Android API lets an ordinary app control (enable/disable/configure) the
        // system hotspot on stock, non-device-owner builds. This is intentionally hard-coded
        // to NOT_AVAILABLE rather than probed, because probing would require exactly the
        // hidden/reflection-based APIs this project refuses to use.
        val canControlHotspot = CapabilityStatus.NOT_AVAILABLE

        // No public API exists for an ordinary third-party app to enumerate hotspot client
        // lists (MAC/IP of connected stations) on stock Android. Some OEM skins expose this
        // only to system apps. We do not use reflection/hidden APIs to attempt it.
        val canEnumerateHotspotClients = CapabilityStatus.NOT_AVAILABLE

        // No public, non-privileged API exists to force-disconnect or persistently ban a
        // client from a hotspot/AP on stock Android. Enforcement is therefore NOT_AVAILABLE
        // by default. If this app is ever installed as a device-owner/profile-owner (e.g. via
        // Android Enterprise) additional DevicePolicyManager network controls could apply —
        // that path is out of scope for this build and is not implemented.
        val canBlockDevice = CapabilityStatus.NOT_AVAILABLE
        val canUnblockDevice = CapabilityStatus.NOT_AVAILABLE

        return DeviceCapabilities(
            canDiscoverDevices = canDiscoverDevices,
            canReadNetworkInfo = canReadNetworkInfo,
            canMonitorNetwork = canMonitorNetwork,
            canControlHotspot = canControlHotspot,
            canBlockDevice = canBlockDevice,
            canUnblockDevice = canUnblockDevice,
            canEnumerateHotspotClients = canEnumerateHotspotClients
        )
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
