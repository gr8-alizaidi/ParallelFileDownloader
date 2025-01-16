package com.aliabbas.aliabbasfiledownloader.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.aliabbas.aliabbasfiledownloader.R
import com.aliabbas.aliabbasfiledownloader.adapter.DownloadDateCardAdapter
import com.aliabbas.aliabbasfiledownloader.broadcast.InternalAppBroadcast
import com.aliabbas.aliabbasfiledownloader.ui.bottomsheet.BottomSheetAddDownloadFragment
import com.aliabbas.aliabbasfiledownloader.databinding.ActivityMainBinding
import com.aliabbas.aliabbasfiledownloader.interfaces.FileItemClickListener
import com.aliabbas.aliabbasfiledownloader.modal.FileAction
import com.aliabbas.aliabbasfiledownloader.modal.FileDownloadState
import com.aliabbas.aliabbasfiledownloader.ui.bottomsheet.BottomSheetSettingsFragment
import com.aliabbas.aliabbasfiledownloader.ui.viewmodel.MainViewModel
import com.aliabbas.aliabbasfiledownloader.utils.*
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.CANCEL
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.DOWNLOADED
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.DOWNLOADING
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.FAILED
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.PAUSE
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.PAUSE_ALL
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.RESUME
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.RESUME_ALL
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.WAITING
import com.aliabbas.aliabbasfiledownloader.utils.FileUtils.clearDebris
import com.aliabbas.aliabbasfiledownloader.utils.FileUtils.openFile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var addNewDownloadBS: BottomSheetAddDownloadFragment? = null
    private var settingBS: BottomSheetSettingsFragment? = null
    private val viewModel: MainViewModel by viewModels()
    private lateinit var downloadDateCardAdapter: DownloadDateCardAdapter
    private val handler = Handler()
    private var debounceRunnable: Runnable? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupPopupMenu()
        setUpRecyclerView()
        initListener()
        initObserver()
    }

    private fun setUpRecyclerView() {
        downloadDateCardAdapter = DownloadDateCardAdapter(this)
        downloadDateCardAdapter.setInnerRecyclerViewClickListener(object : FileItemClickListener {
            override fun onFileDownloadItemClick(
                fileItem: FileDownloadState,
                isCancelled: Boolean
            ) {
                Log.e("item clicked", "file item - $fileItem  isCancelled - $isCancelled")
                if (isCancelled) {
                    viewModel.removeDownload(fileItem, this@MainActivity)
                } else {
                    when (fileItem.fileStatus) {
                        WAITING -> {
                            Log.e("touch", "waiting")
                            viewModel.pauseDownload(fileItem.id)
                        }
                        PAUSE -> {
                            Log.e("touch", "pause")
                            InternalAppBroadcast.messageToService(
                                this@MainActivity,
                                RESUME,
                                fileItem.id
                            )
                        }
                        DOWNLOADED -> {
                            Log.e("touch", "open")
                            openFile(this@MainActivity, fileItem.fullFileName, fileItem.mimeType)
                        }
                        FAILED -> {
                            Log.e("touch", "failed")
                            clearDebris(this@MainActivity, fileItem.fullFileName)
                            viewModel.putFileToWaiting(fileItem)
                            fileItem.fileStatus = WAITING

                            downloadDateCardAdapter.updateFileDownloadProgress(
                                FileAction(
                                    fileItem.id,
                                    0,
                                    "Waiting in queue",
                                    fileItem.fileDownloadProgress == -1,
                                    WAITING
                                )
                            )

                        }
                        else -> {
                            Log.e("touch", "else")
                            InternalAppBroadcast.messageToService(
                                this@MainActivity,
                                PAUSE,
                                fileItem.id
                            )
                        }
                    }
                }
            }
        })

        binding.downloadList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.downloadList.adapter = downloadDateCardAdapter

    }

    private fun initObserver() {
        viewModel.downloadsLiveData.observe(this) {
            Log.e("downloadList", it.toString())
            if (it.isNotEmpty()) {
                binding.noDownload.visibility = GONE
                binding.downloadContent.visibility = VISIBLE
            } else {
                binding.noDownload.visibility = VISIBLE
                binding.downloadContent.visibility = GONE
            }
            downloadDateCardAdapter.updateDownloadList(it)
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("${baseContext?.packageName}.MY_NOTIFICATION_BROADCAST_ALI")
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)
        viewModel.getUsersDownloads()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e("OnReceiveMA", "hre")
            val fileActionData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent?.getParcelableExtra(FILE_ACTION_DATA, FileAction::class.java)
            else
                intent?.getParcelableExtra(FILE_ACTION_DATA) as? FileAction?

            Log.e("fileActionData", fileActionData.toString())
            if (fileActionData != null) {
                Log.e("maBR", fileActionData?.action.toString())
                if (fileActionData.fileStatus == CANCEL) {
                    viewModel.removeDownload(
                        FileDownloadState(
                            fileActionData.fileId,
                            "",
                            "",
                            "",
                            "",
                            0,
                            true,
                            false,
                            CANCEL,
                            0,
                            fileActionData.interruptedBy,
                            -1,
                            null
                        ), this@MainActivity
                    )
                } else if (fileActionData.fileStatus == UPDATE_ALL) {
                    viewModel.getUsersDownloads()
                } else
                    downloadDateCardAdapter.updateFileDownloadProgress(fileActionData)
            }
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        lifecycleScope.launch {
            ServiceUtil.restartFileDownloadService(applicationContext, FileStatus.DOWNLOADED)
        }
    }

    fun fetchNewData() = viewModel.getUsersDownloads()

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    private fun initListener() {
        binding.addDownloadBtn.debounceOnClick {
            if (addNewDownloadBS == null) {
                addNewDownloadBS = BottomSheetAddDownloadFragment()
            }
            addNewDownloadBS?.show(supportFragmentManager, "bottomSheetAddDownload")

        }
    }

    private fun setupPopupMenu() {
        val popupMenu =
            PopupMenu(ContextThemeWrapper(this, R.style.popupMenuStyle), binding.menuBtn)
        popupMenu.inflate(R.menu.menu)
        popupMenu.gravity = Gravity.END
        popupMenu.setOnMenuItemClickListener { item ->
            debounceRunnable?.let { handler.removeCallbacks(it) }

            debounceRunnable = Runnable {
                when (item.itemId) {
                    R.id.action_pause_all -> {
                        InternalAppBroadcast.messageToService(context = this, action = PAUSE_ALL)
                    }
                    R.id.action_resume_all -> {
                        InternalAppBroadcast.messageToService(context = this, action = RESUME_ALL)
                    }
                    R.id.action_settings -> {
                        if (settingBS == null) {
                            settingBS = BottomSheetSettingsFragment()
                        }
                        settingBS?.show(supportFragmentManager, "bottomSheetSettingsFragment")
                    }
                }
                debounceRunnable = null
            }

            handler.postDelayed(debounceRunnable!!, 300) // Adjust the delay duration as needed

            true
        }

        binding.menuBtn.setOnClickListener {
            try {
                val popup = PopupMenu::class.java.getDeclaredField("mPopup")
                popup.isAccessible = true
                val menu = popup.get(popupMenu)
                menu.javaClass
                    .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(menu, true)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                popupMenu.show()
            }
        }
    }
}