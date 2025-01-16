package com.aliabbas.aliabbasfiledownloader.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aliabbas.aliabbasfiledownloader.R
import com.aliabbas.aliabbasfiledownloader.databinding.ItemDateCardBinding
import com.aliabbas.aliabbasfiledownloader.interfaces.FileItemClickListener
import com.aliabbas.aliabbasfiledownloader.modal.FileAction
import com.aliabbas.aliabbasfiledownloader.modal.FileDownloadState
import com.aliabbas.aliabbasfiledownloader.utils.DownloadInterruption
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.DOWNLOADED
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.FAILED
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.PAUSE
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.RESUME
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.WAITING
import com.aliabbas.aliabbasfiledownloader.utils.FileUtils.getFileType

class DownloadDateCardAdapter(
    val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var innerClickListener: FileItemClickListener? = null

    fun setInnerRecyclerViewClickListener(listener: FileItemClickListener) {
        this.innerClickListener = listener
    }

    private var dateWiseList = mapOf<String, MutableList<FileDownloadState>>()


    fun updateFileDownloadProgress(fileAction: FileAction) {
        val fileItemState = dateWiseList.values.flatten().find { item ->
            item.id == fileAction.fileId
        }
        val fileViewBinding = fileItemState?.itemViewBinding
        if (fileItemState != null || fileViewBinding != null) {

//            fileItemState.fileDownloadProgress = fileAction.downloadProgress
//            fileItemState.viewActionDetail?.text = fileAction.action ?: "--"
//            if (!fileAction.isIndeterminate)
//                fileItemState.viewProgressText?.text = "${fileAction.downloadProgress}%"
//            fileItemState.viewProgressBar?.let { pbar ->
//                if (fileAction.isIndeterminate) {
//                    if (!pbar.isIndeterminate) {
//                        pbar.isIndeterminate = true
//                        fileItemState.viewProgressBar = pbar
//                    }
//                } else {
//                    pbar.setProgressCompat(fileAction.downloadProgress, true)
//                }
//            }

            fileViewBinding!!.actionTitle.text = fileItemState.fullFileName
            if (fileAction.downloadProgress == -1) {
                fileViewBinding.actionProgress.isIndeterminate = true
                fileViewBinding.actionProgressPercent.visibility = GONE
            } else {
                fileViewBinding.actionProgress.isIndeterminate = false
                fileViewBinding.actionProgressPercent.text = "${fileAction.downloadProgress}%"
                fileViewBinding.actionProgress.progress = fileAction.downloadProgress
            }
            fileViewBinding.actionDetail.text = fileAction.action

            when (fileAction.fileStatus) {
                RESUME -> {
                    fileViewBinding.actionIcon.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.ic_pause
                        )
                    )
                    fileViewBinding.actionProgress.setIndicatorColor(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.progress_downloading,
                            null
                        )
                    )
                    fileViewBinding.actionSeparator.visibility = GONE
                    fileViewBinding.actionSource.visibility = GONE
                    fileViewBinding.actionProgressPercent.visibility = VISIBLE
                }
                PAUSE -> {
                    if (fileAction.interruptedBy == DownloadInterruption.WIFI) {
                        fileViewBinding.actionIcon.setImageDrawable(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.ic_pending
                            )
                        )
                        fileViewBinding.actionDetail.text = "Waiting for WiFi"

                        fileViewBinding.actionProgress.setIndicatorColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.progress_waiting,
                                null
                            )
                        )
                    } else {
                        fileViewBinding.actionIcon.setImageDrawable(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.ic_resume
                            )
                        )

                        fileViewBinding.actionProgress.setIndicatorColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.progress_paused,
                                null
                            )
                        )
                    }
                    fileViewBinding.actionSeparator.visibility = GONE
                    fileViewBinding.actionSource.visibility = GONE
                    fileViewBinding.actionProgressPercent.visibility = VISIBLE
                }
                WAITING -> {
                    fileViewBinding!!.actionIcon.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.ic_pending
                        )
                    )
                    fileViewBinding.actionDetail.text = "Waiting in queue"
                    fileViewBinding.actionSeparator.visibility = GONE
                    fileViewBinding.actionSource.visibility = GONE
                    fileViewBinding.actionProgressPercent.visibility = GONE
                }
                FAILED -> {
                    fileViewBinding!!.actionIcon.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.item_failed
                        )
                    )
                    fileViewBinding.actionProgress.setProgress(100, false)
                    fileViewBinding.actionProgress.setIndicatorColor(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.progress_failed,
                            null
                        )
                    )

                    fileViewBinding.actionDetail.text = fileAction.action ?: "Failed to download"
                    fileViewBinding.actionSeparator.visibility = GONE
                    fileViewBinding.actionSource.visibility = GONE
                    fileViewBinding.actionProgressPercent.visibility = GONE
                }
                DOWNLOADED -> {
                    val drawable = when (getFileType(fileItemState.mimeType)) {
                        "Audio" -> R.drawable.item_audio
                        "Video" -> R.drawable.item_video
                        "Image" -> R.drawable.item_image
                        else -> R.drawable.item_doc
                    }

                    fileViewBinding.actionIcon.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            drawable
                        )
                    )


                    fileViewBinding.actionProgress.visibility = GONE
//                    fileViewBinding.actionDetail.text = ""+item.fileSize+" MB"
                    fileViewBinding.actionSeparator.visibility = VISIBLE
                    fileViewBinding.actionSource.visibility = VISIBLE
                    fileViewBinding.actionSource.text = fileItemState.downloadUrl
                    fileViewBinding.actionProgressPercent.visibility = GONE
                }
            }
            fileItemState.fileStatus = fileAction.fileStatus
            fileItemState.fileDownloadProgress = fileAction.downloadProgress
        }
    }

    inner class DownloadDateCardViewHolder(val binding: ItemDateCardBinding) :
        RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding =
            ItemDateCardBinding.inflate(inflater, parent, false)
        return DownloadDateCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {


        val fileActionDay = dateWiseList.keys.toTypedArray()[position]
        Log.e("DDCA", fileActionDay)
        Log.e("DDCA", dateWiseList[fileActionDay].toString())

        (holder as DownloadDateCardViewHolder).binding.txtDate.text = fileActionDay
        if (dateWiseList[fileActionDay] != null && dateWiseList[fileActionDay]!!.size > 0) {
            dateWiseList[fileActionDay]!!.sortBy { it.fileStatus == DOWNLOADED }
            val downloadFileAdapter =
                DownloadFileAdapter(context).also {
                    it.setOnItemClickListener(object : FileItemClickListener {

                        override fun onFileDownloadItemClick(
                            fileItem: FileDownloadState,
                            isCancelled: Boolean
                        ) {
                            innerClickListener?.onFileDownloadItemClick(fileItem, isCancelled)
                        }
                    })
                }
            downloadFileAdapter.differ.submitList(dateWiseList[fileActionDay])
            holder.binding.fileDownloadRv.apply {
                this.layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                this.adapter = downloadFileAdapter
//                this.addItemDecoration(RecyclerViewItemDecoration(context, R.drawable.rv_divider))
            }
        }
    }

    fun updateDownloadList(newListData: MutableMap<String, MutableList<FileDownloadState>>) {
        dateWiseList = newListData
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return dateWiseList.size
    }
}