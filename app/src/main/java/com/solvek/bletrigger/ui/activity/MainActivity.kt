package com.solvek.bletrigger.ui.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.solvek.bletrigger.application.BleTriggerApplication.Companion.logViewModel
import com.solvek.bletrigger.manager.BluetoothManager
import com.solvek.bletrigger.service.ScannerForegroundService
import com.solvek.bletrigger.ui.content.MainContent
import com.solvek.bletrigger.ui.theme.BleTriggerTheme
import com.solvek.bletrigger.utils.grantPermissions


class MainActivity : ComponentActivity() {

    private var scannerServiceBound = false
    private var scannerService: ScannerForegroundService? = null

    private val scannerServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ScannerForegroundService.LocalBinder
            scannerService = binder.service
            scannerServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            scannerService = null
            scannerServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BleTriggerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(logViewModel)
                }
            }
        }

        grantPermissions {
            /*bindService(
                Intent(
                    this@MainActivity,
                    ScannerForegroundService::class.java
                ),
                scannerServiceConnection,
                Context.BIND_AUTO_CREATE
            )*/
            BluetoothManager.getDefaultInstance().startScanCallback()
        }
    }

    override fun onStop() {
        super.onStop()
        scannerService?.unbindService(scannerServiceConnection)
    }
}