package com.networkguardian.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.networkguardian.ui.screens.DashboardScreen
import com.networkguardian.ui.screens.DeviceDetailsScreen
import com.networkguardian.ui.screens.DevicesScreen
import com.networkguardian.ui.screens.HistoryScreen
import com.networkguardian.ui.screens.PrivacyScreen
import com.networkguardian.ui.screens.SettingsScreen
import com.networkguardian.ui.screens.TrustedDevicesScreen
import com.networkguardian.ui.viewmodel.GuardianViewModel

private data class BottomTab(val screen: Screen, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun GuardianNavHost(viewModel: GuardianViewModel) {
    val navController = rememberNavController()
    val tabs = listOf(
        BottomTab(Screen.Dashboard, "Home", Icons.Filled.Home),
        BottomTab(Screen.Devices, "Devices", Icons.Filled.Devices),
        BottomTab(Screen.Trusted, "Trusted", Icons.Filled.VerifiedUser),
        BottomTab(Screen.History, "History", Icons.Filled.History),
        BottomTab(Screen.Settings, "Settings", Icons.Filled.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.screen.route } == true,
                        onClick = {
                            navController.navigate(tab.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(viewModel) }
            composable(Screen.Devices.route) {
                DevicesScreen(viewModel) { deviceId ->
                    navController.navigate(Screen.DeviceDetails.createRoute(deviceId))
                }
            }
            composable(Screen.DeviceDetails.route) { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
                DeviceDetailsScreen(viewModel, deviceId) { navController.popBackStack() }
            }
            composable(Screen.Trusted.route) { TrustedDevicesScreen(viewModel) }
            composable(Screen.History.route) { HistoryScreen(viewModel) }
            composable(Screen.Settings.route) { SettingsScreen(viewModel.capabilities.value) }
            composable(Screen.Privacy.route) { PrivacyScreen() }
        }
    }
}
