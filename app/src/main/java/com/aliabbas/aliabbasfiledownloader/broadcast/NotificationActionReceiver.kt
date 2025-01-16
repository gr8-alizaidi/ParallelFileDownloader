package com.aliabbas.aliabbasfiledownloader.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aliabbas.aliabbasfiledownloader.modal.FileAction
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.CANCEL
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.NOTIF_REMOVE
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.PAUSE
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.RESUME

class NotificationActionReceiver : BroadcastReceiver() {

    private val TAG = "NotificationActionReceiver"

    override fun onReceive(context: Context, intent: Intent?) {

        if (intent != null) {

            if (intent.action == "DOWNLOAD_PAUSE"
                || intent.action == "DOWNLOAD_RESUME"
                || intent.action == "DOWNLOAD_CANCEL"
            ) {

                val downloadId = intent.getIntExtra("DOWNLOAD_ID", -1)
                Log.e(TAG, "NotificationActionReceiver onReceive $downloadId[${intent.action!!}]")
                if (downloadId != -1) {
                    val action = getAction(intent.action!!)
                    InternalAppBroadcast.messageToService(
                        context = context,
                        action = action,
                        downloadId = downloadId
                    )
                } else Log.e(TAG, "download id not found from pendingIntent action!")
            }
        }
    }

    private fun getAction(action: String): Int {
        return when (action) {
            "DOWNLOAD_PAUSE" -> PAUSE
            "DOWNLOAD_RESUME" -> RESUME
            else -> NOTIF_REMOVE
        }
    }

}