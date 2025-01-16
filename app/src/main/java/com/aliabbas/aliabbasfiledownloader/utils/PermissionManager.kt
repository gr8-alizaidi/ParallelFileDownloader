package com.aliabbas.aliabbasfiledownloader.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class PermissionManager(
    private val fragment: Fragment
) {

    private val requestPermissionLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val isStoragePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
            if (isStoragePermissionGranted) {
                openDirectoryPicker()
            } else {
                val isPermissionPermanentlyDenied =
                    !fragment.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                showPermissionDeniedDialog(isPermissionPermanentlyDenied)
            }
        }

    private val openSettingsLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        openDirectoryPicker()
                    } else {
                        showPermissionDeniedDialog(true)
                    }
                }
            }
        }

    fun checkStoragePermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openDirectoryPicker()

        }
        else {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE

            if (ContextCompat.checkSelfPermission(
                    fragment.requireContext(),
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openDirectoryPicker()
            } else {
                val permissionsToRequest = arrayOf(permission)
                requestPermissionLauncher.launch(permissionsToRequest)
            }
        }
    }

    private fun showPermissionDeniedDialog(isPermissionPermanentlyDenied: Boolean) {
        val builder = AlertDialog.Builder(fragment.requireContext())
        builder.setTitle("Permission Denied")
            .setMessage("To grant permission, please go to the app settings and enable the storage permission.")
            .setPositiveButton("Go to Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", fragment.requireContext().packageName, null)
        intent.data = uri
        openSettingsLauncher.launch(intent)
    }

    private fun openDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        fragment.startActivityForResult(intent, REQUEST_CODE_DIRECTORY_PICKUP)
    }

}
