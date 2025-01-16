package com.aliabbas.aliabbasfiledownloader.di

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.aliabbas.aliabbasfiledownloader.R
import com.aliabbas.aliabbasfiledownloader.utils.NOTIFICATION_CHANNEL_ID
import com.aliabbas.aliabbasfiledownloader.utils.NotificationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped


@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {


    @ServiceScoped
    @Provides
    fun provideBaseNotificationBuilder(
        @ApplicationContext app: Context
    ) = NotificationCompat.Builder(app, NOTIFICATION_CHANNEL_ID)
        .setAutoCancel(false)
        .setOngoing(true)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Searching pending downloads")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationManager.IMPORTANCE_MIN)
        .setCategory(Notification.CATEGORY_SERVICE)

    @ServiceScoped
    @Provides
    fun proivdeNotificationHelper(@ApplicationContext app: Context) = NotificationHelper(app)
}