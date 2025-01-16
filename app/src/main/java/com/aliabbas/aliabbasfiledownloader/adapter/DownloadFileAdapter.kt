package com.aliabbas.aliabbasfiledownloader.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aliabbas.aliabbasfiledownloader.R
import com.aliabbas.aliabbasfiledownloader.databinding.ItemDownloadBinding
import com.aliabbas.aliabbasfiledownloader.db.DownloadEntity
import com.aliabbas.aliabbasfiledownloader.interfaces.FileItemClickListener
import com.aliabbas.aliabbasfiledownloader.modal.FileDownloadState
import com.aliabbas.aliabbasfiledownloader.utils.DownloadInterruption.WIFI
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.CANCEL
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.DOWNLOADED
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.FAILED
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.PAUSE
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.RESUME
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.WAITING
import com.aliabbas.aliabbasfiledownloader.utils.FileUtils
import com.aliabbas.aliabbasfiledownloader.utils.UtilFunctions.formatBytes


class DownloadFileAdapter(
    private val context: Context
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var itemClickListener: FileItemClickListener? = null

    fun setOnItemClickListener(listener: FileItemClickListener?) {
        itemClickListener = listener
    }

    private val differCallback = object : DiffUtil.ItemCallback<FileDownloadState>() {
        override fun areItemsTheSame(
            oldItem: FileDownloadState,
            newItem: FileDownloadState
        ): Boolean {
            return oldItem.toString() == newItem.toString()
        }

        override fun areContentsTheSame(
            oldItem: FileDownloadState,
            newItem: FileDownloadState
        ): Boolean {
            return oldItem == newItem
        }
    }
    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding =
            ItemDownloadBinding.inflate(inflater, parent, false)
        return DownloadFileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val fileViewBinding = (holder as DownloadFileViewHolder).binding
        val item = differ.currentList[position]
        Log.e("DFAA", item.toString())

        item.itemViewBinding = fileViewBinding
        if (position == differ.currentList.size - 1) {
            fileViewBinding.itemSeperatorView.visibility = GONE
        }
        when (item.fileStatus) {
            RESUME -> {
                fileViewBinding.actionIcon.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_pause
                    )
                )

                fileViewBinding.actionTitle.text = item.fullFileName
                fileViewBinding.actionProgress.setProgress(item.fileDownloadProgress, true)
                fileViewBinding.actionProgress.setIndicatorColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.progress_downloading,
                        null
                    )
                )
                val totalMB = formatBytes(item.fileSize)
                val downloadBytes = (item.fileSize.toDouble()) * item.fileDownloadProgress / 100;
                val downloadedMB = formatBytes(downloadBytes.toLong())

                fileViewBinding.actionDetail.text = "$downloadedMB / $totalMB"
                fileViewBinding.actionSeparator.visibility = GONE
                fileViewBinding.actionSource.visibility = GONE
                fileViewBinding.actionProgressPercent.visibility = VISIBLE
                fileViewBinding.actionProgressPercent.text = "${item.fileDownloadProgress}%"
            }
            PAUSE -> {
                if (item.interruptedBy == WIFI) {
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

                    val totalMB = formatBytes(item.fileSize)
                    val downloadBytes =
                        (item.fileSize.toDouble()) * item.fileDownloadProgress / 100;
                    val downloadedMB = formatBytes(downloadBytes.toLong())
                    fileViewBinding.actionDetail.text = "$downloadedMB / $totalMB"
                    fileViewBinding.actionProgress.setIndicatorColor(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.progress_paused,
                            null
                        )
                    )

                }

                fileViewBinding.actionTitle.text = item.fullFileName
                fileViewBinding.actionProgress.setProgress(item.fileDownloadProgress, true)

                fileViewBinding.actionSeparator.visibility = GONE
                fileViewBinding.actionSource.visibility = GONE
                fileViewBinding.actionProgressPercent.visibility = VISIBLE
                fileViewBinding.actionProgressPercent.text = "${item.fileDownloadProgress}%"
            }
            WAITING -> {
                fileViewBinding.actionIcon.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_pending
                    )
                )

                fileViewBinding.actionTitle.text = item.fullFileName
                fileViewBinding.actionProgress.setProgress(0, false)


                fileViewBinding.actionDetail.text = "Waiting in queue"
                fileViewBinding.actionSeparator.visibility = GONE
                fileViewBinding.actionSource.visibility = GONE
                fileViewBinding.actionProgressPercent.visibility = GONE
            }
            FAILED -> {
                fileViewBinding.actionIcon.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.item_failed
                    )
                )

                fileViewBinding.actionTitle.text = item.fullFileName
                fileViewBinding.actionProgress.setProgress(100, false)
                fileViewBinding.actionProgress.setIndicatorColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.progress_failed,
                        null
                    )
                )

                fileViewBinding.actionDetail.text = "Failed to download"
                fileViewBinding.actionSeparator.visibility = GONE
                fileViewBinding.actionSource.visibility = GONE
                fileViewBinding.actionProgressPercent.visibility = GONE
            }
            DOWNLOADED -> {
                val drawable = when (FileUtils.getFileType(item.mimeType)) {
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
                val totalMB = formatBytes(item.fileSize)
                fileViewBinding.actionTitle.text = item.fullFileName
                fileViewBinding.actionProgress.visibility = GONE
                fileViewBinding.actionDetail.text = totalMB
                fileViewBinding.actionSeparator.visibility = VISIBLE
                fileViewBinding.actionSource.visibility = VISIBLE
                fileViewBinding.actionSource.text = item.downloadUrl
                fileViewBinding.actionProgressPercent.visibility = GONE
            }
        }
        fileViewBinding.actionIcon.setOnClickListener {
            itemClickListener?.onFileDownloadItemClick(item, false)
        }

        fileViewBinding.actionClose.setOnClickListener {
            itemClickListener?.onFileDownloadItemClick(item, true)
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    internal class DownloadFileViewHolder(val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root)

}
