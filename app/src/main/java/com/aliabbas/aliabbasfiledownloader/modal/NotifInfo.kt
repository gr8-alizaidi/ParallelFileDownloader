package com.aliabbas.aliabbasfiledownloader.modal

import androidx.annotation.Keep

@Keep
data class NotifInfo (
    val id: Int,
    val title: String,
    val eta: String?,
    val action: String,
    val progress: Int,
    val dSpeed: String?,
    val dStatus: Int
        ){
}