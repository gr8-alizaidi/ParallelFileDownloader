package com.aliabbas.aliabbasfiledownloader.db

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus

@Keep
@Entity(tableName = "download_entity")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var downloadUrl: String,
    var tmpFolderUri: String,
    var downloadFolderUri: String?,
    var fullFileName: String,
    var mimeType: String,
    var copiedSize: Long = 0,
    var fileSize: Long = 0,
    var wifiOnly: Boolean = true,
    var supportRange: Boolean = false,
    var fileStatus: Int = FileStatus.WAITING,
    var fileDownloadProgress: Int = 0,
    var interruptedBy: Int? = null,//paused by user or waiting for wifi
    var lastUpdatedAt: Long = -1, //last updated timestamp
    var action: String? = null
) {
    override fun toString(): String =
        "id=$id filename=$fullFileName filesize=$fileSize fileStatus=$fileStatus downloadFolderUri=$downloadFolderUri downloadProgress=$fileDownloadProgress  wifiOnly=$wifiOnly supportRange=$supportRange interrupted=$interruptedBy tmpFolderUri=$tmpFolderUri action=$action"
}