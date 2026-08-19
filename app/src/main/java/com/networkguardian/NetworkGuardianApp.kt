package com.networkguardian

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.networkguardian.notifications.NotificationChannels
import com.networkguardian.workers.NetworkMonitorWorker
import java.util.concurrent.TimeUnit

class NetworkGuardianApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        // Touch AppGraph early so first screen render doesn't pay DB-open latency.
        AppGraph.get(this)

        scheduleBackgroundMonitoring()
    }

    /**
     * WorkManager enforces a 15-minute floor for periodic work, so this is a background
     * safety net rather than the primary discovery path — the in-app "Scan now" action (see
     * DashboardScreen) triggers an immediate pass without that floor. Without this call the
     * worker class exists but never actually runs.
     */
    private fun scheduleBackgroundMonitoring() {
        val request = PeriodicWorkRequestBuilder<NetworkMonitorWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            NetworkMonitorWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
