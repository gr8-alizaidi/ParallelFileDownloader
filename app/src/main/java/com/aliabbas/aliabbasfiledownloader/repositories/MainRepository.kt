package com.aliabbas.aliabbasfiledownloader.repositories

import com.aliabbas.aliabbasfiledownloader.db.DownloadEntity
import com.aliabbas.aliabbasfiledownloader.db.DownloadEntityDAO
import javax.inject.Inject


class MainRepository @Inject constructor(
     val downloadEntityDAO: DownloadEntityDAO
) {
    suspend fun getAllDownload() = downloadEntityDAO.getAllDownloads()

    suspend fun setFileName(id: Int, title: String) = downloadEntityDAO.setFileName(id, title)

    suspend fun getDestinationFolderUri(id: Int) = downloadEntityDAO.getDestinationFolderUri(id)

    suspend fun getCurrentlyDownloadingFile() = downloadEntityDAO.getCurrentlyDownloadingFile()

    suspend fun addFileToDownload(downloadEntity: DownloadEntity): Long {

        return downloadEntityDAO.addFileToDownload(downloadEntity)
    }

    suspend fun getRemainingFiles() = downloadEntityDAO.getRemainingFiles()

    suspend fun getFilesInQueue() = downloadEntityDAO.getFilesInQueue()

    suspend fun updateFileDownloadProgress(id: Int, fileStatus: Int, interruptedBy: Int?, timestamp: Long, progress: Int, copiedByte: Long)
    = downloadEntityDAO.updateFileDownloadProgress(id, fileStatus, interruptedBy, timestamp, progress, copiedByte)

    suspend fun updateDownloadWaiting(id: Int, downloadStatus: Int, timestamp: Long)
    = downloadEntityDAO.updateDownloadWaiting(id, downloadStatus, timestamp)

    suspend fun updateDownloadEnd(id: Int, downloadStatus: Int, lastProgress: Int, interruptedBy: Int?, timestamp: Long, action: String) = downloadEntityDAO.updateDownloadEnd(id, downloadStatus, lastProgress, interruptedBy, timestamp, action)

    suspend fun deleteDownload(id: Int) = downloadEntityDAO.deleteDownload(id)

    suspend fun updateDownloadedFileUri(id: Int, title: String, size: Long, downloadedUri: String, timestamp: Long) = downloadEntityDAO.updateDownloadedFileUri(id, title, size, downloadedUri, timestamp)

    suspend fun putDownloadToWaiting(id: Int, timestamp: Long) = downloadEntityDAO.putFileToWaiting(id, timestamp)

    suspend fun clearFilesInterruption() = downloadEntityDAO.clearAllFilesInterruption()

    suspend fun putInterruptediFilesToWaiting(timestamp: Long) = downloadEntityDAO.putInterruptediFileToWaiting(timestamp)

    suspend fun putCellInterruptedFilesToWaiting(timestamp: Long) = downloadEntityDAO.putCellInterruptedFileToWaiting(timestamp)

}