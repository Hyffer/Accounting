package com.example.accounting.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.accounting.R
import com.example.accounting.service.ViewInspectionAccessibilityService

class FloatingOverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "view_inspector"
        const val NOTIFICATION_ID = 1
    }

    private var overlayManager: FloatingOverlayManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground()
        setupOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        overlayManager?.hide()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    }

    private fun startForeground() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_content))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun setupOverlay() {
        overlayManager = FloatingOverlayManager(this) {
            sendInspectionBroadcast()
        }
        overlayManager?.show()
    }

    private fun sendInspectionBroadcast() {
        val intent = Intent(ViewInspectionAccessibilityService.INSPECT_ACTION)
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }
}
