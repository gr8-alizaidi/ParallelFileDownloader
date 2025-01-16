package com.aliabbas.aliabbasfiledownloader.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.room.util.convertByteToUUID
import com.aliabbas.aliabbasfiledownloader.broadcast.InternalAppBroadcast
import com.aliabbas.aliabbasfiledownloader.broadcast.NetworkChangeCallback
import com.aliabbas.aliabbasfiledownloader.db.DownloadEntity
import com.aliabbas.aliabbasfiledownloader.interfaces.DownloadInterface
import com.aliabbas.aliabbasfiledownloader.modal.FileAction
import com.aliabbas.aliabbasfiledownloader.modal.NotifInfo
import com.aliabbas.aliabbasfiledownloader.processHelper.FileDownloader
import com.aliabbas.aliabbasfiledownloader.repositories.MainRepository
import com.aliabbas.aliabbasfiledownloader.utils.*
import com.aliabbas.aliabbasfiledownloader.utils.DownloadInterruption.CELLULAR
import com.aliabbas.aliabbasfiledownloader.utils.DownloadInterruption.WIFI
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.CANCEL
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.DOWNLOADED
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.DOWNLOADING
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.FAILED
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.NOTIF_REMOVE
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.PAUSE
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.PAUSE_ALL
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.RESUME
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.RESUME_ALL
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.START
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.WAITING
import com.aliabbas.aliabbasfiledownloader.utils.FileUtils.getUriFromSharedPreferences
import com.aliabbas.aliabbasfiledownloader.utils.UtilFunctions.formatBytes
import com.aliabbas.aliabbasfiledownloader.utils.UtilFunctions.getIntFromSharedPreferences
import com.aliabbas.aliabbasfiledownloader.utils.UtilFunctions.isInternetConnected
import com.aliabbas.aliabbasfiledownloader.utils.UtilFunctions.isWifiConnected
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject


