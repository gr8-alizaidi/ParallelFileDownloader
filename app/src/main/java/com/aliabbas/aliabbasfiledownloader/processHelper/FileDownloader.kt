package com.aliabbas.aliabbasfiledownloader.processHelper

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.aliabbas.aliabbasfiledownloader.db.DownloadEntity
import com.aliabbas.aliabbasfiledownloader.interfaces.DownloadInterface
import com.aliabbas.aliabbasfiledownloader.repositories.MainRepository
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.CANCEL
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.DOWNLOADED
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.FAILED
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.PAUSE
import com.aliabbas.aliabbasfiledownloader.utils.DownloadInterruption.CELLULAR
import com.aliabbas.aliabbasfiledownloader.utils.DownloadInterruption.USER
import com.aliabbas.aliabbasfiledownloader.utils.DownloadInterruption.WIFI
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.DOWNLOADING
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.WAITING
import com.aliabbas.aliabbasfiledownloader.utils.FileUtils.clearDebris
import com.aliabbas.aliabbasfiledownloader.utils.FileUtils.deleteTmpFile
import com.aliabbas.aliabbasfiledownloader.utils.UtilFunctions.formatTime
import com.aliabbas.aliabbasfiledownloader.utils.UtilFunctions.isInternetConnected
import com.aliabbas.aliabbasfiledownloader.utils.UtilFunctions.isWifiConnected
import kotlinx.coroutines.*
import okhttp3.*
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject


class FileDownloader @Inject constructor(private val mainRepository: MainRepository) {

    private val fileDownloadMap = mutableMapOf<Int, Job>()
    private val jobCallbackMap = mutableMapOf<Int, DownloadInterface>()
    private val pausedJobMap = mutableMapOf<Int, AtomicBoolean>()
    private val pauseAll = AtomicBoolean(false)
    private val cancellationJobMap = mutableMapOf<Int, AtomicBoolean>()
    private val jobIdMap = mutableMapOf<Int, DownloadEntity>()
    private val jobCallMap = mutableMapOf<Int, Call>()
    private val jobCoroutineMap = mutableMapOf<Int, CoroutineScope>()


