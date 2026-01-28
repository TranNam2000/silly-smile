package com.jrm.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi

object InternetUtil {
    @RequiresApi(api = Build.VERSION_CODES.M)
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        if (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return true
        }
        if (actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return true
        }
        if (actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return true
        }
        return actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
    }
}