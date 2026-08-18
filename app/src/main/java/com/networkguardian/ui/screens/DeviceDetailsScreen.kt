package com.networkguardian.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.spacedBy
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.networkguardian.domain.models.OperationResult
import com.networkguardian.domain.models.TrustState
import com.networkguardian.ui.components.ConfirmDialog
import com.networkguardian.ui.components.TrustStateBadge
import com.networkguardian.ui.viewmodel.GuardianViewModel
import com.networkguardian.util.unavailableIfBlank

@Composable
fun DeviceDetailsScreen(viewModel: GuardianViewModel, deviceId: String, onBack: () -> Unit) {
    val devices by viewModel.devices.collectAsState()
    val device = devices.firstOrNull { it.id == deviceId }

    var showBlockConfirm by remember { mutableStateOf(false) }
    var showUnblockConfirm by remember { mutableStateOf(false) }
    var lastResultMessage by remember { mutableStateOf<String?>(null) }

    if (device == null) {
        Text("Device no longer available.", modifier = Modifier.padding(24.dp))
        return
    }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(device.friendlyName ?: "Unnamed device", style = MaterialTheme.typography.titleLarge)
            TrustStateBadge(device.trustState)
        }

        Text("IP: ${unavailableIfBlank(device.ipAddress)}")
        Text("MAC: ${unavailableIfBlank(device.macAddress)}")
        Text("Type: ${device.deviceType.name}")
        Text("First seen: ${unavailableIfBlank(device.firstSeenEpochMs.toString())}")
        Text("Last seen: ${unavailableIfBlank(device.lastSeenEpochMs.toString())}")
        Text("Reachable now: ${if (device.isCurrentlyReachable) "Yes" else "No"}")

        lastResultMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (device.trustState == TrustState.TRUSTED) {
                OutlinedButton(onClick = { viewModel.untrustDevice(device) }) { Text("Remove trust") }
            } else {
                Button(onClick = { viewModel.trustDevice(device) }) { Text("Trust") }
            }

            if (device.trustState == TrustState.BLOCKED) {
                OutlinedButton(onClick = { showUnblockConfirm = true }) { Text("Unblock") }
            } else {
                OutlinedButton(onClick = { showBlockConfirm = true }) { Text("Block") }
            }
        }

        OutlinedButton(onClick = {
            viewModel.forgetDevice(device)
            onBack()
        }) { Text("Forget device") }
    }

    if (showBlockConfirm) {
        ConfirmDialog(
            title = "Block this device?",
            message = "This device will be added to your permanent blocklist. Enforcement depends on the capabilities provided by your Android device/network.",
            confirmLabel = "Block",
            onConfirm = {
                showBlockConfirm = false
                viewModel.blockDevice(device, null) { result ->
                    lastResultMessage = describeResult(result)
                }
            },
            onDismiss = { showBlockConfirm = false }
        )
    }

    if (showUnblockConfirm) {
        ConfirmDialog(
            title = "Unblock this device?",
            message = "This removes the device from your blocklist and, where supported, removes any network-level restriction.",
            confirmLabel = "Unblock",
            onConfirm = {
                showUnblockConfirm = false
                viewModel.unblockDevice(device) { result ->
                    lastResultMessage = describeResult(result)
                }
            },
            onDismiss = { showUnblockConfirm = false }
        )
    }
}

private fun describeResult(result: OperationResult): String = when (result) {
    is OperationResult.Success -> "Done."
    is OperationResult.Failed -> "Failed: ${result.reason}"
    is OperationResult.Unsupported -> result.explanation
    is OperationResult.PermissionDenied -> "Permission needed: ${result.permission}"
}
