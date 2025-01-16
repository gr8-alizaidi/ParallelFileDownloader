package com.aliabbas.aliabbasfiledownloader.utils

const val GET_LINK_LAYOUT = 101
const val GET_GRABBING_INFO_LAYOUT = 102
const val GET_FILE_SAVE_DETAILS_LAYOUT = 103
const val GET_SUCCESS_LAYOUT = 104
const val GET_FAILURE_LAYOUT = 105
const val FILE_URI = "file_uri_sp"
const val NOTIFICATION_CHANNEL_ID = "ali_abbas_file_downloader_app_channel"
const val NOTIFICATION_CHANNEL_NAME = "ALI ABBAS FILE DOWNLOADER"
const val FILE_ACTION_DATA = "fileActionData"
const val NOTIFICATION_ID = 106
const val UPDATE_ALL = 107
const val REQUEST_CODE_DIRECTORY_PICKUP = 123

object FileStatus {
    const val WAITING = 0
    const val PAUSE = 1
    const val START = 2
    const val RESUME = 3
    const val RESUME_ALL = 4
    const val PAUSE_ALL = 5
    const val CANCEL = 6
    const val FAILED = 7
    const val DOWNLOADED = 8
    const val DOWNLOADING = 9
    const val NOTIF_REMOVE = 10
}

object DownloadInterruption {
    const val WIFI = 50
    const val CELLULAR = 51
    const val USER = 52
}