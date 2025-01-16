package com.aliabbas.aliabbasfiledownloader.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliabbas.aliabbasfiledownloader.broadcast.InternalAppBroadcast
import com.aliabbas.aliabbasfiledownloader.db.DownloadEntity
import com.aliabbas.aliabbasfiledownloader.modal.FileDownloadState
import com.aliabbas.aliabbasfiledownloader.processHelper.FileDownloader
import com.aliabbas.aliabbasfiledownloader.repositories.MainRepository
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.CANCEL
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.DOWNLOADED
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.DOWNLOADING
import com.aliabbas.aliabbasfiledownloader.utils.FileUtils.clearDebris
import com.aliabbas.aliabbasfiledownloader.utils.FileUtils.deleteTmpFile
import com.aliabbas.aliabbasfiledownloader.utils.UtilFunctions.convertTimestampToLocalDate
import com.aliabbas.aliabbasfiledownloader.utils.UtilFunctions.convertToViewState
import com.aliabbas.aliabbasfiledownloader.utils.UtilFunctions.getDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val mainRepository: MainRepository,
    val fileDownloader: FileDownloader
) : ViewModel() {

    private val _downloadsLiveData = MutableLiveData<MutableMap<String, MutableList<FileDownloadState>>>()
    val downloadsLiveData: LiveData<MutableMap<String, MutableList<FileDownloadState>>> get() = _downloadsLiveData


    fun getUsersDownloads() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mainRepository.getAllDownload().toSet()
            delay(200)
            Log.e("dList",list.toString())
            val viewStateList = mutableListOf<FileDownloadState>()
            for(item in list) {
                viewStateList.add(convertToViewState(item))
            }
            Log.e("vsList",list.toString())
            val map = viewStateList.groupBy {
                it.lastUpdatedAt
            }.entries.sortedByDescending {
                it.key
            }
            Log.e("mapList",map.toString())
            val dateWiseMap = mutableMapOf<String, MutableList<FileDownloadState>>()
            for (data in map) {
                val timeStamp = data.key
                val listOfItems = data.value

                val day = getDay(convertTimestampToLocalDate(timeStamp))
                if (dateWiseMap.containsKey(day)) {
                    dateWiseMap[day]?.addAll(listOfItems)
                } else {
                    dateWiseMap[day] = listOfItems.toMutableList()
                }
            }
            _downloadsLiveData.postValue(dateWiseMap)
        }
    }

    fun removeDownload(fileItem: FileDownloadState, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            clearDebris(context, fileItem.fullFileName)
            when (fileItem.fileStatus) {
                DOWNLOADED -> {
                    mainRepository.deleteDownload(fileItem.id)
                    delay(100)
                    getUsersDownloads()
                }
                else -> {
                    InternalAppBroadcast.messageToService(context, action = CANCEL, downloadId = fileItem.id)
                    delay(200)
                    getUsersDownloads()
//                    deleteTmpFile(context, fileItem)
//                    downloadsDao.deleteDownload(downloadId)
//                    val appNotificationManager = AppNotificationManager.from(context)
//                    appNotificationManager.cancelNotification(downloadId)
                }
            }
        }
    }

    fun pauseDownload(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            fileDownloader.pauseDownload(id)
        }
    }

    fun putFileToWaiting(fileItem: FileDownloadState) {
        viewModelScope.launch {
            mainRepository.putDownloadToWaiting(fileItem.id, System.currentTimeMillis())
        }
    }

}