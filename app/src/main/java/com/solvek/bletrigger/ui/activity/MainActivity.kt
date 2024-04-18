package com.solvek.bletrigger.ui.activity

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.solvek.bletrigger.application.BleTriggerApplication.Companion.logViewModel
import com.solvek.bletrigger.service.ScannerForegroundService
import com.solvek.bletrigger.ui.content.MainContent
import com.solvek.bletrigger.ui.theme.BleTriggerTheme
import com.solvek.bletrigger.utils.grantPermissions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

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
            with(logViewModel) {
                if (!haveBtPermissions) {
                    haveBtPermissions = true
                    clear()
                }
            }
            lifecycleScope.launch {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                while (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    delay(100)
                }
                startForegroundService(
                    Intent(
                        this@MainActivity,
                        ScannerForegroundService::class.java
                    )
                )
            }
        }
    }
}