package com.aliabbas.aliabbasfiledownloader.broadcast

import android.content.Context
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.util.Log
import com.aliabbas.aliabbasfiledownloader.utils.DownloadInterruption.CELLULAR
import com.aliabbas.aliabbasfiledownloader.utils.DownloadInterruption.WIFI
import com.aliabbas.aliabbasfiledownloader.utils.UtilFunctions.isWifiConnected


class NetworkChangeCallback(val context: Context) : NetworkCallback() {
    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        // Internet connection is available
        if(isWifiConnected(context))
            InternalAppBroadcast.messageToService(context,WIFI)
        else
            InternalAppBroadcast.messageToService(context, CELLULAR)
        Log.e("NetworkChangeCallback", "Internet connection is back")
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        // Internet connection is lost
        Log.e("NetworkChangeCallback", "Internet connection is lost")
    }
}
