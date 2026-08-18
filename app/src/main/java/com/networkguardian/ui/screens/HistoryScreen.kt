package com.networkguardian.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.networkguardian.ui.viewmodel.GuardianViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: GuardianViewModel) {
    val history by viewModel.history.collectAsState()
    val formatter = remember(history) { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Network History", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { viewModel.clearHistory() }) { Text("Clear") }
        }

        if (history.isEmpty()) {
            Text("No history yet.", modifier = Modifier.padding(top = 16.dp))
        } else {
            LazyColumn {
                items(history) { event ->
                    Text("${formatter.format(Date(event.timestampEpochMs))} — ${event.eventType.name.replace('_', ' ').lowercase()} (${event.deviceLabel})")
                }
            }
        }
    }
}
