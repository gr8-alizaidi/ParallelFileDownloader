package com.aliabbas.aliabbasfiledownloader.modal

import androidx.annotation.Keep
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.aliabbas.aliabbasfiledownloader.databinding.ItemDownloadBinding
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus
import com.google.android.material.progressindicator.LinearProgressIndicator

@Keep
data class FileDownloadState(
    var id: Int = 0,
    var downloadUrl: String,
    var destinationFolderUri: String,
    var fullFileName: String,
    var mimeType: String,
    var fileSize: Long = 0,
    var wifiOnly: Boolean = true,
    var supportRange: Boolean = false,
    var fileStatus: Int = FileStatus.WAITING,
    var fileDownloadProgress: Int = 0,
    var interruptedBy: Int? = null,//paused by user or waiting for wifi
    var lastUpdatedAt: Long = -1, //last updated timestamp

    var itemViewBinding: ItemDownloadBinding? = null
) {
    override fun toString(): String = "id=$id filename=$fullFileName filesize=$fileSize fileStatus=$fileStatus downloadProgress=$fileDownloadProgress  wifiOnly=$wifiOnly supportRange=$supportRange interrupted=$interruptedBy destinationFolderUri=$destinationFolderUri"
}