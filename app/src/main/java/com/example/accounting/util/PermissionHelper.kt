package com.example.accounting.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.getSystemService

object PermissionHelper {
    fun checkOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun checkAccessibilityPermission(context: Context): Boolean {
        val accessibilityManager = context.getSystemService<AccessibilityManager>()
            ?: return false

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val serviceName = "${context.packageName}/${context.packageName}.service.ViewInspectionAccessibilityService"
        return enabledServices.contains(serviceName)
    }

    fun requestOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    }

    fun requestAccessibilityPermission(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(intent)
    }
}
