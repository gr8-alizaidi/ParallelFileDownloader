package com.aliabbas.aliabbasfiledownloader.modal

import androidx.annotation.Keep

@Keep
data class ErrorData(
    var isError: Boolean,
    var errorMessage: String? = null
)