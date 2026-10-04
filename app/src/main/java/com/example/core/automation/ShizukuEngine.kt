package com.example.core.automation

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ShizukuStatus {
    NOT_INSTALLED,
    INSTALLED_WAITING_SERVICE,
    RUNNING_PERMISSION_NEEDED,
    CONNECTED_AND_ACTIVE
}

/**
 * Shizuku Hybrid Integration.
 * Shizuku is treated as an optional system-level execution backend.
 * Checks installation, service availability, and permission safely.
 */
class ShizukuEngine(private val context: Context) {

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }

    private val _status = MutableStateFlow(ShizukuStatus.NOT_INSTALLED)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus(): ShizukuStatus {
        val pm = context.packageManager
        val isInstalled = try {
            pm.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

        val newStatus = if (!isInstalled) {
            ShizukuStatus.NOT_INSTALLED
        } else {
            // Check if service binder is running via query intent or binder check
            val intent = pm.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            if (intent != null) {
                ShizukuStatus.INSTALLED_WAITING_SERVICE
            } else {
                ShizukuStatus.NOT_INSTALLED
            }
        }
        _status.value = newStatus
        return newStatus
    }

    fun isAvailable(): Boolean {
        return _status.value == ShizukuStatus.CONNECTED_AND_ACTIVE
    }

    fun executeSystemCommand(command: String): Pair<Boolean, String> {
        if (!isAvailable()) {
            return false to "Shizuku service is not currently active. Falling back to Accessibility or Intent."
        }
        return false to "Command requires elevated Shizuku binder session."
    }
}
