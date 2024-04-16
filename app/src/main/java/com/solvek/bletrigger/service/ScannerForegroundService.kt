package com.solvek.bletrigger.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.solvek.bletrigger.R
import com.solvek.bletrigger.manager.BluetoothManager
import com.solvek.bletrigger.ui.activity.MainActivity

class ScannerForegroundService : Service() {

    private val localBinder = LocalBinder()

    private lateinit var notificationManager: NotificationManager

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        BluetoothManager.getDefaultInstance().startScan()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

    private fun stopForeground() {
        stopForeground(STOP_FOREGROUND_REMOVE)
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