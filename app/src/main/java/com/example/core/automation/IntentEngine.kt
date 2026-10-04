package com.example.core.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class IntentEngine(private val context: Context) {

    fun launchAppByNameOrPackage(identifier: String): Boolean {
        val pm = context.packageManager
        // 1. Try exact package name
        var launchIntent = pm.getLaunchIntentForPackage(identifier)

        // 2. If not found, search installed packages by label
        if (launchIntent == null) {
            val installedApps = pm.getInstalledApplications(0)
            val matchedApp = installedApps.firstOrNull { appInfo ->
                val label = pm.getApplicationLabel(appInfo).toString()
                label.equals(identifier, ignoreCase = true) || label.contains(identifier, ignoreCase = true)
            }
            if (matchedApp != null) {
                launchIntent = pm.getLaunchIntentForPackage(matchedApp.packageName)
            }
        }

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            false
        }
    }

    fun openUrl(url: String): Boolean {
        return try {
            val cleanUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openSystemSettings(settingType: String): Boolean {
        val action = when (settingType.lowercase()) {
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        return try {
            val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
