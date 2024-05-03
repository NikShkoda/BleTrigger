package com.solvek.bletrigger.worker

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.solvek.bletrigger.R
import com.solvek.bletrigger.application.BleTriggerApplication.Companion.logViewModel
import com.solvek.bletrigger.manager.BluetoothManager
import com.solvek.bletrigger.service.ScannerForegroundService
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

    private lateinit var scanContinuation: CancellableContinuation<ContinuationResult>

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                BluetoothManager.getDefaultInstance().stopScan(this)
                applicationContext.logViewModel.append("Scan ended!")
                scanContinuation.resume(ContinuationResult.ScanSuccess(result.device))
            }
        }
        val scanResult = suspendCancellableCoroutine { scanContinuation ->
            this.scanContinuation = scanContinuation
            BluetoothManager.getDefaultInstance().scanForData(scanCallback)
            applicationContext.logViewModel.append("Scan started!")
        }
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            Log.i(TAG, "Connected to patch")
                            applicationContext.logViewModel.append("Connected to patch device!")
                            BluetoothManager.getDefaultInstance().discoverServices(gatt)
                        }

                        BluetoothProfile.STATE_DISCONNECTED -> {
                            Log.i(TAG, "Disconnected from patch")
                            applicationContext.logViewModel.append("Disconnected from patch device!")
                        }
                    }
                } else {
                    BluetoothManager.getDefaultInstance().disconnectDevice()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothManager.getDefaultInstance().readTime(gatt)
                }
            }


            // Other version did not work on some devices
            @Suppress("DEPRECATION")
            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                super.onCharacteristicRead(gatt, characteristic, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (characteristic.uuid == UUID.fromString(BluetoothManager.READ_TIME_CHARACTERISTIC)) {
                        BluetoothManager.getDefaultInstance().disconnectDevice()
                        val buffer = ByteBuffer.wrap(characteristic.value)
                        val deviceTime = buffer.getTime()
                        applicationContext.logViewModel.append(
                            "Patch time is: ${
                                applicationContext.logViewModel.formatTime(
                                    deviceTime
                                )
                            }"
                        )
                    }
                }
            }
        }
        BluetoothManager.getDefaultInstance().connectToDevice(
            applicationContext,
            (scanResult as ContinuationResult.ScanSuccess).device.address,
            gattCallback
        )
        var disconnectAttempt = 0
        delay(10000L)
        while (!BluetoothManager.getDefaultInstance().isDisconnected()) {
            BluetoothManager.getDefaultInstance().disconnectDevice()
            disconnectAttempt++
            applicationContext.logViewModel.append("Disconnecting again, attempt number $disconnectAttempt")
            delay(2000L)
        }
        //makeRequest()
        return Result.success()
    }

    // Creates an instance of ForegroundInfo which can be used to update the
    // ongoing notification.
    private fun createForegroundInfo(): ForegroundInfo {
        val mainNotificationText = "App is working in background"
        val titleText = "BleTrigger"
        val notificationChannel = NotificationChannel(
            ScannerForegroundService.NOTIFICATION_CHANNEL_ID,
            titleText,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)

        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(getId())

        val notificationCompatBuilder =
            NotificationCompat.Builder(
                applicationContext,
                ScannerForegroundService.NOTIFICATION_CHANNEL_ID
            )

        val notification = notificationCompatBuilder
            .setContentTitle(titleText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentText(mainNotificationText)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_go_to,
                "Stop worker",
                intent
            )
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
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

    private fun ByteBuffer.skip(amount: Int) {
        this.position(this.position() + amount)
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
        private const val TAG = "BluetoothManager"
        const val PARAM_DEVICE_ADDRESS = "PARAM_DEVICE_ADDRESS"
        private const val NOTIFICATION_ID = 2
    }

    sealed class ContinuationResult {
        data class ScanSuccess(val device: BluetoothDevice) : ContinuationResult()
        data class ConnectionSuccess(val gatt: BluetoothGatt) : ContinuationResult()
    }
}