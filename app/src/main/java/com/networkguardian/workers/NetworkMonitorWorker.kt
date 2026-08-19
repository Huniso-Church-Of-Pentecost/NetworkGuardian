package com.networkguardian.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.networkguardian.AppGraph

/**
 * Periodic (WorkManager-scheduled, respects Doze/battery restrictions) discovery pass. Delegates
 * the actual reconciliation logic to DiscoveryReconciler, which is shared with the in-app
 * "Scan now" action so both paths behave identically.
 *
 * Note: WorkManager enforces a 15-minute minimum interval for periodic work — this worker is
 * the background safety net, not the primary way discovery happens. It must be scheduled at
 * app startup (see NetworkGuardianApp) or it will never run.
 */
class NetworkMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val graph = AppGraph.get(applicationContext)
        val profile = graph.activeProfileProvider.currentProfileId() ?: return Result.success()

        graph.discoveryReconciler.runOnce(profile)

        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "network_monitor_periodic"
    }
}
