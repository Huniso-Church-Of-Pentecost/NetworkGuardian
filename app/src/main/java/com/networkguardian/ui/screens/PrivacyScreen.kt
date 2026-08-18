package com.networkguardian.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.spacedBy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Privacy", style = MaterialTheme.typography.titleLarge)
        Text("• Device and network information is stored locally on your device by default.")
        Text("• No cloud account is required to use NetworkGuardian.")
        Text("• No network data is uploaded by the core app.")
        Text("• The app only performs network-management actions that you initiate and confirm.")
        Text("• You are responsible for only monitoring/managing networks you own or are explicitly authorized to administer.")
    }
}
