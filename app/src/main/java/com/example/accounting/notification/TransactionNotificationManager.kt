package com.example.accounting.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.example.accounting.model.PaymentViewType
import com.example.accounting.model.TransactionInfo

object TransactionNotificationManager {
    private const val CHANNEL_ID = "transaction_detection"
    private const val CHANNEL_NAME = "Transaction Detection"
    private const val NOTIFICATION_ID_BASE = 1000

    /**
     * Shows notification about detected transaction
     * @param context
     * @param transaction with database generated id field
     */
    fun showNotification(context: Context, transaction: TransactionInfo) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager)

        val title = when (transaction.viewType) {
            is PaymentViewType.Request -> "检测到支付请求"
            is PaymentViewType.Success -> "支付成功"
            else -> return
        }

        val content = buildNotificationContent(transaction)
        val pendingIntent = createEditPendingIntent(context, transaction.id)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = NOTIFICATION_ID_BASE + transaction.id.toInt()
        notificationManager.notify(notificationId, notification)
    }

    private fun buildNotificationContent(transaction: TransactionInfo): String {
        val lines = mutableListOf<String>()

        transaction.merchant?.let { lines.add("商户: $it") }
        lines.add("金额: ¥${transaction.amount}")
        transaction.paymentMethod?.let { lines.add("付款方式: $it") }

        if (transaction.viewType == PaymentViewType.Request) {
            lines.add("状态: 等待确认")
        }

        return lines.joinToString("\n")
    }

    /**
     * Creates a PendingIntent that opens the Edit Screen for the given transaction.
     * Uses deep link: app://com.example.accounting/recent/{transactionId}
     */
    private fun createEditPendingIntent(context: Context, transactionId: Long): PendingIntent {
        val deepLinkUri = "app://com.example.accounting/recent/$transactionId".toUri()
        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, transactionId.toInt(), intent, flags)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }
}
