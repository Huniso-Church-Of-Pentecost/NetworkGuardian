package com.networkguardian.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.networkguardian.domain.models.TrustState
import com.networkguardian.ui.viewmodel.GuardianViewModel

@Composable
fun TrustedDevicesScreen(viewModel: GuardianViewModel) {
    val devices by viewModel.devices.collectAsState()
    val trusted = devices.filter { it.trustState == TrustState.TRUSTED }

    if (trusted.isEmpty()) {
        Text("No trusted devices yet. Trust a device from its details page.", modifier = Modifier.padding(24.dp))
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        items(trusted, key = { it.id }) { device ->
            ListItem(headlineContent = { Text("✓ ${device.friendlyName ?: device.id}") })
        }
    }
}
