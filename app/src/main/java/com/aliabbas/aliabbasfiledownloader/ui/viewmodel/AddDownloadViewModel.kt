package com.aliabbas.aliabbasfiledownloader.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliabbas.aliabbasfiledownloader.processHelper.GetLinkInfo
import com.aliabbas.aliabbasfiledownloader.db.DownloadEntity
import com.aliabbas.aliabbasfiledownloader.interfaces.LinkInfoInterface
import com.aliabbas.aliabbasfiledownloader.modal.ErrorData
import com.aliabbas.aliabbasfiledownloader.modal.LinkInfoData
import com.aliabbas.aliabbasfiledownloader.repositories.MainRepository
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.START
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.WAITING
import com.aliabbas.aliabbasfiledownloader.utils.FileUtils.getMimeType
import com.aliabbas.aliabbasfiledownloader.utils.FileUtils.getUriFromSharedPreferences
import com.aliabbas.aliabbasfiledownloader.utils.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddDownloadViewModel @Inject constructor(
    val mainRepository: MainRepository
) : ViewModel() {


    private val _errorObserver: SingleLiveEvent<ErrorData> = SingleLiveEvent()
    val errorObserver: SingleLiveEvent<ErrorData> get() = _errorObserver


    private val _documentPathSelect = SingleLiveEvent<DocumentFile?>()
    val documentPathSelect: SingleLiveEvent<DocumentFile?> get() = _documentPathSelect

    private val _linkInfoObserver: SingleLiveEvent<LinkInfoData> = SingleLiveEvent()
    val linkInfoObserver: SingleLiveEvent<LinkInfoData> get() = _linkInfoObserver


    private val _addDownloadEvent: SingleLiveEvent<Int> = SingleLiveEvent()
    val addDownloadEvent: SingleLiveEvent<Int> get() = _addDownloadEvent


    val scope = CoroutineScope(Dispatchers.IO)

    fun fetchLinkInfo(url: String) {
        GetLinkInfo.getLinkInfo(url, object : LinkInfoInterface {
            override fun infoFetchSuccess(linkInfoData: LinkInfoData) {
                Log.e("here", "ifs")
                _linkInfoObserver.postValue(linkInfoData)
            }

            override fun infoFetchFailure(errorMessage: String) {
                _errorObserver.postValue(ErrorData(true, errorMessage))
            }
        })
    }

    fun setUserPath(documentFile: DocumentFile?) {
        _documentPathSelect.value = documentFile
    }

    fun addDownload(linkInfoData: LinkInfoData, wifiOnly: Boolean, destinationUri: Uri) {
        scope.launch {
            try {
                delay(100)
                val mimeType = getMimeType(linkInfoData.fileExtension)
                if (mimeType.isNullOrBlank()) {
                    _errorObserver.postValue(ErrorData(true, "Unable to fetch MIME type of file"))
                }
                val downloadEntity = DownloadEntity(
                    downloadUrl = linkInfoData.fileUrl,
                    tmpFolderUri = "",
                    wifiOnly = wifiOnly,
                    fullFileName = "${linkInfoData.fullFileName}",
                    mimeType = mimeType ?: "",
                    fileSize = linkInfoData.contentLength,
                    supportRange = !linkInfoData.supportRanges.isNullOrBlank(),
                    fileStatus = WAITING,
                    lastUpdatedAt = System.currentTimeMillis(),
                    downloadFolderUri = destinationUri.toString()
                )

                val id = mainRepository.addFileToDownload(downloadEntity)
                delay(500)
                if (id <= 0) {
                    _errorObserver.postValue(
                        ErrorData(
                            true,
                            "Failed to add file to downloading queue!"
                        )
                    )
                } else
                    _addDownloadEvent.postValue(1)
            } catch (e: Exception) {
                e.printStackTrace()
                _errorObserver.postValue(ErrorData(true, "Error : ${e.message}"))
            }
        }
    }

}