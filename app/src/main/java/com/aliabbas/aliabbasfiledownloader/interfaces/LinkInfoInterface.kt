package com.aliabbas.aliabbasfiledownloader.interfaces

import android.os.Message
import com.aliabbas.aliabbasfiledownloader.modal.LinkInfoData

interface LinkInfoInterface {

    fun infoFetchSuccess(linkInfoData: LinkInfoData)
    fun infoFetchFailure(errorMessage: String)
}