    fun downloadFile(
        context: Context,
        downloadEntity: DownloadEntity,
        outputFile: DocumentFile,
        downloadInterface: DownloadInterface,
    ) {
        if (!fileDownloadMap.containsKey(downloadEntity.id)) {
            val coroutineScope = CoroutineScope(Dispatchers.IO)
            jobCoroutineMap[downloadEntity.id] = coroutineScope

            jobIdMap[downloadEntity.id] = downloadEntity
            jobCallbackMap[downloadEntity.id] = downloadInterface
            pausedJobMap[downloadEntity.id] = AtomicBoolean(false)
            pauseAll.set(false)
            cancellationJobMap[downloadEntity.id] = AtomicBoolean(false)

            fileDownloadMap[downloadEntity.id] = CoroutineScope(Dispatchers.IO).launch {
                val outputFileSize = outputFile.length()
                val requestBuilder = Request.Builder().url(downloadEntity.downloadUrl)

                if (outputFileSize > 0) {
                    requestBuilder.addHeader("Range", "bytes=$outputFileSize-")
                }
                var initialProgress = 0
                if (outputFileSize > 0 && downloadEntity.fileSize > 0) {
                    initialProgress = ((outputFileSize * 100 / downloadEntity.fileSize).toInt())
                }
                markDownloadStarted(downloadEntity, initialProgress)
                downloadInterface.onDownloadStarted(initialProgress, downloadEntity)

                val client = OkHttpClient.Builder().connectTimeout(1, TimeUnit.MINUTES)
                    .readTimeout(2, TimeUnit.MINUTES).writeTimeout(3, TimeUnit.MINUTES)
                val request = requestBuilder.build()
                val call = client.build().newCall(request)
                jobCallMap[downloadEntity.id] = call
                try {
                    if (downloadEntity.wifiOnly && !isWifiConnected(context)) {
                        throw Exception("No wifi Connectivity found")
                    }
                    val response = call.execute()
                    if (response.isSuccessful) {
                        if (response.body != null) {
                            val outputStream = outputFile.uri.let {
                                context.contentResolver.openOutputStream(
                                    it,
                                    "wa"
                                )
                            }!!
                            fetchData(
                                outputFile,
                                response.body!!,
                                downloadEntity,
                                downloadInterface,
                                context,
                                outputStream
                            )
                        } else {
                            removeDownloadJob(downloadEntity)
                            val errorMessage = "Response body is null"
                            markDownloadFailed(downloadEntity, errorMessage)
                            downloadInterface.onDownloadFailed(
                                errorMessage,
                                downloadEntity
                            )
                        }
                    } else {
                        removeDownloadJob(downloadEntity)
                        val errorMessage = "Error : code ${response.code}"
                        markDownloadFailed(downloadEntity, errorMessage)
                        downloadInterface.onDownloadFailed(
                            errorMessage,
                            downloadEntity
                        )
                    }
                } catch (e: Exception) {
                    val isInternetActive = isInternetConnected(context)
                    if (isInternetActive) {
                        val interruptedBy = if (downloadEntity.wifiOnly) WIFI else CELLULAR
                        downloadEntity.fileStatus = PAUSE
                        markDownloadPaused(downloadEntity, 0, interruptedBy, 0)
                        downloadInterface.onDownloadPaused(
                            downloadEntity,
                            "0 Mbps",
                            downloadEntity.fileDownloadProgress,
                            "00:00",
                            WIFI
                        )
                        removeDownloadJob(downloadEntity)
                    } else {
                        val errorMessage = "Failed to download: ${e.message}"
                        markDownloadFailed(downloadEntity, errorMessage)
                        downloadInterface.onDownloadFailed(errorMessage, downloadEntity)
                        downloadEntity.fileStatus = CANCEL
                        removeDownloadJob(downloadEntity)
                    }
                }
            }
        }
    }


    private suspend fun markDownloadStarted(downloadEntity: DownloadEntity, startProgress: Int) {
        mainRepository.updateFileDownloadProgress(
            downloadEntity.id,
            DOWNLOADING,
            null,
            System.currentTimeMillis(),
            startProgress,
            0
        )
        delay(200)
    }


    private fun markDownloadCompleted(downloadEntity: DownloadEntity) {
        jobCoroutineMap[downloadEntity.id]?.launch {
            mainRepository.updateFileDownloadProgress(
                downloadEntity.id,
                DOWNLOADED,
                null,
                System.currentTimeMillis(),
                100,
                downloadEntity.fileSize
            )
            delay(100)
        }
    }


    private fun markDownloadCancel(downloadEntity: DownloadEntity) {
        if (jobCallMap.containsKey(downloadEntity.id))
            jobCallMap.remove(downloadEntity.id)
        CoroutineScope(Dispatchers.IO).launch {
            Log.e("deletingFromDB", "true")
            mainRepository.deleteDownload(downloadEntity.id)
            delay(200)
        }
    }

    private fun markDownloadPaused(
        downloadEntity: DownloadEntity,
        progress: Int,
        interruptedBy: Int,
        copiedByte: Long
    ) {
        jobCoroutineMap[downloadEntity.id]?.launch {
            mainRepository.updateFileDownloadProgress(
                downloadEntity.id,
                PAUSE,
                interruptedBy,
                System.currentTimeMillis(),
                progress,
                copiedByte
            )
        }
    }

    private fun markDownloadFailed(
        downloadEntity: DownloadEntity,
        errorMessage: String = "Failed to download"
    ) {
        jobCoroutineMap[downloadEntity.id]?.launch {
            mainRepository.updateDownloadEnd(
                downloadEntity.id,
                FAILED,
                100,
                null,
                System.currentTimeMillis(),
                errorMessage
            )
            delay(200)
        }
    }

