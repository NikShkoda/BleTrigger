package com.solvek.bletrigger.worker

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.solvek.bletrigger.manager.BluetoothManager
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume


class SendRequestWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private lateinit var connectContinuation: CancellableContinuation<BluetoothGatt>
    private lateinit var disconnectContinuation: CancellableContinuation<BluetoothGatt>

    override suspend fun doWork(): Result {
        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectContinuation.resume(gatt)
                        Log.i(TAG, "Connected to Gatt")
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        disconnectContinuation.resume(gatt)
                        Log.i(TAG, "Disconnected from Gatt")
                    }
                }
            }
        }
        val deviceAddress = inputData.getString(PARAM_DEVICE_ADDRESS)
            ?: error("Device should be discovered before connection")
        suspendCancellableCoroutine { continuation ->
            this.connectContinuation = continuation
            BluetoothManager.getDefaultInstance().connectToDevice(
                applicationContext,
                deviceAddress,
                callback
            )
        }
        delay(5000L)
        suspendCancellableCoroutine { continuation ->
            this.disconnectContinuation = continuation
            BluetoothManager.getDefaultInstance().disconnectDevice()
        }
        makeRequest()
        return Result.success()
    }

    private suspend fun makeRequest() {
        withContext(Dispatchers.IO) {
            val url = URL("https://google.com")
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.connect()
                val httpResponse = conn.getResponseCode()
                Log.i(TAG, "Request to $url was made with response : ${httpResponse}")
            } catch (error: Throwable) {
                Log.e(TAG, error.message ?: "Unknown error")
            }
        }
    }

    companion object {
        // For testing purposes. This way it's easier to spot request log
        private const val TAG = "DataHandler"
        const val PARAM_DEVICE_ADDRESS = "PARAM_DEVICE_ADDRESS"
    }
}