package com.networkguardian.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.networkguardian.domain.models.CapabilityStatus
import com.networkguardian.domain.models.DeviceCapabilities
import com.networkguardian.ui.components.CapabilityStatusBadge
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(capabilities: DeviceCapabilities) {
    LazyColumn(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.titleLarge) }

        item {
            Text("Diagnostics", style = MaterialTheme.typography.titleMedium)
            Text(
                "What this device actually allows NetworkGuardian to do, detected at startup.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        val rows = listOf(
            "Network discovery" to capabilities.canDiscoverDevices,
            "Connected device enumeration" to capabilities.canDiscoverDevices,
            "Network info reading" to capabilities.canReadNetworkInfo,
            "Network monitoring" to capabilities.canMonitorNetwork,
            "Hotspot control" to capabilities.canControlHotspot,
            "Hotspot client enumeration" to capabilities.canEnumerateHotspotClients,
            "Persistent block enforcement" to capabilities.canBlockDevice,
            "Unblock enforcement" to capabilities.canUnblockDevice
        )

        items(rows) { (label, status) ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label)
                    CapabilityStatusBadge(status)
                }
            }
        }

        item {
            Text(
                "NetworkGuardian never simulates a capability it doesn't actually have. " +
                    "\"Limited\" discovery means reachable devices on your local subnet are " +
                    "detected via standard network APIs — this is not full client enumeration, " +
                    "which no public Android API exposes to ordinary apps.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
