package com.jkandcoding.android.myapplication.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

class ConnectivityCheckingInterceptor @Inject constructor(
    private val connectivityManager: ConnectivityManager
) : Interceptor, ConnectivityManager.NetworkCallback() {

    private var online = false

    init {
        if (Build.VERSION.SDK_INT >= 24) {
            connectivityManager.registerDefaultNetworkCallback(this)
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (Build.VERSION.SDK_INT < 24) {
            online = connectivityManager.activeNetworkInfo?.isConnected ?: false
        }

        if (online) {
            return chain.proceed(chain.request())
        } else {
            throw IOException("Internet connection is unavailable")
        }
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        online = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}