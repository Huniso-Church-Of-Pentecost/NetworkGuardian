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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.networkguardian.domain.models.ConnectionEventType
import com.networkguardian.ui.viewmodel.GuardianViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: GuardianViewModel) {
    val devices by viewModel.devices.collectAsState()
    val history by viewModel.history.collectAsState()
    val state = viewModel.dashboardState()

    LazyColumn(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("NetworkGuardian", style = MaterialTheme.typography.titleLarge)
            Text(state.activeProfileName, style = MaterialTheme.typography.bodyLarge)
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🟢 Active", style = MaterialTheme.typography.titleMedium)
                    Text("${devices.size} devices connected")
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Trusted: ${state.trustedCount}")
                        Text("Unknown: ${state.unknownCount}")
                        Text("Blocked: ${state.blockedCount}")
                    }
                }
            }
        }

        item {
            Text("Recent Activity", style = MaterialTheme.typography.titleMedium)
        }

        if (history.isEmpty()) {
            item { Text("No activity recorded yet.", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(history.take(10)) { event ->
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.timestampEpochMs))
                Text("$time — ${describeEvent(event.eventType)}${event.deviceLabel.let { " ($it)" }}")
            }
        }
    }
}

private fun describeEvent(type: ConnectionEventType): String = when (type) {
    ConnectionEventType.DEVICE_DISCOVERED -> "New device detected"
    ConnectionEventType.DEVICE_CONNECTED -> "Device connected"
    ConnectionEventType.DEVICE_DISCONNECTED -> "Device disconnected"
    ConnectionEventType.DEVICE_TRUSTED -> "Device trusted"
    ConnectionEventType.DEVICE_UNTRUSTED -> "Trust removed"
    ConnectionEventType.DEVICE_BLOCKED -> "Device blocked"
    ConnectionEventType.DEVICE_UNBLOCKED -> "Device unblocked"
    ConnectionEventType.DEVICE_PAUSED -> "Device paused"
    ConnectionEventType.DEVICE_RESUMED -> "Device resumed"
    ConnectionEventType.NETWORK_CHANGED -> "Network changed"
    ConnectionEventType.HOTSPOT_ENABLED -> "Hotspot enabled"
    ConnectionEventType.HOTSPOT_DISABLED -> "Hotspot disabled"
}