@AndroidEntryPoint
class FileDownloadService : Service() {
    @Inject
    lateinit var mainRepository: MainRepository
    @Inject
    lateinit var fileDownloader: FileDownloader
    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder
    @Inject
    lateinit var notificationHelper: NotificationHelper
    private var networkChangeCallback: NetworkChangeCallback? = null

    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + job)

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val filter = IntentFilter("${baseContext?.packageName}.MY_NOTIFICATION_BROADCAST_ALI")
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)

        networkChangeCallback = NetworkChangeCallback(this@FileDownloadService)
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(networkRequest, networkChangeCallback!!)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                baseNotificationBuilder.build(),
                FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }


    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val data = intent?.getIntExtra("action", -1)
            if (context != null && data != -1) {
                when (data) {
                    WIFI -> {
                        Log.e("Service", "WIFI")
                        putWifiFilesToWaiting()
                        startAnotherDownload()
                    }
                    CELLULAR -> {
                        Log.e("Service", "Cellular")
                        putCellularFilesToWaiting()
                        startAnotherDownload()
                    }
                    RESUME -> {
                        Log.e("downloadService", "resume")
                        val downloadId = intent.getIntExtra("DOWNLOAD_ID", -1)
                        if (downloadId != -1) {
                            startDownloading(downloadId)
                        }
                    }
                    PAUSE -> {
                        val downloadId = intent.getIntExtra("DOWNLOAD_ID", -1)
                        Log.e("Service", "pause $downloadId")

                        if (downloadId != -1) {
                            pauseFileDownload(downloadId)
                        }
                    }
                    PAUSE_ALL -> {
                        Log.e("Service", "pause all")
                        pauseAllDownloads()
                    }
                    CANCEL -> {
                        Log.e("Service", "cancel")
                        val downloadId = intent.getIntExtra("DOWNLOAD_ID", -1)
                        if (downloadId != -1) {
                            cancelDownload(context, downloadId)
                        }
                    }
                    RESUME_ALL -> {
                        Log.e("Service", "Resume All")

                        //setAllToWaiting
                        resumeAllFiles()

                        startAllDownload()
                    }
                    NOTIF_REMOVE -> {
                        Log.e("Service","notif_remove")
                        val downloadId = intent.getIntExtra("DOWNLOAD_ID", -1)
                        if (downloadId != -1) {
                            removeDownload(downloadId)
                        }
                    }
                    else -> {
                        Log.e("Service", "WRDRF")
                        if (isInternetConnected(this@FileDownloadService))
                            startAnotherDownload()
                    }
                }
            }
        }


        private fun pauseFileDownload(downloadId: Int) {
            fileDownloader.pauseDownload(downloadId)
        }

        private fun pauseAllDownloads() = fileDownloader.pauseAllDownload()

        private fun cancelDownload(context: Context, downloadId: Int) {
            coroutineScope.launch {
                if (fileDownloader.getFileEntity(downloadId) == null) {
                    mainRepository.deleteDownload(downloadId)
                    delay(200)
                    refereshAdapter()
                } else
                    fileDownloader.cancelDownload(context, downloadId)
            }
        }


        private fun startDownloading(downloadId: Int) {
            coroutineScope.launch {
                val downloadEntity = fileDownloader.getFileEntity(downloadId)
                delay(100)
                if(downloadEntity == null) {
                    refereshAdapter()
                }
                if (getAvailableParallelLine() > 0) {
//                    if (!isInternetConnected(this@FileDownloadService)) {
//                        downloadEntity!!.fileStatus = WAITING
//                        downloadEntity.lastUpdatedAt = System.currentTimeMillis()
//                        mainRepository.addFileToDownload(downloadEntity)
//                        delay(200)
//                        return@launch
//                    }
//                    if (downloadEntity!!.wifiOnly) {
//                        val isWifiConnected = isWifiConnected(this@FileDownloadService)
//                        if (!isWifiConnected) {
//                            downloadEntity.fileStatus = PAUSE
//                            downloadEntity.interruptedBy = WIFI
//                            downloadEntity.lastUpdatedAt = System.currentTimeMillis()
//                            mainRepository.addFileToDownload(downloadEntity)
//                            delay(200)
//                            return@launch
//                        }
//                    }
                    downloadFileFromNetwork(this@FileDownloadService, downloadEntity!!)
                } else {
                    downloadEntity!!.fileStatus = WAITING
                    downloadEntity.lastUpdatedAt = System.currentTimeMillis()
                    mainRepository.addFileToDownload(downloadEntity)
                    return@launch
                }
            }
        }
    }

    private fun refereshAdapter() {
        val fileAction = FileAction(
            fileId = -1,
            downloadProgress = 0,
            action = "Update All",
            isIndeterminate = false,
            fileStatus = UPDATE_ALL
        )
        InternalAppBroadcast.messageFromService(
            context = this@FileDownloadService,
            fileAction = fileAction
        )
        Log.e("refereshing","Service to adapter")
    }

    private fun putCellularFilesToWaiting() {
        coroutineScope.launch {
            val numUpdation = mainRepository.putCellInterruptedFilesToWaiting(System.currentTimeMillis())
            delay(200)
            if(numUpdation > 0) {
                refereshAdapter()
                delay(200)
            }
        }
    }

    private fun putWifiFilesToWaiting() {
        coroutineScope.launch {
            val numUpdation = mainRepository.putInterruptediFilesToWaiting(System.currentTimeMillis())
            delay(200)
            if(numUpdation > 0) {
                refereshAdapter()
                delay(200)
            }
        }
    }

    private fun removeDownload(downloadId: Int) {
        val notificationManager = (this@FileDownloadService).getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(downloadId)
        val fileAction = FileAction(
            fileId = downloadId,
            downloadProgress = 0,
            action = "Download Cancelled",
            isIndeterminate = false,
            fileStatus = CANCEL
        )
        InternalAppBroadcast.messageFromService(
            context = this@FileDownloadService,
            fileAction = fileAction
        )

    }

    private fun resumeAllFiles() {
        coroutineScope.launch {
            fileDownloader.resumeAllFiles()
            delay(200)
        }
    }

    private fun startAllDownload() {
        coroutineScope.launch {
            var filesWaiting = mainRepository.getRemainingFiles()
            delay(200)
            val index = 0
            while (getAvailableParallelLine() > 0 && filesWaiting.isNotEmpty()) {
                val entity = filesWaiting[index]
//                if (isInternetConnected(this@FileDownloadService)) {
//                    if (entity.wifiOnly && isWifiConnected(this@FileDownloadService)) {
                        downloadFileFromNetwork(this@FileDownloadService, entity)
//                    } else if (!entity.wifiOnly) {
//                        downloadFileFromNetwork(this@FileDownloadService, entity)
//                    }
                    filesWaiting = mainRepository.getRemainingFiles()
                    delay(200)
//                } else return@launch
            }
        }
    }

    fun startAnotherDownload() {
        coroutineScope.launch {

            var filesWaiting = mainRepository.getFilesInQueue()
            delay(200)

            val index = 0
            while (getAvailableParallelLine() > 0 && filesWaiting.isNotEmpty()) {
                val entity = filesWaiting[index]
                if(entity.wifiOnly && entity.interruptedBy == WIFI && !isWifiConnected(this@FileDownloadService))
                    continue
//                if (isInternetConnected(this@FileDownloadService)) {
//                    if (entity.wifiOnly && isWifiConnected(this@FileDownloadService)) {
//                        downloadFileFromNetwork(this@FileDownloadService, entity)
//                    } else if (!entity.wifiOnly) {
                        downloadFileFromNetwork(this@FileDownloadService, entity)
//                    }
                    filesWaiting = mainRepository.getFilesInQueue()
                    delay(200)
//                } else return@launch
            }
        }
    }

    private suspend fun getAvailableParallelLine(): Int {
        val maxParallelGiven = getIntFromSharedPreferences(this@FileDownloadService, "MAX_PARALLEL")
        val currentlyDownloading = mainRepository.getCurrentlyDownloadingFile()
        delay(200)

        Log.e("availableParallel","$maxParallelGiven ----   ${currentlyDownloading.size}")
        return (maxParallelGiven - currentlyDownloading.size)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.unregisterNetworkCallback(networkChangeCallback!!)
    }

    private suspend fun downloadFileFromNetwork(
        appContext: Context,
        downloadEntity: DownloadEntity
    ) {
        val targetUri = getUriFromSharedPreferences(appContext, FILE_URI)!!
        val outputFile = DocumentFile.fromTreeUri(this, targetUri) ?: return
        val tmpFile = outputFile.findFile(downloadEntity.fullFileName)
        val dFile =
            if (downloadEntity.supportRange && tmpFile != null) tmpFile
            else outputFile.createFile(downloadEntity.mimeType, downloadEntity.fullFileName)
        Log.e(
            "dwnld",
            "${downloadEntity.supportRange} && ${downloadEntity.fileDownloadProgress} > 0 && $dFile "
        )
        if (dFile == null) {
            Log.e("error", dFile.toString())
            return
        }
        Log.e("dffn", "${downloadEntity.id}")

        if (dFile.exists()) {
            Log.e("dffn", "2 - ${downloadEntity.id}")

            fileDownloader.downloadFile(
                this@FileDownloadService,
                downloadEntity,
                dFile,
                object : DownloadInterface {

                    override fun onDownloadStarted(
                        initialProgress: Int,
                        downloadEntity: DownloadEntity
                    ) {
                        Log.e(
                            "Service",
                            "onDownloadStarted() => progress => $initialProgress, downloadId => ${downloadEntity.id}"
                        )
                        val downloadSpeed = "0 Mbps"
                        var action = "0 MB of 0 MB"
                        if (downloadEntity.fileSize > 0) {
                            action = "0 MB of ${downloadEntity.fileSize} MB"
                        }
                        val notificationData = NotifInfo(
                            id = downloadEntity.id,
                            dStatus = DOWNLOADING,
                            title = downloadEntity.fullFileName,
                            progress = initialProgress,
                            dSpeed = downloadSpeed,
                            action = action,
                            eta = "0:00"
                        )
                        val fileAction = FileAction(
                            fileId = downloadEntity.id,
                            downloadProgress = 0,
                            action = "Waiting in queue",
                            isIndeterminate = downloadEntity.fileDownloadProgress == -1,
                            fileStatus = WAITING
                        )
                        InternalAppBroadcast.messageFromService(
                            context = appContext,
                            fileAction = fileAction
                        )
                        notificationHelper.showCustomNotification(
                            notificationData,
                            this@FileDownloadService
                        )
                    }

                    override fun onDownloadPaused(
                        downloadEntity: DownloadEntity,
                        downloadSpeed: String,
                        downloadProgress: Int,
                        ETA: String,
                        interruptedBy : Int?
                    ) {
                        Log.e(
                            "Service",
                            "onDownloadPaused() => downloadId => ${downloadEntity.id}"
                        )
                        val notificationData = NotifInfo(
                            id = downloadEntity.id,
                            dStatus = PAUSE,
                            title = downloadEntity.fullFileName,
                            progress = downloadProgress,
                            dSpeed = downloadSpeed,
                            action = "Paused",
                            eta = "0:00"
                        )

                        val fileAction = FileAction(
                            fileId = downloadEntity.id,
                            downloadProgress = downloadProgress,
                            action = "Paused",
                            isIndeterminate = downloadEntity.fileDownloadProgress == -1,
                            fileStatus = PAUSE,
                            interruptedBy = interruptedBy
                        )
                        InternalAppBroadcast.messageFromService(
                            context = appContext,
                            fileAction = fileAction
                        )
                        notificationHelper.showCustomNotification(
                            notificationData,
                            this@FileDownloadService
                        )
                        coroutineScope.launch {
                            delay(200)
                            startAnotherDownload()
                        }
                    }

                    override fun onDownloadCancel(downloadEntity: DownloadEntity) {
                        Log.e(
                            "Service",
                            "onDownloadCancel() => downloadId => ${downloadEntity.id}"
                        )
                        coroutineScope.launch {
                            val notificationManager =
                                getSystemService(Context.NOTIFICATION_SERVICE)
                                        as NotificationManager
                            notificationManager.cancel(downloadEntity.id)

                            refereshAdapter()
                            InternalAppBroadcast.messageToService(
                                context = appContext,
                                action = START
                            )
                            delay(200)
                            startAnotherDownload()
                        }
                    }

                    override fun onProgressUpdate(
                        progress: Int,
                        eta: String,
                        speed: String,
                        copiedBytes: Long,
                        totalBytes: Long,
                        downloadEntity: DownloadEntity
                    ) {
                        coroutineScope.launch {
                            Log.e(
                                "Service",
                                "onProgressUpdate: $progress, downloadId => ${downloadEntity.id}, speed => ${speed}, eta => $eta"
                            )
                            val action = if (totalBytes > 0) "${formatBytes(copiedBytes)} of ${
                                formatBytes(totalBytes)
                            }" else formatBytes(copiedBytes)
                            val notificationData = NotifInfo(
                                id = downloadEntity.id,
                                dStatus = DOWNLOADING,
                                title = downloadEntity.fullFileName,
                                progress = progress,
                                dSpeed = speed,
                                action = action,
                                eta = eta
                            )

                            val fileAction = FileAction(
                                fileId = downloadEntity.id,
                                downloadProgress = progress,
                                action = action,
                                isIndeterminate = progress == -1,
                                fileStatus = RESUME
                            )

                            InternalAppBroadcast.messageFromService(
                                context = appContext,
                                fileAction = fileAction
                            )

                            notificationHelper.showCustomNotification(
                                notificationData,
                                this@FileDownloadService
                            )
                        }
                    }

                    override fun onDownloadComplete(
                        outputFile: DocumentFile,
                        totalBytes: Long,
                        downloadEntity: DownloadEntity
                    ) {
                        coroutineScope.launch {
                            Log.e(
                                "Service",
                                "onDownloadComplete: downloadId => ${downloadEntity.id} | tmpPath=>${outputFile.name}"
                            )
                            mainRepository.updateDownloadedFileUri(
                                downloadEntity.id,
                                downloadEntity.fullFileName,
                                totalBytes,
                                getUriFromSharedPreferences(appContext, FILE_URI)!!.toString(),
                                System.currentTimeMillis()
                            )

                            delay(200)

//                            }

                            InternalAppBroadcast.messageToService(
                                context = appContext,
                                action = DOWNLOADED
                            )

                            val notificationManager =
                                baseContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.cancel(downloadEntity.id)
                        }
                        val fileSize = formatBytes(downloadEntity.fileSize)
                        val notificationData = NotifInfo(
                            id = downloadEntity.id,
                            dStatus = DOWNLOADING,
                            title = downloadEntity.fullFileName,
                            progress = 100,
                            dSpeed = "0 Mbps",
                            action = "Downloaded",
                            eta = "00:00:00"
                        )
                        val fileAction = FileAction(
                            fileId = downloadEntity.id,
                            downloadProgress = 0,
                            action = fileSize,
                            isIndeterminate = downloadEntity.fileDownloadProgress == -1,
                            fileStatus = DOWNLOADED
                        )
                        InternalAppBroadcast.messageFromService(
                            context = appContext,
                            fileAction = fileAction
                        )
                        notificationHelper.showCustomNotification(
                            notificationData,
                            this@FileDownloadService
                        )
                        coroutineScope.launch {
                            delay(200)
                            startAnotherDownload()
                        }
                    }

                    override fun onDownloadFailed(
                        errorMessage: String,
                        downloadEntity: DownloadEntity
                    ) {
                        coroutineScope.launch {

                            InternalAppBroadcast.messageToService(
                                context = appContext,
                                action = START
                            )
                            val fileAction = FileAction(
                                fileId = downloadEntity.id,
                                downloadProgress = 0,
                                action = errorMessage,
                                isIndeterminate = downloadEntity.fileDownloadProgress == -1,
                                fileStatus = FAILED
                            )
                            InternalAppBroadcast.messageFromService(
                                context = appContext,
                                fileAction = fileAction
                            )
                            delay(200)
                            val notificationManager =
                                getSystemService(Context.NOTIFICATION_SERVICE)
                                        as NotificationManager
                            notificationManager.cancel(downloadEntity.id)
                            coroutineScope.launch {
                                delay(200)
                                startAnotherDownload()
                            }
                        }
                    }

                })
        } else {
            Log.e("Service", "tmpFile not opened")
        }
    }
}
