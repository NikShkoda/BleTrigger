package com.solvek.bletrigger.service

import android.annotation.SuppressLint
import android.app.AlarmManager
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
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.solvek.bletrigger.R
import com.solvek.bletrigger.application.BleTriggerApplication.Companion.logViewModel
import com.solvek.bletrigger.manager.BluetoothManager
import com.solvek.bletrigger.ui.activity.MainActivity
import com.solvek.bletrigger.ui.viewmodel.LogViewModel
import com.solvek.bletrigger.utils.BLE_WORK_CONNECT
import com.solvek.bletrigger.worker.SendRequestWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration

class ScannerForegroundService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val localBinder = LocalBinder()
    private val lock = Mutex()

    private lateinit var notificationManager: NotificationManager
    private lateinit var powerManager: PowerManager
    private lateinit var callback: ScanCallback

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                scope.launch {
                    lock.withLock {
                        logViewModel.append("Device is advertising 0100")
                        WorkManager.getInstance(applicationContext)
                            .enqueueUniqueWork(
                                BLE_WORK_CONNECT,
                                ExistingWorkPolicy.REPLACE,
                                OneTimeWorkRequestBuilder<SendRequestWorker>()
                                    .setInputData(
                                        Data.Builder().putString(
                                            SendRequestWorker.PARAM_DEVICE_ADDRESS,
                                            result.device.address
                                        ).build()
                                    )
                                    .setConstraints(
                                        Constraints.Builder()
                                            .setRequiredNetworkType(NetworkType.CONNECTED).build()
                                    ).build()
                            )
                        logViewModel.onDevice(
                            result.device.address.replace(
                                ":",
                                ""
                            )
                        )
                        BluetoothManager.getDefaultInstance().stopScan(callback)
                        logViewModel.append("Scanner stopped")
                    }
                }
            }
        }
        scope.launch {
            logViewModel.state.collectLatest { state ->
                when (state) {
                    LogViewModel.STATE.STATE_IDLE -> {
                        logViewModel.append("Scanner started")
                        BluetoothManager.getDefaultInstance().scanForData(callback)
                    }

                    LogViewModel.STATE.STATE_DATA -> {
                        WorkManager.getInstance(applicationContext)
                            .getWorkInfosForUniqueWorkFlow(BLE_WORK_CONNECT)
                            .collectLatest { result ->
                                if (result.all { it.state == WorkInfo.State.SUCCEEDED }) {
                                    logViewModel.onState(LogViewModel.STATE.STATE_IDLE)
                                    logViewModel.append("Device is advertising 0000")
                                }
                            }
                    }
                }
            }
        }

        scope.launch {
            while (true) {
                delay(Duration.ofMinutes(5).toMillis())
                if(powerManager.isDeviceIdleMode) {
                    logViewModel.append("App is in doze mode!")
                }
                logViewModel.append("App is still working!")
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val notification = generateNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        logViewModel.append("Revive app!")
        val restartServiceIntent =
            Intent(applicationContext, ScannerForegroundService::class.java).also {
                it.setPackage(packageName)
            }
        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(
            this, 1, restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        applicationContext.getSystemService(Context.ALARM_SERVICE);
        val alarmService: AlarmManager =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        )
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