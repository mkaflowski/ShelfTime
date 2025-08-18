package kaf.audiobookshelfwearos.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class NetworkConnectivityManager(
    private val context: Context,
    private val onConnectivityRestored: suspend () -> Unit
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var isConnected = false
    private var wasOffline = false
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            val wasOfflineBefore = !isConnected
            isConnected = true
            
            if (wasOfflineBefore && wasOffline) {
                Timber.d("Network connectivity restored - triggering sync")
                CoroutineScope(Dispatchers.IO).launch {
                    onConnectivityRestored()
                }
                wasOffline = false
            }
        }
        
        override fun onLost(network: Network) {
            super.onLost(network)
            isConnected = false
            wasOffline = true
            Timber.d("Network connectivity lost")
        }
        
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (hasInternet && !isConnected) {
                onAvailable(network)
            }
        }
    }
    
    fun startMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            isConnected = isNetworkAvailable()
            Timber.d("Network monitoring started - initial state: connected=$isConnected")
        } catch (e: Exception) {
            Timber.e(e, "Failed to register network callback")
        }
    }
    
    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Timber.d("Network monitoring stopped")
        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister network callback")
        }
    }
    
    fun isNetworkAvailable(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            Timber.e(e, "Error checking network availability")
            false
        }
    }
}
