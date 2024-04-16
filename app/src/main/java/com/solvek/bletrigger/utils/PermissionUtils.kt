package com.solvek.bletrigger.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.solvek.bletrigger.application.BleTriggerApplication.Companion.logViewModel

private val permissionsToCheck by lazy {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    }
}

fun ComponentActivity.grantPermissions(onGranted: () -> Unit) {
    if (!checkForPermissions(permissionsToCheck, onGranted)) {
        return
    }

    with(logViewModel) {
        if (!haveBtPermissions) {
            haveBtPermissions = true
            clear()
        }
    }
    onGranted()
}

private fun ComponentActivity.checkForPermissions(
    permissions: Array<String>,
    onGranted: () -> Unit
): Boolean {
    if (permissions.isNotEmpty() && permissions.all { permission ->
            ActivityCompat.checkSelfPermission(
                this, permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    ) {
        return true
    }

    with(logViewModel) {
        append("Please grant permissions and restart app!")
        haveBtPermissions = false
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val backgroundLocationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    onGranted()
                }
            }
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { isGranted ->
            if (isGranted.isNotEmpty() && isGranted.all { granted -> granted.value }) {
                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {*/
                    onGranted()
                //}
            }
        }.launch(permissions)
    }

    return false
}