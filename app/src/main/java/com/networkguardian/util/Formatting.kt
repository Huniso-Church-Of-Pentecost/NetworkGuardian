package com.networkguardian.util

fun unavailableIfBlank(value: String?): String = if (value.isNullOrBlank()) "Unavailable" else value

fun formatDuration(millis: Long): String {
    if (millis < 0) return "Unavailable"
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
