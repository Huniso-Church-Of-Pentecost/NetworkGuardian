package com.networkguardian.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.networkguardian.ui.components.DeviceCard
import com.networkguardian.ui.viewmodel.GuardianViewModel

@Composable
fun DevicesScreen(viewModel: GuardianViewModel, onDeviceClick: (String) -> Unit) {
    val devices by viewModel.devices.collectAsState()
    val now = System.currentTimeMillis()

    if (devices.isEmpty()) {
        Text(
            "No devices detected yet. Discovery runs periodically while monitoring is enabled.",
            modifier = Modifier.padding(24.dp)
        )
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.Top) {
        items(devices, key = { it.id }) { device ->
            DeviceCard(device = device, nowEpochMs = now, onClick = { onDeviceClick(device.id) })
        }
    }
}
