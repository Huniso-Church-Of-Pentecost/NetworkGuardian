package com.networkguardian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.networkguardian.domain.models.CapabilityStatus
import com.networkguardian.domain.models.TrustState
import com.networkguardian.ui.theme.GuardianAmber
import com.networkguardian.ui.theme.GuardianGray
import com.networkguardian.ui.theme.GuardianGreen
import com.networkguardian.ui.theme.GuardianRed

@Composable
fun StatusBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun TrustStateBadge(state: TrustState, modifier: Modifier = Modifier) {
    val (label, color) = when (state) {
        TrustState.TRUSTED -> "Trusted" to GuardianGreen
        TrustState.BLOCKED -> "Blocked" to GuardianRed
        TrustState.UNKNOWN -> "Unknown" to GuardianAmber
    }
    StatusBadge(label, color, modifier)
}

@Composable
fun CapabilityStatusBadge(status: CapabilityStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        CapabilityStatus.SUPPORTED -> "Supported" to GuardianGreen
        CapabilityStatus.LIMITED -> "Limited" to GuardianAmber
        CapabilityStatus.NOT_AVAILABLE -> "Not available" to GuardianGray
    }
    StatusBadge(label, color, modifier)
}
