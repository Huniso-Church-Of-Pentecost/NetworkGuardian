package com.networkguardian.domain.models

/**
 * Best-effort device classification. NetworkGuardian only assigns a specific type when
 * available evidence (hostname hints, user-provided labels, etc.) reasonably supports it.
 * Otherwise it stays UNKNOWN — the app never guesses a manufacturer or model it can't verify.
 */
enum class DeviceType {
    ANDROID_PHONE,
    IPHONE,
    TABLET,
    LAPTOP,
    DESKTOP,
    SMART_TV,
    IOT_DEVICE,
    UNKNOWN
}
