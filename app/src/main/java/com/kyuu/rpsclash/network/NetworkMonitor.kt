package com.kyuu.rpsclash.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArraySet

class NetworkMonitor(context: Context) {

    interface NetworkListener {
        fun onNetworkAvailable()
        fun onNetworkLost()
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val listeners = CopyOnWriteArraySet<NetworkListener>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var isConnected: Boolean = checkInitialConnection()
        private set

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isConnected = true
                mainHandler.post {
                    listeners.forEach { it.onNetworkAvailable() }
                }
            }

            override fun onLost(network: Network) {
                isConnected = checkInitialConnection()
                if (!isConnected) {
                    mainHandler.post {
                        listeners.forEach { it.onNetworkLost() }
                    }
                }
            }
        })
    }

    private fun checkInitialConnection(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun addListener(listener: NetworkListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: NetworkListener) {
        listeners.remove(listener)
    }
}
