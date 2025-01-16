package com.aliabbas.aliabbasfiledownloader.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aliabbas.aliabbasfiledownloader.modal.FileAction
import com.aliabbas.aliabbasfiledownloader.utils.DownloadInterruption.CELLULAR
import com.aliabbas.aliabbasfiledownloader.utils.DownloadInterruption.USER
import com.aliabbas.aliabbasfiledownloader.utils.DownloadInterruption.WIFI
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.DOWNLOADED
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.DOWNLOADING
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.FAILED
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.PAUSE
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.RESUME
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.START
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.WAITING
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadEntityDAO {

    @Query("SELECT * FROM download_entity")
    suspend fun getAllDownloads(): List<DownloadEntity>

    @Query("UPDATE download_entity SET fullFileName=:title WHERE id=:id")
    suspend fun setFileName(id: Int, title:String)

    @Query("SELECT * FROM download_entity where fileStatus = '${DOWNLOADING}' ORDER BY lastUpdatedAt DESC")
    suspend fun getCurrentlyDownloadingFile(): List<DownloadEntity>

    @Query("SELECT * FROM download_entity WHERE fileStatus = '${WAITING}' OR (fileStatus = '${PAUSE}' AND interruptedBy <> '${USER}') ORDER BY lastUpdatedAt DESC")
    suspend fun getFilesInQueue(): List<DownloadEntity>

    @Query("SELECT tmpFolderUri FROM download_entity WHERE id=:id")
    suspend fun getDestinationFolderUri(id: Int): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFileToDownload(item: DownloadEntity): Long

    @Query("SELECT * FROM download_entity where fileStatus = '${WAITING}' OR fileStatus = '${PAUSE}' ORDER BY lastUpdatedAt DESC")
    suspend fun getRemainingFiles(): List<DownloadEntity>

    @Query("UPDATE download_entity SET fileStatus=:fileStatus, interruptedBy=:interruptedBy, lastUpdatedAt=:timestamp, fileDownloadProgress=:progress, copiedSize=:copiedByte WHERE id=:id")
    suspend fun updateFileDownloadProgress(id: Int, fileStatus: Int, interruptedBy: Int?, timestamp: Long, progress: Int, copiedByte: Long)

    @Query("UPDATE download_entity SET fileStatus=:downloadStatus, lastUpdatedAt=:timestamp WHERE id=:id")
    suspend fun updateDownloadWaiting(id: Int, downloadStatus: Int, timestamp: Long)

    @Query("UPDATE download_entity SET fileStatus=:downloadStatus, fileDownloadProgress=:lastProgress, interruptedBy=:interruptedBy, lastUpdatedAt=:timestamp, action=:action WHERE id=:id")
    suspend fun updateDownloadEnd(id: Int, downloadStatus: Int, lastProgress: Int, interruptedBy: Int?, timestamp: Long, action: String?)

    @Query("UPDATE download_entity SET fileSize=:size, fullFileName=:title, fileStatus=${DOWNLOADED}, downloadFolderUri=:downloadedUri, lastUpdatedAt=:timestamp WHERE id=:id")
    suspend fun updateDownloadedFileUri(id: Int, title: String, size: Long, downloadedUri: String, timestamp: Long)

    @Query("DELETE FROM download_entity WHERE id=:id")
    suspend fun deleteDownload(id: Int)

    @Query("UPDATE download_entity SET fileStatus=${WAITING}, lastUpdatedAt=:timestamp WHERE id=:id")
    suspend fun putFileToWaiting(id: Int, timestamp: Long)

    @Query("UPDATE download_entity SET fileStatus = '${WAITING}' AND interruptedBy = null WHERE fileStatus NOT IN (${DOWNLOADING}, ${DOWNLOADED})")
    suspend fun clearAllFilesInterruption()

    @Query("UPDATE download_entity SET fileStatus=${WAITING}, lastUpdatedAt=:timestamp WHERE (interruptedBy='$WIFI' or interruptedBy='$CELLULAR')")
    suspend fun putInterruptediFileToWaiting(timestamp: Long):Int

    @Query("UPDATE download_entity SET fileStatus=${WAITING}, lastUpdatedAt=:timestamp WHERE interruptedBy='$CELLULAR'")
    suspend fun putCellInterruptedFileToWaiting(timestamp: Long):Int

}