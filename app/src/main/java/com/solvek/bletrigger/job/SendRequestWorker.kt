package com.solvek.bletrigger.job

import android.bluetooth.BluetoothGattCallback
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.solvek.bletrigger.manager.BluetoothManager

class SendRequestWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        BluetoothManager.getDefaultInstance().stopScan()
        val deviceAddress = inputData.getString(PARAM_DEVICE_ADDRESS)
        BluetoothManager.getDefaultInstance().connectToDevice(
            applicationContext,
            deviceAddress,
            object: BluetoothGattCallback() {

            }
        )
        return Result.success()
    }

    companion object {
        const val PARAM_DEVICE_ADDRESS = "PARAM_DEVICE_ADDRESS"
    }
}