    private fun removeDownloadJob(downloadEntity: DownloadEntity) {
        if (downloadEntity.fileStatus == CANCEL)
            jobCallbackMap.remove(downloadEntity.id)


        pausedJobMap.remove(downloadEntity.id)
        jobCallbackMap.remove(downloadEntity.id)
        fileDownloadMap.remove(downloadEntity.id)
        jobCoroutineMap.remove(downloadEntity.id)
    }

    private fun fetchData(
        outputFile: DocumentFile,
        responseBody: ResponseBody,
        downloadEntity: DownloadEntity,
        downloadInterface: DownloadInterface,
        context: Context,
        outputStream: OutputStream
    ) {
        var lastProgress = 0
        var lastTime = System.currentTimeMillis()


        val inputStream = responseBody.byteStream()

        try {
            val fileSeek = outputFile.length()
            val buffer = ByteArray(4096)
            var bytesRead: Int
            var totalBytesRead: Long = fileSeek
            val totalBytes: Long = downloadEntity.fileSize
            var lastBytesRead = totalBytesRead
            var formattedSpeed = ""
            var formattedTimeLeft = ""


            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                downloadEntity.copiedSize = totalBytesRead

                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - lastTime
                if (elapsedTime >= 1500) { // Calculate speed and estimated time every second
                    val bytesDownloaded = totalBytesRead - fileSeek
                    val speed =
                        bytesDownloaded.toDouble() / elapsedTime.toDouble() // Speed in bytes per millisecond
                    val speedInMB = speed * 1000 / (1024 * 1024) // Speed in MB/s
                    val remainingBytes = totalBytes - totalBytesRead
                    val estimatedTimeLeft =
                        remainingBytes / speed // Estimated time left in milliseconds

                    lastTime = currentTime

                    formattedSpeed = "%.2f Mbps".format(speedInMB)
                    formattedTimeLeft = formatTime(estimatedTimeLeft)
                    val currentProgress =
                        if (totalBytes > 0) (totalBytesRead * 100 / totalBytes).toInt() else -1
                    if (totalBytesRead > lastBytesRead) {
                        downloadInterface.onProgressUpdate(
                            currentProgress,
                            formattedTimeLeft,
                            formattedSpeed,
                            totalBytesRead,
                            totalBytes,
                            downloadEntity
                        )

                        lastProgress = currentProgress
                        lastBytesRead = totalBytesRead
                    }
                }
                if (pauseAll.get()) {
                    markDownloadPaused(downloadEntity, lastProgress, USER, totalBytesRead)
                    downloadEntity.fileDownloadProgress = lastProgress
                    downloadInterface.onDownloadPaused(
                        downloadEntity,
                        formattedSpeed,
                        lastProgress,
                        formattedTimeLeft
                    )
                    cancelDownloadingJob(downloadEntity.id)
                    removeDownloadJob(downloadEntity)
                    return
                }

                if (pausedJobMap.containsKey(downloadEntity.id) && pausedJobMap[downloadEntity.id]?.get() == true) {
                    markDownloadPaused(downloadEntity, lastProgress, USER, totalBytesRead)
                    downloadEntity.fileDownloadProgress = lastProgress
                    downloadInterface.onDownloadPaused(
                        downloadEntity,
                        formattedSpeed,
                        lastProgress,
                        formattedTimeLeft
                    )
                    cancelDownloadingJob(downloadEntity.id)
                    removeDownloadJob(downloadEntity)
                    return
                }

                if (cancellationJobMap.containsKey(downloadEntity.id) && cancellationJobMap[downloadEntity.id]?.get() == true) {
                    markDownloadCancel(downloadEntity)
                    cancelDownloadingJob(downloadEntity.id)
                    removeDownloadJob(downloadEntity)
//                    deleteTmpFile(context, downloadEntity)
                    downloadInterface.onDownloadCancel(downloadEntity)
                    clearDebris(context, downloadEntity.fullFileName)
                    return
                }
            }
            removeDownloadJob(downloadEntity)
            if (downloadEntity.fileSize == 0L)
                downloadEntity.fileSize = totalBytesRead

            markDownloadCompleted(downloadEntity)
            downloadInterface.onDownloadComplete(
                outputFile,
                totalBytesRead,
                downloadEntity
            )
        } catch (e: IOException) {
            val isInternetActive = isInternetConnected(context)
            if (isInternetActive) {
                val interruptedBy = if (downloadEntity.wifiOnly) WIFI else CELLULAR
                markDownloadPaused(
                    downloadEntity,
                    lastProgress,
                    interruptedBy,
                    downloadEntity.copiedSize
                )
                downloadEntity.fileDownloadProgress = lastProgress
                downloadInterface.onDownloadPaused(
                    downloadEntity,
                    "0 Mbps",
                    lastProgress,
                    "00:00",
                    WIFI
                )
                removeDownloadJob(downloadEntity)
            } else {
                val errorMessage =
                    "Failed to download: ${e.message}"
                markDownloadFailed(downloadEntity)
//                deleteTmpFile(context, downloadEntity)
                clearDebris(context, downloadEntity.fullFileName)
                downloadInterface.onDownloadFailed(
                    errorMessage,
                    downloadEntity
                )
                removeDownloadJob(downloadEntity)
            }
        } finally {
            try {
                outputStream.close()
                inputStream.close()
                responseBody.close()
            } catch (e: Exception) {
            }
        }
    }


    private fun cancelDownloadingJob(id: Int) {
        val job = fileDownloadMap[id]
        val call = jobCallMap[id]
        val coroutineScope = jobCoroutineMap[id]
        job?.cancel()
        call?.cancel()
        coroutineScope?.cancel()
    }

    fun pauseDownload(downloadId: Int) {
        val pauseJob = pausedJobMap[downloadId]
        pauseJob?.set(true)
        Log.e("isPauseJobPresent", "$pauseJob")
    }

    fun pauseAllDownload() {
        pauseAll.set(true)
    }

    fun cancelDownload(context: Context, downloadId: Int) {
        Log.e("IsJobID", "${jobIdMap[downloadId]}")
        if (jobIdMap[downloadId] == null)
            return
        val pauseFlags = pausedJobMap[downloadId]
        Log.e("IsPauseFlag", "${pausedJobMap[downloadId]}")
        if (pauseFlags == null) {
            Log.e("coroutineMap", "${jobCoroutineMap[downloadId]}")
            val jobScope = jobCoroutineMap[downloadId]
            if (jobScope == null) {
                CoroutineScope(Dispatchers.IO).launch {
                    mainRepository.deleteDownload(downloadId)
                    delay(200)
                    jobCallbackMap[downloadId]?.onDownloadCancel(jobIdMap[downloadId]!!)
                }
            } else {
                jobScope!!.launch {
                    clearDebris(context, jobIdMap[downloadId]!!.fullFileName)
                    markDownloadCancel(jobIdMap[downloadId]!!)
                    Log.e("callbackMap", "${jobCallbackMap[downloadId]}")
                    jobCallbackMap[downloadId]?.onDownloadCancel(jobIdMap[downloadId]!!)
                    removeDownloadJob(jobIdMap[downloadId]!!)
                }
            }
        } else {
            val cancellationFlag = cancellationJobMap[downloadId]
            cancellationFlag?.set(true)
        }
    }

    fun getFileEntity(downloadId: Int): DownloadEntity? {
        return if (jobIdMap[downloadId] != null) {
            jobIdMap[downloadId]
        } else null
    }

    fun resumeAllFiles() {
        CoroutineScope(Dispatchers.IO).launch {
            mainRepository.clearFilesInterruption()
        }
    }
}