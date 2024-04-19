package com.solvek.bletrigger.manager

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import com.solvek.bletrigger.application.BleTriggerApplication.Companion.logViewModel
import java.util.UUID

class BluetoothManager private constructor(context: Context) {

    private var bluetoothGatt: BluetoothGatt? = null
    private var callback: ScanCallback? = null

    private val bluetoothAdapter by lazy {
        (context.getSystemService(
            ComponentActivity.BLUETOOTH_SERVICE
        ) as android.bluetooth.BluetoothManager).adapter
    }

    private val idleStateFilters by lazy {
        mutableListOf<ScanFilter>().apply {
            val filterShortServiceUUID = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(HEART_RATE_SERVICE_UUID))
                .setManufacturerData(2957, byteArrayOf(64, 86, 97, 19, 85, 80, 1, 0))
                .build()
            add(filterShortServiceUUID)
        }
    }

    fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    private val settings by lazy {
        ScanSettings.Builder()
            .setLegacy(false)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
    }

    fun startScanWithCallback(
        context: Context,
        scanCallback: ScanCallback? = callback
    ) {
        try {
            bluetoothAdapter.bluetoothLeScanner.startScan(
                idleStateFilters,
                settings,
                scanCallback
            )
            context.logViewModel.append("Scan started")
            Log.i(TAG, "scan started")
            this.callback = scanCallback
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun stopScanWithCallback() {
        try {
            callback?.let {
                bluetoothAdapter.bluetoothLeScanner.stopScan(it)
                Log.i(TAG, "scan stopped")
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
            bluetoothGatt = device.connectGatt(context, false, bluetoothGattCallback)
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun discoverServices() {
        try {
            bluetoothGatt?.discoverServices()
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

    fun closeGatt() {
        try {
            bluetoothGatt?.close()
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    fun readTime() {
        try {
            val characteristic = bluetoothGatt?.getService(UUID.fromString(TIME_SERVICE))
                ?.getCharacteristic(UUID.fromString(TIME_CHARACTERISTIC))
            val value = bluetoothGatt?.readCharacteristic(characteristic)
        } catch (error: SecurityException) {
            error("Scan is only allowed if app has needed permissions")
        }
    }

    companion object {
        private const val TAG = "BluetoothManager"

        const val SCAN_REPORT_DELAY_MS = 10000L

        const val HEART_RATE_SERVICE_UUID = "0000180D-0000-1000-8000-00805F9B34FB"
        const val TIME_SERVICE = "92fc3961-6a47-43d4-82b0-4677de96378b"
        const val TIME_CHARACTERISTIC = "a4e232e7-5ab0-4687-bddd-f1349c68247e"

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