package com.networkguardian

import android.app.Application
import com.networkguardian.notifications.NotificationChannels

class NetworkGuardianApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        // Touch AppGraph early so first screen render doesn't pay DB-open latency.
        AppGraph.get(this)
    }
}
