package com.aliabbas.aliabbasfiledownloader.interfaces

import com.aliabbas.aliabbasfiledownloader.modal.FileDownloadState

interface FileItemClickListener {
    fun onFileDownloadItemClick(fileItem: FileDownloadState, isCancelled: Boolean)
}