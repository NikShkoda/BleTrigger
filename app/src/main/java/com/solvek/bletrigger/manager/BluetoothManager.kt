package com.solvek.bletrigger.manager

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import java.util.UUID

class BluetoothManager private constructor(context: Context) {

    private var isScanning = false

    private val bluetoothAdapter by lazy {
        (context.getSystemService(
            ComponentActivity.BLUETOOTH_SERVICE
        ) as android.bluetooth.BluetoothManager).adapter
    }

    private val idleStateFilters by lazy {
        mutableListOf<ScanFilter>().apply {
            val filterShortServiceUUID = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(HEART_RATE_SERVICE_UUID))
                .setManufacturerData(
                    2957,
                    byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
                    byteArrayOf(0, 0, 0, 0, 0, 0, 1, 1)
                )
                .build()
            add(filterShortServiceUUID)
        }
    }

    private val dataStateFilters by lazy {
        mutableListOf<ScanFilter>().apply {
            val filterShortServiceUUID = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(HEART_RATE_SERVICE_UUID))
                .setManufacturerData(
                    2957,
                    byteArrayOf(0, 0, 0, 0, 0, 0, 1, 0),
                    byteArrayOf(0, 0, 0, 0, 0, 0, 1, 1)
                )
                .build()
            add(filterShortServiceUUID)
        }
    }

    private val settings by lazy {
        ScanSettings.Builder()
            .setLegacy(true)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
    }

    fun scanForData(
        scanCallback: ScanCallback
    ) {
        try {
            bluetoothAdapter.bluetoothLeScanner.startScan(
                dataStateFilters,
                settings,
                scanCallback
            )
            Log.i(TAG, "scan started")
            isScanning = true
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun scanForIdle(
        scanCallback: ScanCallback
    ) {
        try {
            bluetoothAdapter.bluetoothLeScanner.startScan(
                idleStateFilters,
                settings,
                scanCallback
            )
            Log.i(TAG, "scan started")
            isScanning = true
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun stopScan(callback: ScanCallback) {
        try {
            if (isScanning) {
                bluetoothAdapter.bluetoothLeScanner.flushPendingScanResults(callback)
                bluetoothAdapter.bluetoothLeScanner.stopScan(callback)
                Log.i(TAG, "scan stopped")
                isScanning = false
            }
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun connectToDevice(
        context: Context,
        address: String,
        bluetoothGattCallback: BluetoothGattCallback
    ) {
        try {
            val device = bluetoothAdapter.getRemoteDevice(address)
            device.connectGatt(context, false, bluetoothGattCallback)
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun discoverServices(bluetoothGatt: BluetoothGatt) {
        try {
            bluetoothGatt.discoverServices()
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun disconnectDevice(bluetoothGatt: BluetoothGatt) {
        try {
            bluetoothGatt.disconnect()
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun readTime(gatt: BluetoothGatt) {
        try {
            val characteristic = gatt.getService(UUID.fromString(READ_TIME_SERVICE))
                ?.getCharacteristic(UUID.fromString(READ_TIME_CHARACTERISTIC))
            gatt.readCharacteristic(characteristic)
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun writeTime(gatt: BluetoothGatt, time: ByteArray) {
        try {
            val characteristic = gatt.getService(UUID.fromString(WRITE_TIME_SERVICE))
                ?.getCharacteristic(UUID.fromString(WRITE_TIME_CHARACTERISTIC))
                ?: error("Characteristic should be present in device")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    characteristic,
                    time,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )

            } else {
                @Suppress("DEPRECATION")
                characteristic.setValue(time)
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(characteristic)
            }
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun closeGatt(gatt: BluetoothGatt) {
        try {
            gatt.close()
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    companion object {
        private const val TAG = "BluetoothManager"

        const val SCAN_REPORT_DELAY_MS = 10000L

        const val HEART_RATE_SERVICE_UUID = "0000180D-0000-1000-8000-00805F9B34FB"

        const val READ_TIME_SERVICE = "92fc3961-6a47-43d4-82b0-4677de96378b"
        const val READ_TIME_CHARACTERISTIC = "a4e232e7-5ab0-4687-bddd-f1349c68247e"

        const val WRITE_TIME_SERVICE = "c8050aac-b8cd-4e7e-8498-3b45b8642ae0"
        const val WRITE_TIME_CHARACTERISTIC = "6e2ca5c3-3365-4841-9df8-a40d8dca4879"

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