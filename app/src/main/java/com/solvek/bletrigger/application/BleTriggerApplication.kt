package com.solvek.bletrigger.application

import android.app.Application
import android.content.Context
import com.solvek.bletrigger.manager.BluetoothManager
import com.solvek.bletrigger.ui.viewmodel.LogViewModel

class BleTriggerApplication : Application() {
    private lateinit var logViewModel: LogViewModel
    override fun onCreate() {
        super.onCreate()
        logViewModel = LogViewModel(this)
        initBluetoothManager()
    }

    private fun initBluetoothManager() {
        BluetoothManager.init(this)
    }

    companion object {
        val Context.logViewModel get() = (applicationContext as BleTriggerApplication).logViewModel
    }
}