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

const val TAG = "BluetoothManager"
const val BLE_WORK_CONNECT = "BLE_WORK_CONNECT"

fun onFound(context: Context, scanResult: ScanResult, hasData: Boolean) {
    if (context.logViewModel.isConnectionEnabled()) {
        context.handleScanResult(context, scanResult, hasData)
    }
}

private fun Context.handleScanResult(
    context: Context,
    scanResult: ScanResult,
    hasData: Boolean
) {
    if (hasData) {
        createSendRequestWork(context, scanResult.device.address)
    }
    Log.i(TAG, "Has data status: $hasData")
    logViewModel.onDevice(scanResult.device.address, hasData)
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
        .enqueueUniqueWork(BLE_WORK_CONNECT, ExistingWorkPolicy.KEEP, uploadWorkRequest)
}