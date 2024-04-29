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
    val log
        get() = _log.asStateFlow()

    private val _connectionEnabled = MutableStateFlow(false)
    val connectionEnabled
        get() = _connectionEnabled.asStateFlow()

    private val _state = MutableStateFlow(STATE.STATE_IDLE)
    val state
        get() = _state.asStateFlow()

    private val registry =
        Registry(prefs.getString(KEY_REGISTRY, REGISTRY_DEF_VALUE) ?: REGISTRY_DEF_VALUE)

    var haveBtPermissions
        get() = prefs.getBoolean(KEY_HAVE_BT_PERMISSION, true)
        set(value) {
            prefs.edit().putBoolean(KEY_HAVE_BT_PERMISSION, value).apply()
        }

    init {
        _log.value = prefs.getString(KEY_LOG, LOG_DEF_VALUE) ?: LOG_DEF_VALUE
        _connectionEnabled.value = prefs.getBoolean(KEY_CONNECTION_ENABLED, true)
        _state.value = STATE.STATE_IDLE
    }

    fun isConnectionEnabled(): Boolean {
        return _connectionEnabled.value
    }

    fun getState(): STATE {
        return state.value
    }

    fun onDevice(id: String) {
        _state.value = STATE.STATE_DATA
        if (registry.isSameStatus(id, true)) {
            return
        }

        registry.store(id, true)

        prefs.edit().putString(KEY_REGISTRY, registry.toJson()).apply()
    }

    fun onState(state: STATE) {
        _state.value = state
    }

    fun append(message: String) {
        val row = "${formatTime(System.currentTimeMillis())}: $message"
        val c = _log.value
        update(if (c.isBlank() || c == LOG_DEF_VALUE) row else "$c\r\n$row")
    }

    fun formatTime(timeMs: Long): String {
        return TIME_FORMAT.format(timeMs)
    }

    fun clear() {
        update(LOG_DEF_VALUE)
    }

    fun setConnectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONNECTION_ENABLED, enabled).apply()
    }

    private fun update(logContent: String) {
        _log.value = logContent
        prefs.edit().putString(KEY_LOG, logContent).apply()
    }

    companion object {
        private const val LOG_DEF_VALUE = "No logs to show yet"
        private const val REGISTRY_DEF_VALUE = "{}"
        private const val KEY_LOG = "log_content"
        private const val KEY_CONNECTION_ENABLED = "connection_enabled"
        private const val KEY_HAVE_BT_PERMISSION = "have_bt_permissions"
        private const val KEY_REGISTRY = "registry"
        private val TIME_FORMAT = SimpleDateFormat.getDateTimeInstance()
    }

    enum class STATE {
        STATE_IDLE, STATE_DATA;
    }
}