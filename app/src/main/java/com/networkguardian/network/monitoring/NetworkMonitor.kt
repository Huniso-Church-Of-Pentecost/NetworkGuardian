package com.networkguardian.network.monitoring

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class NetworkStateEvent {
    data object Available : NetworkStateEvent()
    data object Lost : NetworkStateEvent()
    data class CapabilitiesChanged(val hasInternet: Boolean, val isWifi: Boolean) : NetworkStateEvent()
}

/**
 * Wraps ConnectivityManager.NetworkCallback (the documented, non-privileged API) as a Flow.
 * Used to know when the active network appears/disappears/changes — legitimate signal for
 * "network changed" / "internet availability" history entries. Does not enumerate other clients.
 */
class NetworkMonitor(private val context: Context) {

    fun observe(): Flow<NetworkStateEvent> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkStateEvent.Available)
            }

            override fun onLost(network: Network) {
                trySend(NetworkStateEvent.Lost)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                trySend(NetworkStateEvent.CapabilitiesChanged(hasInternet, isWifi))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, callback)

        awaitClose { cm.unregisterNetworkCallback(callback) }
    }
}
