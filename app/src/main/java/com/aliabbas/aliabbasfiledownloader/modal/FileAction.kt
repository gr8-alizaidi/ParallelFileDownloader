package com.aliabbas.aliabbasfiledownloader.modal

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.android.parcel.Parcelize

@Keep
@Parcelize
data class FileAction(
    var fileId: Int,
    var downloadProgress: Int = 0,
    var action: String? = null,
    var isIndeterminate: Boolean = false,
    var fileStatus: Int,
    var interruptedBy: Int? = null
) : Parcelable
