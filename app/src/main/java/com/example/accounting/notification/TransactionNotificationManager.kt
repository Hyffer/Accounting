package com.example.accounting.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.accounting.model.PaymentViewType
import com.example.accounting.model.TransactionInfo

object TransactionNotificationManager {
    private const val CHANNEL_ID = "transaction_detection"
    private const val CHANNEL_NAME = "Transaction Detection"
    private const val NOTIFICATION_ID_BASE = 1000

    fun showNotification(context: Context, transaction: TransactionInfo) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager)

        val title = when (transaction.viewType) {
            is PaymentViewType.Request -> "检测到支付请求"
            is PaymentViewType.Success -> "支付成功"
            else -> return
        }

        val content = buildNotificationContent(transaction)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationId = NOTIFICATION_ID_BASE + transaction.timestamp.toInt()
        notificationManager.notify(notificationId, notification)
    }

    private fun buildNotificationContent(transaction: TransactionInfo): String {
        val lines = mutableListOf<String>()

        transaction.merchant?.let { lines.add("商户: $it") }
        lines.add("金额: ${transaction.amount}")
        transaction.paymentMethod?.let { lines.add("付款方式: $it") }

        if (transaction.viewType == PaymentViewType.Request) {
            lines.add("状态: 等待确认")
        }

        return lines.joinToString("\n")
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}
