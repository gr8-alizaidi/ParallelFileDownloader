package com.aliabbas.aliabbasfiledownloader.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aliabbas.aliabbasfiledownloader.R
import com.aliabbas.aliabbasfiledownloader.broadcast.NotificationActionReceiver
import com.aliabbas.aliabbasfiledownloader.modal.NotifInfo
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.CANCEL
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.DOWNLOADED
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.PAUSE
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.RESUME
import dagger.hilt.android.qualifiers.ApplicationContext

class NotificationHelper(context: Context) : ContextWrapper(context) {


    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showCustomNotification(notifInfo: NotifInfo, context: Context) {
        val contentView = RemoteViews(context.packageName, R.layout.custom_notification_layout)

        if (notifInfo.eta != null) {
            contentView.setViewVisibility(R.id.eta_field, VISIBLE)
            contentView.setTextViewText(R.id.eta_field, notifInfo.eta)
        } else {
            contentView.setViewVisibility(R.id.eta_field, GONE)
        }
        contentView.setTextViewText(R.id.file_name_field, notifInfo.title)
        contentView.setTextViewText(R.id.action_field, notifInfo.action)

        if (notifInfo.dSpeed != null) {
            contentView.setViewVisibility(R.id.icon_dot, VISIBLE)
            contentView.setViewVisibility(R.id.download_speed_field, VISIBLE)
            contentView.setTextViewText(R.id.download_speed_field, notifInfo.dSpeed)
        } else {
            contentView.setViewVisibility(R.id.icon_dot, GONE)
            contentView.setViewVisibility(R.id.download_speed_field, GONE)
        }

        if (notifInfo.progress >= 0) {
            contentView.setProgressBar(R.id.notif_progressBar, 100, notifInfo.progress, false)
        } else {
            contentView.setProgressBar(R.id.notif_progressBar, 100, 100, true)
        }

        when (notifInfo.dStatus) {
            PAUSE -> {
                contentView.setViewVisibility(R.id.pause_txt, GONE)
                contentView.setViewVisibility(R.id.resume_txt, VISIBLE)
            }
            else -> {
                contentView.setViewVisibility(R.id.resume_txt, GONE)
                contentView.setViewVisibility(R.id.pause_txt, VISIBLE)
            }
        }

        contentView.setOnClickPendingIntent(
            R.id.pause_txt,
            getPendingIntent(context, "DOWNLOAD_PAUSE", notifInfo.id)
        )
        contentView.setOnClickPendingIntent(
            R.id.resume_txt,
            getPendingIntent(context, "DOWNLOAD_RESUME", notifInfo.id)
        )
        contentView.setOnClickPendingIntent(
            R.id.cancel_txt,
            getPendingIntent(context, "DOWNLOAD_CANCEL", notifInfo.id)
        )

        val channelId = notifInfo.id.toString()
        val channelName = "File Download Notification"

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_launcher_foreground
                )
            )
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(contentView)
            .setOngoing(true)

        if (notifInfo.dStatus == DOWNLOADED || notifInfo.dStatus == PAUSE) {
            notificationBuilder.setOngoing(false)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(notificationChannel)
        }

        val notification = notificationBuilder.build()

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        notificationManagerCompat.notify(notifInfo.id, notification)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getPendingIntent(
        context: Context,
        action: String,
        notificationId: Int
    ): PendingIntent? {
        val intent = Intent(context, NotificationActionReceiver::class.java)
        intent.action = action
        intent.putExtra("DOWNLOAD_ID", notificationId)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

}
