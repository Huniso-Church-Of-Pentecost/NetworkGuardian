package com.networkguardian.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Devices : Screen("devices")
    data object DeviceDetails : Screen("device_details/{deviceId}") {
        fun createRoute(deviceId: String) = "device_details/$deviceId"
    }
    data object Trusted : Screen("trusted")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object Privacy : Screen("privacy")
}
