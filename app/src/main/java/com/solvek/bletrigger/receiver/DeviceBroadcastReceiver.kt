package com.solvek.bletrigger.receiver

import android.annotation.SuppressLint
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.util.size
import com.solvek.bletrigger.application.BleTriggerApplication.Companion.logViewModel
import com.solvek.bletrigger.manager.BluetoothManager
import com.solvek.bletrigger.service.ScannerForegroundService
import com.solvek.bletrigger.ui.activity.WakeUpActivity
import java.util.concurrent.TimeUnit


class DeviceBroadcastReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "===== A new message received")

        val bleCallbackType: Int = intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1)
        if (bleCallbackType == -1) {
            Log.w(TAG, "Callback type not specified")
            return
        }
        Log.i(TAG, "Received callback type $bleCallbackType")

        val scanResults = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(
                BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT, ScanResult::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(
                BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT
            )
        }
        if (scanResults.isNullOrEmpty()) {
            Log.w(TAG, "No scan results in the message")
            return
        }

        Log.i(TAG, "Received ${scanResults.size} scan results")
        scanResults.forEachIndexed { idx, scanResult ->
            context.handleScanResult(idx, scanResult)
        }
        startWakeUpActivity(context)
    }

    private fun startWakeUpActivity(context: Context) {
        @Suppress("DEPRECATION")
        with(
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                ScannerForegroundService::class.java.name
            )
        ) {
            acquire(TimeUnit.SECONDS.toMillis(5))
            release()
        }
        BluetoothManager.getDefaultInstance().stopScan()
        context.startActivity(
            Intent(context, WakeUpActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun Context.handleScanResult(idx: Int, scanResult: ScanResult) {
        val address = scanResult.device.address.replace(":", "")
        Log.i(TAG, "Handling scan result $idx $address")
        val sr = scanResult.scanRecord
        if (sr == null) {
            Log.w(TAG, "No scan record")
            return
        }
        val md = sr.manufacturerSpecificData
        if (md.size != 1) {
            Log.w(TAG, "Manufacturer data contains ${md.size} item. Exactly one is expected")
            return
        }

        Log.i(TAG, "Manufacturer data key: ${md.keyAt(0)}")
        val bytes = md.valueAt(0)

        if (bytes.size != 8) {
            Log.e(TAG, "Provided data must contain exactly 8 bytes")
            return
        }

        val hasData = bytes[6].toInt() != 0 || bytes[7].toInt() != 0
        Log.i(TAG, "Has data status: $hasData")
        logViewModel.onDevice(address, hasData)
    }

    companion object {
        private const val TAG = "DeviceBroadcastReceiver"
    }
}