package com.aliabbas.aliabbasfiledownloader.ui.bottomsheet


import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.viewModels
import android.Manifest
import com.aliabbas.aliabbasfiledownloader.ui.viewmodel.AddDownloadViewModel
import com.aliabbas.aliabbasfiledownloader.R
import com.aliabbas.aliabbasfiledownloader.databinding.BottomSheetAddNewDownloadBinding
import com.aliabbas.aliabbasfiledownloader.modal.LinkInfoData
import com.aliabbas.aliabbasfiledownloader.ui.MainActivity
import com.aliabbas.aliabbasfiledownloader.utils.*
import com.aliabbas.aliabbasfiledownloader.utils.FileStatus.WAITING
import com.aliabbas.aliabbasfiledownloader.utils.FileUtils.getUriFromSharedPreferences
import com.aliabbas.aliabbasfiledownloader.utils.FileUtils.saveUriToSharedPreferences
import com.aliabbas.aliabbasfiledownloader.utils.FileUtils.unreachableDirectory
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class BottomSheetAddDownloadFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetAddNewDownloadBinding
    private val viewModel: AddDownloadViewModel by viewModels()
    private lateinit var permissionManager: PermissionManager
    private var currentLinkInfoData: LinkInfoData? = null
    val coroutineScope = CoroutineScope(Dispatchers.IO)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetAddNewDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnCancelListener {
            switchLayout(GET_LINK_LAYOUT)
        }
        val contentView = View.inflate(context, R.layout.bottom_sheet_add_new_download, null)
        dialog.setContentView(contentView)
        (contentView.parent as View).setBackgroundColor(Color.TRANSPARENT)
        val layoutParams = contentView.layoutParams as FrameLayout.LayoutParams
        layoutParams.gravity = Gravity.BOTTOM
        layoutParams.setMargins(0, 0, 0, 0)
        contentView.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.bg_bottom_sheet)
        return dialog
    }

    override fun getTheme(): Int {
        return R.style.BottomSheetDialogTheme
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        permissionManager = PermissionManager(this)
        switchLayout(GET_LINK_LAYOUT)
        initListener()
        initObserver()
    }

    private fun initObserver() {
        viewModel.errorObserver.observe(viewLifecycleOwner) {
            if (it.isError) {
                if (it.errorMessage != null) {
                    binding.errorMessage.text = it.errorMessage
                    switchLayout(GET_FAILURE_LAYOUT)
                }
//                dismiss()
            }
        }

        viewModel.linkInfoObserver.observe(viewLifecycleOwner) {
            binding.downloadLink.setText("")
            currentLinkInfoData = it
            showLinkData(it)
            switchLayout(GET_FILE_SAVE_DETAILS_LAYOUT)
        }

        viewModel.documentPathSelect.observe(viewLifecycleOwner) { docFile ->
            if (docFile?.uri?.path != null) {
                val folder = docFile.uri.path!!.split("/").last()
                if (unreachableDirectory.contains(folder)) {
                    showToast("Selected Directory is unreachable. Choose another directory.")
                } else {
                    binding.folderLocation.setText(folder)
                }
            }
        }

        viewModel.addDownloadEvent.observe(viewLifecycleOwner) {
            if (it >= 0) {
                Log.e("ADE", it.toString())
                if (activity != null) {
                    (activity as MainActivity).fetchNewData()
                }
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    checkNotifPermission()
                }
                binding.titleGrabbingInfo.text = "Adding..."
                switchLayout(GET_GRABBING_INFO_LAYOUT)

                coroutineScope.launch {
                    ServiceUtil.restartFileDownloadService(
                        requireActivity().applicationContext,
                        WAITING
                    )
                    delay(200)
                    withContext(Dispatchers.Main) {
                        switchLayout(GET_SUCCESS_LAYOUT)
                    }
                }

            } else {
                showToast("Error $it")
            }
        }
    }
    val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!it) {
            Snackbar.make(
                binding.root,
                "Please grant Notification permission from App Settings",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkNotifPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showLinkData(linkData: LinkInfoData) {
        binding.fileName.setText(linkData.fileName)
        binding.fileSize.text = linkData.fileSize
        binding.fileExtension.text = linkData.fileExtension
        val uriFromSP = getUriFromSharedPreferences(requireContext(), FILE_URI)
        if (uriFromSP != null) {
            binding.folderLocation.setText(uriFromSP.path!!.split("/").last())
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListener() {
        binding.linkField.setEndIconOnClickListener {
            binding.downloadLink.setText("")
        }
        binding.cancelButton.debounceOnClick {
            dismiss()
        }
        binding.btnClose.debounceOnClick {
            dismiss()
        }
        binding.addButton.debounceOnClick {
            if (binding.downloadLink.text.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Download link is empty", Toast.LENGTH_SHORT)
                    .show()
            }
            binding.titleGrabbingInfo.text = "Grabbing Info..."
            switchLayout(GET_GRABBING_INFO_LAYOUT)
            viewModel.fetchLinkInfo(binding.downloadLink.text.toString())
        }
        binding.folderLocation.setOnTouchListener { _, motionEvent ->
            Log.e("touchEvent", motionEvent.toString())
            if (motionEvent.action == MotionEvent.ACTION_UP)
                permissionManager.checkStoragePermission()
            true
        }
        binding.btnAdd.debounceOnClick {
            if (currentLinkInfoData != null) {
                val destinationUri = getUriFromSharedPreferences(requireContext(), FILE_URI)
                if (destinationUri == null) {
                    showToast("Set destination folder")
                    return@debounceOnClick
                }
                setDownloadFileName()
                viewModel.addDownload(
                    currentLinkInfoData!!, binding.wifiCheckbox.isChecked,
                    destinationUri
                )
            } else {
                showToast("Please try again")
            }
        }

        binding.btnCloseFailure.debounceOnClick {
            this.dismiss()
        }

        binding.btnBackFailure.debounceOnClick {
            binding.downloadLink.setText("")
            switchLayout(GET_LINK_LAYOUT)
        }

        binding.btnDone.debounceOnClick {
            dismiss()
        }
    }

    private fun setDownloadFileName() {
        val targetUri = getUriFromSharedPreferences(requireContext(), FILE_URI)
        val targetDocumentFile = DocumentFile.fromTreeUri(
            requireContext(),
            targetUri!!
        ) ?: return

        var finalName = binding.fileName.text.toString()
        val numberOfFileAlreadyExist = targetDocumentFile.listFiles()
            .filter { it.name?.startsWith(finalName) ?: false }.size
        finalName += if (numberOfFileAlreadyExist > 0) {
            " ($numberOfFileAlreadyExist).${currentLinkInfoData!!.fileExtension}"
        } else {
            ".${currentLinkInfoData!!.fileExtension}"
        }
        currentLinkInfoData!!.fullFileName = finalName.trim()
        Log.e("fileCreated", finalName)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_DIRECTORY_PICKUP && resultCode == Activity.RESULT_OK) {
            val treeUri = data?.data
            if (treeUri != null) {
                val flags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(treeUri, flags)
                saveUriToSharedPreferences(
                    requireContext().applicationContext,
                    FILE_URI,
                    treeUri
                )
                val documentTree = DocumentFile.fromTreeUri(requireContext(), treeUri)
                if (documentTree != null && documentTree.canWrite()) {
                    viewModel.setUserPath(documentTree)
                } else showToast("Write Permission Error. Select a different location")
            } else showToast("Failed to Select Directory")
        }
    }


    fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun switchLayout(id: Int) {
        when (id) {
            GET_LINK_LAYOUT -> {
                binding.getLinkLayout.visibility = VISIBLE
                binding.getGrabbingInfoLayout.visibility = GONE
                binding.getFileSaveDetailsLayout.visibility = GONE
                binding.getSuccessLayout.visibility = GONE
                binding.getFailedLayout.visibility = GONE
            }
            GET_GRABBING_INFO_LAYOUT -> {
                binding.getLinkLayout.visibility = GONE
                binding.getGrabbingInfoLayout.visibility = VISIBLE
                binding.getFileSaveDetailsLayout.visibility = GONE
                binding.getSuccessLayout.visibility = GONE
                binding.getFailedLayout.visibility = GONE
            }
            GET_FILE_SAVE_DETAILS_LAYOUT -> {
                binding.getLinkLayout.visibility = GONE
                binding.getGrabbingInfoLayout.visibility = GONE
                binding.getFileSaveDetailsLayout.visibility = VISIBLE
                binding.getSuccessLayout.visibility = GONE
                binding.getFailedLayout.visibility = GONE
            }
            GET_SUCCESS_LAYOUT -> {
                binding.getLinkLayout.visibility = GONE
                binding.getGrabbingInfoLayout.visibility = GONE
                binding.getFileSaveDetailsLayout.visibility = GONE
                binding.getSuccessLayout.visibility = VISIBLE
                binding.getFailedLayout.visibility = GONE
            }
            GET_FAILURE_LAYOUT -> {
                binding.getLinkLayout.visibility = GONE
                binding.getGrabbingInfoLayout.visibility = GONE
                binding.getFileSaveDetailsLayout.visibility = GONE
                binding.getSuccessLayout.visibility = GONE
                binding.getFailedLayout.visibility = VISIBLE
            }
        }
    }


}