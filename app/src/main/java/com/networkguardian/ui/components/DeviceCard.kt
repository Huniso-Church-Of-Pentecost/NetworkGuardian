package com.networkguardian.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.spacedBy
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.networkguardian.domain.models.NetworkDevice
import com.networkguardian.util.formatDuration
import com.networkguardian.util.unavailableIfBlank

@Composable
fun DeviceCard(
    device: NetworkDevice,
    nowEpochMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        onClick = onClick
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = device.friendlyName ?: "Unnamed device",
                    style = MaterialTheme.typography.titleMedium
                )
                TrustStateBadge(device.trustState)
            }

            Text(
                text = "IP: ${unavailableIfBlank(device.ipAddress)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "MAC: ${unavailableIfBlank(device.macAddress)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (device.isCurrentlyReachable) {
                    "Connected: ${formatDuration(nowEpochMs - device.firstSeenEpochMs)}"
                } else {
                    "Last seen: ${formatDuration(nowEpochMs - device.lastSeenEpochMs)} ago"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
