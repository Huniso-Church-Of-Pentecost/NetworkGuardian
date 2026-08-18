package com.networkguardian.network.identification

import com.networkguardian.domain.models.DeviceType

/**
 * Best-effort, evidence-based classification. Deliberately conservative: returns UNKNOWN
 * unless a signal is genuinely indicative. NetworkGuardian does not attempt MAC-vendor OUI
 * lookups against a bundled/offline table here to avoid overclaiming manufacturer identity;
 * that can be added later as an explicit, clearly-labeled "vendor guess" feature if desired.
 */
object DeviceIdentifier {

    /**
     * @param hostname a locally observed hostname/mDNS name, if any (never fetched from the network
     *   in an intrusive way — only what standard resolution already provides).
     */
    fun classify(hostname: String?): DeviceType {
        val name = hostname?.lowercase() ?: return DeviceType.UNKNOWN

        return when {
            listOf("iphone").any { name.contains(it) } -> DeviceType.IPHONE
            listOf("ipad").any { name.contains(it) } -> DeviceType.TABLET
            listOf("android").any { name.contains(it) } && name.contains("tv") -> DeviceType.SMART_TV
            listOf("android").any { name.contains(it) } -> DeviceType.ANDROID_PHONE
            listOf("macbook", "laptop", "notebook").any { name.contains(it) } -> DeviceType.LAPTOP
            listOf("imac", "desktop", "pc-", "-pc").any { name.contains(it) } -> DeviceType.DESKTOP
            listOf("roku", "chromecast", "appletv", "smarttv", "bravia", "firetv").any { name.contains(it) } -> DeviceType.SMART_TV
            listOf("esp", "iot", "sonoff", "shelly", "tasmota", "sensor", "plug", "bulb").any { name.contains(it) } -> DeviceType.IOT_DEVICE
            else -> DeviceType.UNKNOWN
        }
    }
}
