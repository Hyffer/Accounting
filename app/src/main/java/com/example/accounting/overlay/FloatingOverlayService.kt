package com.example.accounting.overlay

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.accounting.R
import com.example.accounting.service.ViewInspectionAccessibilityService

class FloatingOverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "view_inspector"
        const val NOTIFICATION_ID = 1

        private const val INSPECT_FROM_NOTIFICATION =
            "com.example.accounting.INSPECT_FROM_NOTIFICATION"
        private const val SHADE_DISMISS_DELAY_MS = 1_000L

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private var overlayManager: FloatingOverlayManager? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground()
        setupOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == INSPECT_FROM_NOTIFICATION) {
            inspectAfterDismissingShade()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        overlayManager?.hide()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForeground() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        val inspectIntent = Intent(this, FloatingOverlayService::class.java)
            .setAction(INSPECT_FROM_NOTIFICATION)
        val inspectPendingIntent = PendingIntent.getService(
            this,
            0,
            inspectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_content))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_search,
                getString(R.string.notification_action_capture),
                inspectPendingIntent
            )
            .build()
    }

    private fun setupOverlay() {
        overlayManager = FloatingOverlayManager(this) {
            sendInspectionBroadcast()
        }
        overlayManager?.show()
    }

    private fun inspectAfterDismissingShade() {
        dismissNotificationShade()
        handler.postDelayed({
            sendInspectionBroadcast()
        }, SHADE_DISMISS_DELAY_MS)
    }

    private fun dismissNotificationShade() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ViewInspectionAccessibilityService.instance?.performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE
            )
        } else {
            // for devices < Android 12
            @Suppress("DEPRECATION", "MissingPermission")
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        }
    }

    private fun sendInspectionBroadcast() {
        val intent = Intent(ViewInspectionAccessibilityService.INSPECT_ACTION)
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }
}
