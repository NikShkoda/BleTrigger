package com.solvek.bletrigger.receiver

import android.annotation.SuppressLint
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.util.size
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.solvek.bletrigger.application.BleTriggerApplication.Companion.logViewModel
import com.solvek.bletrigger.job.SendRequestWorker
import com.solvek.bletrigger.job.SendRequestWorker.Companion.PARAM_DEVICE_ADDRESS
import com.solvek.bletrigger.manager.BluetoothManager


class DeviceBroadcastReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FOUND -> {
                onFound(context, intent)
            }

            ACTION_DOZE_MODE -> {
                onDozeMode(context)
            }
        }
    }

    private fun onFound(context: Context, intent: Intent) {
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
            context.handleScanResult(context, idx, scanResult)
        }
    }

    private fun onDozeMode(context: Context) {
        var idx = 0
        BluetoothManager.getDefaultInstance().startScanWithCallback(
            scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    super.onScanResult(callbackType, result)
                    context.handleScanResult(context, idx, result)
                    idx++
                }
            }
        )
    }

    private fun Context.handleScanResult(context: Context, idx: Int, scanResult: ScanResult) {
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
        if (hasData) {
            createSendRequestWork(context, scanResult.device.address)
        }
    }

    private fun createSendRequestWork(context: Context, address: String) {
        val uploadWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<SendRequestWorker>()
                .setInputData(Data.Builder().putString(PARAM_DEVICE_ADDRESS, address).build())
                .build()
        WorkManager
            .getInstance(context)
            .enqueue(uploadWorkRequest)
    }

    companion object {
        private const val TAG = "DeviceBroadcastReceiver"
        private const val ACTION_FOUND = "com.solvek.bletrigger.ACTION_FOUND"
        private const val ACTION_DOZE_MODE = "com.solvek.bletrigger.DOZE_MODE"
    }
}