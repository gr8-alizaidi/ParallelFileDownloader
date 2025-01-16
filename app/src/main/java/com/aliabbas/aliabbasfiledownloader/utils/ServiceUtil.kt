package com.aliabbas.aliabbasfiledownloader.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat.startForegroundService
import com.aliabbas.aliabbasfiledownloader.broadcast.InternalAppBroadcast
import com.aliabbas.aliabbasfiledownloader.service.FileDownloadService
import kotlinx.coroutines.delay

object ServiceUtil {

    suspend fun restartFileDownloadService(appContext: Context, action: Int) {

        startDownloadService(appContext)
        delay(1500)
        refreshFileDownloadService(appContext, action)
    }

    private fun refreshFileDownloadService(appContext: Context, action: Int) {
        InternalAppBroadcast.messageToService(context = appContext, action = action)
    }

    private fun startDownloadService(appContext: Context) {
        if (!isServiceRunning(appContext, FileDownloadService::class.java)) {
            val intent = Intent(appContext, FileDownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(appContext, intent)
            } else {
                appContext.startService(intent)
            }
        }
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses
        for (process in runningProcesses) {
            val serviceName = process.processName
            if (serviceName == serviceClass.name) {
                return true
            }
        }
        return false
    }
}