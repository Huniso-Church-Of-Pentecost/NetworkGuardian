package com.networkguardian.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.spacedBy
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LockScreen(onUnlock: (String) -> Boolean, onBiometricRequested: () -> Unit, biometricAvailable: Boolean) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("NetworkGuardian is locked", style = MaterialTheme.typography.titleLarge)
        Text("Enter your PIN to continue", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it; error = null },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.padding(top = 16.dp)
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = { if (!onUnlock(pin)) error = "Incorrect PIN" },
            modifier = Modifier.padding(top = 16.dp)
        ) { Text("Unlock") }

        if (biometricAvailable) {
            Button(onClick = onBiometricRequested, modifier = Modifier.padding(top = 8.dp)) {
                Text("Use biometrics")
            }
        }
    }
}
