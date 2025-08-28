package com.example.calculator.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.net.toUri

class StoragePermissionUtil(private val activity: AppCompatActivity) {

    private var onPermissionGranted: (() -> Unit)? = null

    fun requestStoragePermission(
        launcher: ActivityResultLauncher<Array<String>>,
        onGranted: () -> Unit
    ) {
        onPermissionGranted = onGranted

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Environment.isExternalStorageManager()) {
                    onGranted()
                } else {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = "package:${activity.packageName}".toUri()
                    }
                    activity.startActivity(intent)
                }
            }
            else -> {
                val permissions = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                launcher.launch(permissions)
            }
        }
    }

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val readPermission = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val writePermission = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            readPermission == PermissionChecker.PERMISSION_GRANTED &&
                    writePermission == PermissionChecker.PERMISSION_GRANTED
        }
    }

    fun handlePermissionResult(permissions: Map<String, Boolean>) {
        if (permissions.all { it.value }) {
            onPermissionGranted?.invoke()
        }
    }
}