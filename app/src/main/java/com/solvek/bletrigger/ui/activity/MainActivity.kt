package com.solvek.bletrigger.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.solvek.bletrigger.application.BleTriggerApplication.Companion.logViewModel
import com.solvek.bletrigger.ui.content.MainContent
import com.solvek.bletrigger.ui.theme.BleTriggerTheme
import com.solvek.bletrigger.utils.startScanner


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

        startScanner()
    }
}