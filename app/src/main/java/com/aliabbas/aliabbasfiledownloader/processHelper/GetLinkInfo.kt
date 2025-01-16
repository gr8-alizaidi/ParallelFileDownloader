package com.aliabbas.aliabbasfiledownloader.processHelper

import android.util.Log
import com.aliabbas.aliabbasfiledownloader.interfaces.LinkInfoInterface
import com.aliabbas.aliabbasfiledownloader.modal.LinkInfoData
import com.aliabbas.aliabbasfiledownloader.utils.FileUtils.getMimeType
import okhttp3.*
import okio.IOException
import java.net.URL

object GetLinkInfo {

    fun getLinkInfo(url: String, linkInfoInterface: LinkInfoInterface) {

        try {
            val request = Request.Builder()
                .url(url)
                .build()
            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    linkInfoInterface.infoFetchFailure(e.message.toString())
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val contentLength = response.header("Content-Length")
                        val contentType = response.header("Content-Type")
                        val supportRanges = response.header("Accept-Ranges")
                        val responseBody = response.body
                        if (responseBody == null) {
                            linkInfoInterface.infoFetchFailure("No response")
                            return
                        } else if (contentType == null) {
                            linkInfoInterface.infoFetchFailure("Unable to get extension of the file")
                            return
                        }

                        val urlPath = URL(url).path.substringAfterLast('/')
                        val dotIndex = urlPath.lastIndexOf(".")
                        val fileName = if (dotIndex != -1) urlPath.substring(0, dotIndex) else null
                        val fileExtension = if (dotIndex != -1) urlPath.substring(dotIndex + 1) else null

//                        if(fileExtension?.length > 3) {
//                            fileExtension = fileExtension.substring(0,5)+".."
//                        }
                        var fileSize = "Unknown size"
                        if (contentLength != null) {
                            fileSize = "${((1.0) * (contentLength.toDouble() / (1024 * 1024)))} MB"
                        }
                        Log.e("content_type",contentType.toString())
                        Log.e("res",response.toString())
                        println("File size: $fileSize MB")
                        println("File extension: $fileExtension")
                        println("File name: $fileName")
                        println("Response body: $responseBody")
                        val linkInfo = LinkInfoData(url, fileName ?: "Invalid Name", fileExtension?: "", fileSize, contentLength?.toLong() ?: 0L, supportRanges)
                        linkInfoInterface.infoFetchSuccess(linkInfo)

                        response.body?.close()
                    } else {
                        // Handle unsuccessful response
                        linkInfoInterface.infoFetchFailure("Request failed with code: ${response.code}")
                    }
                }
            })
        } catch (e: Exception) {
            linkInfoInterface.infoFetchFailure(e.message.toString())
        }
    }
}