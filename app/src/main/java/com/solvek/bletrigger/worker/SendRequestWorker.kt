package com.solvek.bletrigger.worker

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.coroutines.resume


class SendRequestWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private lateinit var connectContinuation: CancellableContinuation<ContinuationResult>
    private lateinit var disconnectContinuation: CancellableContinuation<ContinuationResult>

    @SuppressLint("MissingPermission")
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
                        Log.i(TAG, "Connected to Gatt")
                        applicationContext.logViewModel.append("Connected to Gatt")
                        BluetoothManager.getDefaultInstance().discoverServices()
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.i(TAG, "Disconnected from Gatt")
                        applicationContext.logViewModel.append("Disconnected from Gatt")
                        BluetoothManager.getDefaultInstance().closeGatt()
                        if (this@SendRequestWorker::disconnectContinuation.isInitialized) {
                            disconnectContinuation.resume(ContinuationResult.Success(gatt))
                        }
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                BluetoothManager.getDefaultInstance().readTime()
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                super.onCharacteristicRead(gatt, characteristic, value, status)
                if (characteristic.uuid == UUID.fromString(BluetoothManager.TIME_CHARACTERISTIC)) {
                    val buffer = ByteBuffer.wrap(value)
                    val deviceTime = buffer.getTime()
                    applicationContext.logViewModel.append(
                        "Patch time is: ${
                            applicationContext.logViewModel.formatTime(
                                deviceTime
                            )
                        }\nAndroid device time is: ${
                            applicationContext.logViewModel.formatTime(
                                System.currentTimeMillis()
                            )
                        }"
                    )
                    connectContinuation.resume(ContinuationResult.Success(gatt))
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

            is ContinuationResult.Success -> {
                delay(10000L)
                suspendCancellableCoroutine { continuation ->
                    this.disconnectContinuation = continuation
                    result.gatt.disconnect()
                    result.gatt.close()
                }
                makeRequest()
            }
        }
        return Result.success()
    }

    private fun ByteBuffer.getTime(): Long {
        val b = ByteBuffer.allocate(8)
        b.order(ByteOrder.LITTLE_ENDIAN)
        b.put(this.array(), position(), 6)
        b.put(0)
        b.put(0)
        b.position(0)
        skip(6)
        return b.long
    }

    private fun ByteBuffer.skip(amount : Int) {
        this.position(this.position()+amount)
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
        data class Success(val gatt: BluetoothGatt) : ContinuationResult()
        object EndedEarlier : ContinuationResult()
    }
}