package com.aliabbas.aliabbasfiledownloader.broadcast

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.aliabbas.aliabbasfiledownloader.modal.FileAction
import com.aliabbas.aliabbasfiledownloader.utils.FILE_ACTION_DATA


object InternalAppBroadcast {

    fun messageToService(context: Context, action: Int, downloadId: Int? = null) {
        val messageIntent = Intent("${context.packageName}.MY_NOTIFICATION_BROADCAST_ALI")

        messageIntent.putExtra("action", action)
        messageIntent.putExtra("DOWNLOAD_ID", downloadId)

        LocalBroadcastManager.getInstance(context).sendBroadcast(messageIntent)

    }

    fun messageFromService(context: Context, fileAction: FileAction) {
        val messageIntent = Intent("${context.packageName}.MY_NOTIFICATION_BROADCAST_ALI")
        messageIntent.putExtra(FILE_ACTION_DATA, fileAction)
        Log.e("brs",fileAction.toString())
        LocalBroadcastManager.getInstance(context).sendBroadcast(messageIntent)

    }
}