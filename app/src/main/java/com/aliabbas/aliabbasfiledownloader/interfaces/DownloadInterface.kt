package com.aliabbas.aliabbasfiledownloader.interfaces

import androidx.documentfile.provider.DocumentFile
import com.aliabbas.aliabbasfiledownloader.db.DownloadEntity
import okhttp3.ResponseBody

interface DownloadInterface {
    fun onDownloadStarted(progress: Int, downloadEntity: DownloadEntity)
    fun onProgressUpdate(progress: Int, eta: String, speed: String, copiedBytes: Long, totalBytes:Long, downloadEntity: DownloadEntity)
    fun onDownloadComplete(filePath: DocumentFile, totalMB: Long, downloadEntity: DownloadEntity)
    fun onDownloadFailed(errorMessage: String, downloadEntity: DownloadEntity)
    fun onDownloadPaused(downloadEntity: DownloadEntity, downloadSpeed: String, downloadProgress: Int, ETA: String, interruptedBy: Int? = null)
    fun onDownloadCancel(downloadEntity: DownloadEntity)
}