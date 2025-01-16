package com.aliabbas.aliabbasfiledownloader.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import com.aliabbas.aliabbasfiledownloader.db.DownloadEntity
import com.aliabbas.aliabbasfiledownloader.modal.FileDownloadState
import java.text.SimpleDateFormat
import java.util.*

object UtilFunctions {

    fun convertToViewState(downloadEntity: DownloadEntity): FileDownloadState {
        return FileDownloadState(
            id = downloadEntity.id,
            downloadUrl = downloadEntity.downloadUrl,
            destinationFolderUri = downloadEntity.downloadFolderUri ?: downloadEntity.tmpFolderUri,
            fullFileName = downloadEntity.fullFileName,
            mimeType = downloadEntity.mimeType,
            fileSize = downloadEntity.fileSize,
            wifiOnly = downloadEntity.wifiOnly,
            supportRange = downloadEntity.supportRange,
            fileStatus = downloadEntity.fileStatus,
            fileDownloadProgress = downloadEntity.fileDownloadProgress,
            interruptedBy = downloadEntity.interruptedBy,
            lastUpdatedAt = downloadEntity.lastUpdatedAt
        )
    }

    fun formatBytes(bytes: Long): String {
        return if (bytes < 1024) {
            "$bytes B"
        } else if (bytes < 1048576) {
            val kilobytes = bytes.toDouble() / 1024
            String.format("%.2f KB", kilobytes)
        } else if (bytes < 1073741824) {
            val megabytes = bytes.toDouble() / 1048576
            String.format("%.2f MB", megabytes)
        } else {
            val gigabytes = bytes.toDouble() / 1073741824
            String.format("%.2f GB", gigabytes)
        }
    }

     fun formatTime(timeMillis: Double): String {
        val seconds = (timeMillis / 1000).toInt() % 60
        val minutes = (timeMillis / (1000 * 60) % 60).toInt()
        val hours = (timeMillis / (1000 * 60 * 60)).toInt()

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }


    fun isInternetConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkCapabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                return networkCapabilities != null &&
                        (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            } else {
                val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
                return networkInfo != null && networkInfo.isConnected
            }
        }

        return false
    }

    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    fun convertTimestampToLocalDate(timestamp: Long): Date {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.time
    }

    fun getDay(date: Date): String {
        val today = Date()
        val yesterday = Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000))
        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        return when {
            isSameDay(date, today) -> ("Today")
            isSameDay(date, yesterday) -> ("Yesterday")
            else -> (dateFormatter.format(date))
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormatter.format(date1) == dateFormatter.format(date2)
    }

    fun saveIntToSharedPreferences(context: Context, key: String, value: Int) {
        val sharedPreferences =
            context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun getIntFromSharedPreferences(context: Context, key: String): Int {
        val sharedPreferences =
            context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getInt(key, 1)
    }

}