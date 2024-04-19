package com.solvek.bletrigger.utils

import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import androidx.core.util.size
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.solvek.bletrigger.application.BleTriggerApplication.Companion.logViewModel
import com.solvek.bletrigger.worker.SendRequestWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex

const val TAG = "DataHandler"
const val BLE_WORK = "BLE_WORK"

private val mutex = Mutex()

suspend fun onFound(context: Context, scanResult: ScanResult, onDeviceFound: () -> Unit) {
    if (context.logViewModel.isConnectionEnabled()) {
        context.handleScanResult(context, scanResult, onDeviceFound)
    }
}

private suspend fun Context.handleScanResult(
    context: Context,
    scanResult: ScanResult,
    onDeviceFound: () -> Unit
) {
    val address = scanResult.device.address.replace(":", "")
    Log.i(TAG, "Handling scan result $address")
    val sr = scanResult.scanRecord
    if (sr == null) {
        Log.w(TAG, "No scan record")
        return
    }
    val md = sr.manufacturerSpecificData
    mutex.lock()
    onDeviceFound()
    createSendRequestWork(context, scanResult.device.address)
    delay(5000)
    mutex.unlock()
    /*if (md.size != 1) {
        Log.w(
            TAG,
            "Manufacturer data contains ${md.size} item. Exactly one is expected"
        )
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
        mutex.lock()
        onDeviceFound()
        createSendRequestWork(context, scanResult.device.address)
        delay(5000)
        mutex.unlock()
    }*/
}

private fun createSendRequestWork(context: Context, address: String) {
    val uploadWorkRequest: OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<SendRequestWorker>()
            .setInputData(
                Data.Builder().putString(SendRequestWorker.PARAM_DEVICE_ADDRESS, address).build()
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
    WorkManager
        .getInstance(context)
        .enqueueUniqueWork(BLE_WORK, ExistingWorkPolicy.KEEP, uploadWorkRequest)
}