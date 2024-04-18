package com.solvek.bletrigger.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.solvek.bletrigger.application.BleTriggerApplication.Companion.logViewModel

private val permissionsToCheck by lazy {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
    } else {
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.FOREGROUND_SERVICE
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE
            }
        }.toTypedArray()
    }
}

fun ComponentActivity.grantPermissions(onGranted: () -> Unit) {
    if (!checkForPermissions(permissionsToCheck, onGranted)) {
        return
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
                    askToDisableBatteryOptimization(this)
                    onGranted()
                }
            }

        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { isGranted ->
            if (isGranted.isNotEmpty() && isGranted.all { granted -> granted.value }) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    askToDisableBatteryOptimization(this)
                    onGranted()
                }
            }
        }.launch(permissions)
    }

    return false
}

@SuppressLint("BatteryLife")
private fun askToDisableBatteryOptimization(context: Context) {
    val intent = Intent()
    val pm: PowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (pm.isIgnoringBatteryOptimizations(context.packageName)) {
        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    } else {
        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        intent.data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}