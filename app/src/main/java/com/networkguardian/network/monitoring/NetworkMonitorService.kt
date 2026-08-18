package com.networkguardian.network.monitoring

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.networkguardian.R
import com.networkguardian.notifications.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * A minimal foreground service used ONLY while the user has active monitoring enabled, per
 * Android's background execution restrictions. It performs local discovery/reachability
 * checks at the user-configured interval; it does not run unbounded background work and
 * stops immediately when the user disables monitoring or leaves the app if monitoring was
 * left off.
 */
class NetworkMonitorService : Service() {

    private var job: Job = SupervisorJob()
    private lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(job)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, NotificationChannels.MONITORING_CHANNEL_ID)
            .setContentTitle(getString(R.string.monitoring_notification_title))
            .setContentText(getString(R.string.monitoring_notification_text))
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .build()

        startForeground(MONITOR_NOTIFICATION_ID, notification)

        // Actual periodic discovery work is delegated to NetworkMonitorWorker via WorkManager,
        // which is better suited to respecting Doze/App Standby than a long-lived loop here.
        // This service's role is limited to keeping the user explicitly informed that
        // monitoring is active, as required by Android's foreground service transparency rules.

        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val MONITOR_NOTIFICATION_ID = 42
    }
}
