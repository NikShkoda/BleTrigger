package com.solvek.bletrigger.worker

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.solvek.bletrigger.application.BleTriggerApplication.Companion.logViewModel
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

    private lateinit var connectContinuation: CancellableContinuation<ContinuationResult>
    private lateinit var disconnectContinuation: CancellableContinuation<ContinuationResult>

    override suspend fun doWork(): Result {
        applicationContext.logViewModel.append("Started a worker")
        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectContinuation.resume(ContinuationResult.Success)
                        Log.i(TAG, "Connected to Gatt")
                        applicationContext.logViewModel.append("Connected to Gatt")
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        if (this@SendRequestWorker::disconnectContinuation.isInitialized) {
                            disconnectContinuation.resume(ContinuationResult.Success)
                        } else {
                            // This means device itself disconnect without our call
                            connectContinuation.resume(ContinuationResult.EndedEarlier)
                        }
                        Log.i(TAG, "Disconnected from Gatt")
                        applicationContext.logViewModel.append("Disconnected from Gatt")
                    }
                }
            }
        }
        val deviceAddress = inputData.getString(PARAM_DEVICE_ADDRESS)
            ?: error("Device should be discovered before connection")
        val result = suspendCancellableCoroutine { continuation ->
            this.connectContinuation = continuation
            BluetoothManager.getDefaultInstance().connectToDevice(
                applicationContext,
                deviceAddress,
                callback
            )
        }
        when (result) {
            ContinuationResult.EndedEarlier -> {
                makeRequest()
            }

            ContinuationResult.Success -> {
                delay(10000L)
                suspendCancellableCoroutine { continuation ->
                    this.disconnectContinuation = continuation
                    BluetoothManager.getDefaultInstance().disconnectDevice()
                }
                makeRequest()
            }
        }
        return Result.success()
    }

    private suspend fun makeRequest() {
        withContext(Dispatchers.IO) {
            applicationContext.logViewModel.append("Request started")
            val url = URL("https://google.com")
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.connect()
                val httpResponse = conn.getResponseCode()
                Log.i(TAG, "Request to $url was made with response : ${httpResponse}")
                applicationContext.logViewModel.append("Request finished successfuly")
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

    sealed class ContinuationResult {
        object Success : ContinuationResult()
        object EndedEarlier : ContinuationResult()
    }
}