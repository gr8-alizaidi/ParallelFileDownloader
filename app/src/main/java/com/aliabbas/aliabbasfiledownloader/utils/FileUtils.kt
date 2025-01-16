package com.aliabbas.aliabbasfiledownloader.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import com.aliabbas.aliabbasfiledownloader.db.DownloadEntity
import com.aliabbas.aliabbasfiledownloader.modal.FileDownloadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


object FileUtils {

    val unreachableDirectory: MutableList<String> = arrayListOf()

    init {
        unreachableDirectory.addAll(
            listOf(
                Environment.DIRECTORY_ALARMS,
                Environment.DIRECTORY_DCIM,
                Environment.DIRECTORY_DOCUMENTS,
                Environment.DIRECTORY_DOWNLOADS,
                Environment.DIRECTORY_MOVIES,
                Environment.DIRECTORY_MUSIC,
                Environment.DIRECTORY_NOTIFICATIONS,
                Environment.DIRECTORY_PICTURES,
                Environment.DIRECTORY_PODCASTS,
                Environment.DIRECTORY_RINGTONES,
                Environment.MEDIA_BAD_REMOVAL,
                Environment.MEDIA_CHECKING,
                Environment.MEDIA_EJECTING,
                Environment.MEDIA_MOUNTED,
                Environment.MEDIA_MOUNTED_READ_ONLY,
                Environment.MEDIA_NOFS,
                Environment.MEDIA_REMOVED,
                Environment.MEDIA_SHARED,
                Environment.MEDIA_UNKNOWN,
                Environment.MEDIA_UNMOUNTABLE,
                Environment.MEDIA_UNMOUNTED
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            unreachableDirectory.addAll(
                listOf(
                    Environment.DIRECTORY_AUDIOBOOKS,
                    Environment.DIRECTORY_SCREENSHOTS,
                )
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            unreachableDirectory.addAll(
                listOf(
                    Environment.DIRECTORY_RECORDINGS
                )
            )
        }
    }

    fun saveUriToSharedPreferences(context: Context, key: String, uri: Uri) {
        val sharedPreferences =
            context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, uri.toString())
        editor.apply()
    }

    fun getUriFromSharedPreferences(context: Context, key: String): Uri? {
        val sharedPreferences =
            context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val uriString = sharedPreferences.getString(key, null)
        return if (uriString != null) Uri.parse(uriString) else null
    }

    fun getMimeType(ext: String): String? {
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getMimeTypeFromExtension(ext)
    }

    fun getFileType(mimeType: String): String {
        return when {
            mimeType.startsWith("audio/") -> "Audio"
            mimeType.startsWith("video/") -> "Video"
            mimeType.startsWith("image/") -> "Image"
            else -> "Doc"
        }
    }


    fun deleteTmpFile(context: Context, downloadEntity: DownloadEntity) {
        try {
            val targetDocumentFile = DocumentFile.fromTreeUri(
                context.applicationContext,
                getUriFromSharedPreferences(context.applicationContext, FILE_URI) ?: return
            ) ?: return

            val dotIndex = downloadEntity.fullFileName.lastIndexOf('.')

            val fileDownloadTitle = downloadEntity.fullFileName.substring(0, dotIndex)
            val tmpFileName = "${downloadEntity.id}_$fileDownloadTitle"

            val tmpFile = targetDocumentFile.findFile(tmpFileName)
            tmpFile?.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openFile(context: Context, fileName: String, mimeType: String) {
        val documentFileUri = getUriFromSharedPreferences(context.applicationContext, FILE_URI)

        if (documentFileUri != null) {
            val targetDocumentFile = DocumentFile.fromTreeUri(
                context.applicationContext,
                documentFileUri
            )
            if (targetDocumentFile == null) {
                Toast.makeText(context, "Unable to locate target file tree", Toast.LENGTH_SHORT)
                    .show()
                return
            }
            val file = targetDocumentFile.findFile(fileName)
            if (file == null) {
                Toast.makeText(context, "File error", Toast.LENGTH_SHORT).show()
                return
            }
            try {

                Log.e(
                    "Open File",
                    "$documentFileUri  --  mime  - $mimeType  --- name -- $fileName -- ${file.type}"
                )
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(file.uri, file.type)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(Intent.createChooser(intent, "Open File Using..."));
            } catch (e: java.lang.Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else Toast.makeText(context, "Invalid file uri", Toast.LENGTH_SHORT).show()
    }

    fun clearDebris(appContext: Context, name: String) {
        val targetUri = getUriFromSharedPreferences(appContext, FILE_URI)!!
        val outputFile = DocumentFile.fromTreeUri(appContext, targetUri) ?: return
        val tmpFile = outputFile.findFile(name)
        tmpFile?.delete()
    }
}