package com.solvek.bletrigger.manager

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import java.lang.reflect.Method
import java.util.UUID


class BluetoothManager private constructor(context: Context) {

    private val bluetoothManager by lazy {
        context.getSystemService(
            ComponentActivity.BLUETOOTH_SERVICE
        ) as android.bluetooth.BluetoothManager
    }

    private val bluetoothAdapter by lazy {
        bluetoothManager.adapter
    }

    private val idleStateFilters by lazy {
        mutableListOf<ScanFilter>().apply {
            val filterShortServiceUUID = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(HEART_RATE_SERVICE_UUID))
                /*.setManufacturerData(
                    2957,
                    byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
                    byteArrayOf(0, 0, 0, 0, 0, 0, 1, 1)
                )*/
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
            .setLegacy(false)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .build()
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothDevice: BluetoothDevice? = null

    fun scanForData(
        scanCallback: ScanCallback
    ) {
        try {
            bluetoothAdapter.bluetoothLeScanner.startScan(
                dataStateFilters,
                settings,
                scanCallback
            )
            Log.i(TAG, "scan started for data")
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
            Log.i(TAG, "scan started for idle")
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun stopScan(callback: ScanCallback) {
        try {
            bluetoothAdapter.bluetoothLeScanner.stopScan(callback)
            Log.i(TAG, "scan stopped")
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun connectToDevice(
        context: Context,
        address: String,
        bluetoothGattCallback: BluetoothGattCallback
    ): BluetoothGatt {
        try {
            val device = bluetoothAdapter.getRemoteDevice(address)
            if (bluetoothDevice == null) {
                bluetoothDevice = device
            }
            return bluetoothGatt?.let { bluetoothGatt ->
                bluetoothGatt.connect()
                return@let bluetoothGatt
            } ?: run {
                return@run device.connectGatt(
                    context,
                    false,
                    bluetoothGattCallback
                ).also {
                    bluetoothGatt = it
                }
            }
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun discoverServices(bluetoothGatt: BluetoothGatt) {
        try {
            (this.bluetoothGatt ?: bluetoothGatt).let { gatt ->
                if (gatt.services.isEmpty()) {
                    gatt.discoverServices()
                } else {
                    readTime(gatt)
                }
            }
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun disconnectDevice() {
        try {
            bluetoothGatt?.disconnect()
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun isDisconnected(): Boolean {
        try {
            return bluetoothManager.getConnectionState(
                bluetoothDevice,
                BluetoothProfile.GATT
            ) != BluetoothProfile.STATE_CONNECTED
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun readTime(gatt: BluetoothGatt): Boolean {
        try {
            val characteristic = gatt.getService(UUID.fromString(READ_TIME_SERVICE))
                ?.getCharacteristic(UUID.fromString(READ_TIME_CHARACTERISTIC))

            return (this.bluetoothGatt ?: gatt).readCharacteristic(characteristic)
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun closeGatt(gatt: BluetoothGatt) {
        try {
            (this.bluetoothGatt ?: gatt).disconnect()
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    private fun refresh(gatt: BluetoothGatt) {
        try {
            // BluetoothGatt gatt
            val refresh: Method = gatt.javaClass.getMethod("refresh")
            //noinspection ConstantConditions
            val success = refresh.invoke((this.bluetoothGatt ?: gatt)) as Boolean
            Log.i(TAG, "Refreshing result: $success")
        } catch (e: Exception) {
            Log.i(TAG, "error")
        }
    }

    companion object {
        private const val TAG = "BluetoothManager"

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