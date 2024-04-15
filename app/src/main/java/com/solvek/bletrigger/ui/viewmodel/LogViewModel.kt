package com.solvek.bletrigger.ui.viewmodel

import android.content.Context
import com.solvek.bletrigger.data.Registry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat

/**
 * Do not create instances of this class
 * Use Context.logViewModel to get a singleton instance
 */
class LogViewModel(context: Context) {

    private val prefs = context.getSharedPreferences("LogViewModel", 0)

    private val _log = MutableStateFlow("")
    val log = _log.asStateFlow()

    private val registry =
        Registry(prefs.getString(KEY_REGISTRY, REGISTRY_DEF_VALUE) ?: REGISTRY_DEF_VALUE)

    var haveBtPermissions
        get() = prefs.getBoolean(KEY_HAVE_BT_PERMISSION, true)
        set(value) {
            prefs.edit().putBoolean(KEY_HAVE_BT_PERMISSION, value).apply()
        }

    init {
        _log.value = prefs.getString(KEY_LOG, LOG_DEF_VALUE) ?: LOG_DEF_VALUE
    }

    fun onDevice(id: String, hasData: Boolean) {
        if (registry.isSameStatus(id, hasData)) {
            return
        }

        registry.store(id, hasData)

        prefs.edit().putString(KEY_REGISTRY, registry.toJson()).apply()

        if (hasData) {
            append("$id HAS data")
        } else {
            append("$id does NOT have data")
        }
    }

    fun append(message: String) {
        val row = "${TIME_FORMAT.format(System.currentTimeMillis())}: $message"
        val c = _log.value
        update(if (c.isBlank()) row else "$c\r\n$row")
    }

    fun clear() {
        update("")
    }

    private fun update(logContent: String) {
        _log.value = logContent
        prefs.edit().putString(KEY_LOG, logContent).apply()
    }

    companion object {
        private const val LOG_DEF_VALUE = ""
        private const val REGISTRY_DEF_VALUE = "{}"
        private const val KEY_LOG = "log_content"
        private const val KEY_HAVE_BT_PERMISSION = "have_bt_permissions"
        private const val KEY_REGISTRY = "registry"
        private val TIME_FORMAT = SimpleDateFormat.getDateTimeInstance()
    }
}