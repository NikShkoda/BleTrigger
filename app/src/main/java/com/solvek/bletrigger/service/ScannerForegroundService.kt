package com.solvek.bletrigger.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.solvek.bletrigger.R
import com.solvek.bletrigger.application.BleTriggerApplication.Companion.logViewModel
import com.solvek.bletrigger.manager.BluetoothManager
import com.solvek.bletrigger.ui.activity.MainActivity
import com.solvek.bletrigger.ui.viewmodel.LogViewModel
import com.solvek.bletrigger.utils.BLE_WORK_CONNECT
import com.solvek.bletrigger.utils.onFound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ScannerForegroundService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val localBinder = LocalBinder()
    private val lock = Mutex()

    private lateinit var notificationManager: NotificationManager
    private lateinit var callback: ScanCallback

    private var isDeviceFound = false
    private var numberOfAttempts: Int = 0

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                scope.launch {
                    lock.withLock {
                        if (!isDeviceFound) {
                            onFound(
                                applicationContext,
                                result
                            ) { hasData ->
                                if(hasData) {
                                    numberOfAttempts ++
                                    isDeviceFound = true
                                    BluetoothManager.getDefaultInstance().stopScan(callback)
                                    applicationContext.logViewModel.append("Scanner stopped")
                                    applicationContext.logViewModel.append("Setting device data flag to 0000, attempt number $numberOfAttempts")
                                } else {
                                    if(numberOfAttempts > 0) {
                                        numberOfAttempts = 0
                                        applicationContext.logViewModel.append("Resetting number of attempts to 0")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        scope.launch {
            applicationContext.logViewModel.state.collectLatest { state ->
                when (state) {
                    LogViewModel.STATE.STATE_IDLE -> {
                        applicationContext.logViewModel.append("Scanner started")
                        BluetoothManager.getDefaultInstance().scanForData(callback)
                    }

                    LogViewModel.STATE.STATE_DATA -> {
                        WorkManager.getInstance(applicationContext)
                            .getWorkInfosForUniqueWorkFlow(BLE_WORK_CONNECT)
                            .collectLatest { result ->
                                if (result.all { it.state == WorkInfo.State.SUCCEEDED }) {
                                    applicationContext.logViewModel.onState(LogViewModel.STATE.STATE_IDLE)
                                    isDeviceFound = false
                                }
                            }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val notification = generateNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return localBinder
    }

    private fun generateNotification(): Notification {
        val mainNotificationText = "App is working in background"
        val titleText = "BleTrigger"
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(notificationChannel)

        val launchActivityIntent = Intent(this, MainActivity::class.java)

        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        return notificationCompatBuilder
            .setContentTitle(titleText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentText(mainNotificationText)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_go_to,
                "Launch TriggerApp",
                activityPendingIntent
            )
            .build()
    }

    inner class LocalBinder : Binder() {
        internal val service: ScannerForegroundService
            get() = this@ScannerForegroundService
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "while_in_use_channel_01"
    }
}