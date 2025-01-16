package com.aliabbas.aliabbasfiledownloader.modal

import androidx.annotation.Keep

@Keep
data class LinkInfoData (
    val fileUrl: String,
    var fileName: String,
    val fileExtension: String,
    val fileSize: String,
    val contentLength: Long = 0L,
    val supportRanges: String?,
    var fullFileName: String? = null
    )