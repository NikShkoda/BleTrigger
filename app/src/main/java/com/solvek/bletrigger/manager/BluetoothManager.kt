package com.solvek.bletrigger.manager

import android.app.PendingIntent
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import com.solvek.bletrigger.receiver.DeviceBroadcastReceiver

class BluetoothManager private constructor(context: Context) {

    private val scanPendingIntent by lazy {
        PendingIntent.getBroadcast(
            context, SCAN_REQUEST_CODE, Intent(context, DeviceBroadcastReceiver::class.java)
                .setAction("com.solvek.bletrigger.ACTION_FOUND"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private val bluetoothAdapter by lazy {
        (context.getSystemService(
            ComponentActivity.BLUETOOTH_SERVICE
        ) as android.bluetooth.BluetoothManager).adapter
    }

    private val filters by lazy {
        mutableListOf<ScanFilter>().apply {
            val filterShortServiceUUID = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(HEART_RATE_SERVICE_UUID))
                .build()
            add(filterShortServiceUUID)
        }
    }

    private val settings by lazy {
        ScanSettings.Builder()
            .setLegacy(false)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(SCAN_REPORT_DELAY_MS)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .build()
    }

    fun startScan(
        scanFilters: List<ScanFilter> = filters,
        scanSettings: ScanSettings = settings,
        pendingIntent: PendingIntent = scanPendingIntent
    ) {
        try {
            bluetoothAdapter.bluetoothLeScanner.startScan(
                scanFilters,
                scanSettings,
                pendingIntent
            )
            Log.i(TAG, "scan started")
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun stopScan() {
        try {
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanPendingIntent)
            Log.i(TAG, "scan stopped")
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    companion object {
        private const val TAG = "BluetoothManager"

        private const val SCAN_REPORT_DELAY_MS = 1000L
        private const val SCAN_REQUEST_CODE = 1

        const val HEART_RATE_SERVICE_UUID = "0000180D-0000-1000-8000-00805F9B34FB"

        @Volatile
        private var INSTANCE: BluetoothManager? = null

        fun init(context: Context) {
            INSTANCE ?: synchronized(this) {
                BluetoothManager(context).also { INSTANCE = it }
            }
        }

        fun getDefaultInstance(): BluetoothManager =
            INSTANCE ?: error("Bluetooth Manager should be initialized before usage")
    }
}