package com.solvek.bletrigger.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.solvek.bletrigger.application.BleTriggerApplication.Companion.logViewModel
import com.solvek.bletrigger.manager.BluetoothManager

private const val TAG = "Scanner"

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

fun ComponentActivity.startScanner() {
    if (!checkForPermissions(permissionsToCheck)) {
        return
    }

    with(logViewModel) {
        if (!haveBtPermissions) {
            haveBtPermissions = true
            clear()
        }
    }
    BluetoothManager.getDefaultInstance().startScan()
    Log.i(TAG, "Scanner started")
}

private fun ComponentActivity.checkForPermissions(permissions: Array<String>): Boolean {
    if (permissions.all { permission ->
            ActivityCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }) {
        return true
    }

    with(logViewModel) {
        append("Please grant permissions and restart app!")
        haveBtPermissions = false
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.all { it.value }) {
                startScanner()
            }
        }.launch(permissions)
    }

    return false
}