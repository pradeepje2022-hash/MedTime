package com.jpinger.pro

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object NetUtil {

    fun name(caps: NetworkCapabilities?): String = when {
        caps == null -> "Offline"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        else -> "Other"
    }

    @Suppress("DEPRECATION")
    fun current(ctx: Context): String {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= 23) {
                val n = cm.activeNetwork
                if (n == null) "Offline" else name(cm.getNetworkCapabilities(n))
            } else {
                val info = cm.activeNetworkInfo
                if (info != null && info.isConnected) (info.typeName ?: "Online") else "Offline"
            }
        } catch (e: SecurityException) {
            "—"
        }
    }
}
