package com.example.accounting.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.accounting.R
import com.example.accounting.data.FileStorageManager
import com.example.accounting.database.AppDatabase
import com.example.accounting.handler.PaymentAppHandlerFactory
import com.example.accounting.model.InspectionResult
import com.example.accounting.model.PaymentViewType
import com.example.accounting.model.TransactionInfo
import com.example.accounting.model.isDuplicate
import com.example.accounting.model.isMatchingRequest
import com.example.accounting.notification.TransactionNotificationManager
import com.example.accounting.repository.TransactionRepository
import com.example.accounting.util.ViewTreeParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class ViewInspectionAccessibilityService : AccessibilityService() {

    companion object {
        const val INSPECT_ACTION = "com.example.accounting.INSPECT_ACTION"
        private const val DUPLICATE_WINDOW_MS = 60_000L // 1 minute

        @Volatile
        var instance: ViewInspectionAccessibilityService? = null
            private set
    }

    private val fileStorageManager by lazy { FileStorageManager(this) }
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val transactionRepository by lazy { TransactionRepository(database) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private val processMutex = Mutex()

    private val inspectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == INSPECT_ACTION) {
                performInspection()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        val filter = IntentFilter(INSPECT_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(inspectReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(inspectReceiver, filter)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        val handler = PaymentAppHandlerFactory.getHandler(packageName) ?: return

        val rootNode = rootInActiveWindow ?: return
        val viewNodeInfo = ViewTreeParser.parseNodeTree(rootNode) ?: return

        val viewType = handler.detectPaymentView(viewNodeInfo)
        if (viewType != PaymentViewType.None) {
            val transaction = handler.extractTransaction(viewNodeInfo, viewType)

            transaction?.let {
                scope.launch(Dispatchers.IO) {
                    processMutex.withLock {
                        processTransaction(it)
                    }
                }
            }
        }
    }

    /**
     * Process a newly detected transaction:
     * 1. Check for duplicates in database (transactions in the past minute)
     * 2. If Success view, check for matching Request to update
     * 3. If neither, save as new transaction and show notification
     */
    private suspend fun processTransaction(transaction: TransactionInfo) {
        val minTimestamp = transaction.timestamp - DUPLICATE_WINDOW_MS
        val recentTransactions = transactionRepository.getTransactionsAfter(minTimestamp)

        // Check for duplicate
        val duplicate = recentTransactions.find { existing ->
            transaction.isDuplicate(existing)
        }
        if (duplicate != null) {
            // Duplicate found, discard
            return
        }

        // If Success view, check for matching Request to update
        if (transaction.viewType == PaymentViewType.Success) {
            val matchingRequest = recentTransactions.find { existing ->
                transaction.isMatchingRequest(existing)
            }
            if (matchingRequest != null) {
                // Update existing Request with Success info
                val updated = matchingRequest.copy(
                    viewType = PaymentViewType.Success,
                    timestamp = transaction.timestamp
                )
                transactionRepository.updateTransaction(updated)
                TransactionNotificationManager.showNotification(this, updated)
                return
            }
        }

        // New transaction - save and notify
        val id = transactionRepository.insertTransaction(transaction)
        val inserted = transaction.copy(id = id)
        TransactionNotificationManager.showNotification(this, inserted)
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        try {
            unregisterReceiver(inspectReceiver)
        } catch (_: Exception) {
            // Receiver might not be registered
        }
    }

    private fun performInspection() {
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Toast.makeText(this, "No active window found", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if root node has valid bounds
        val bounds = android.graphics.Rect()
        rootNode.getBoundsInScreen(bounds)
        if (bounds.isEmpty) {
            Toast.makeText(
                this,
                "Empty view hierarchy - app may restrict accessibility",
                Toast.LENGTH_LONG
            ).show()
        }

        scope.launch(Dispatchers.IO) {
            try {
                val viewNodeInfo = ViewTreeParser.parseNodeTree(rootNode)
                if (viewNodeInfo == null) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            this@ViewInspectionAccessibilityService,
                            "Failed to parse view tree",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val packageName = rootNode.packageName?.toString() ?: "unknown"
                val activityName = rootNode.viewIdResourceName

                val result = InspectionResult(
                    timestamp = System.currentTimeMillis(),
                    packageName = packageName,
                    activityName = activityName,
                    rootNode = viewNodeInfo,
                    totalViewCount = viewNodeInfo.size
                )

                fileStorageManager.saveInspection(result)

                launch(Dispatchers.Main) {
                    val message = if (result.totalViewCount <= 1) {
                        "Warning: Only ${result.totalViewCount} view found. App may restrict accessibility."
                    } else {
                        getString(R.string.inspection_saved)
                    }
                    Toast.makeText(
                        this@ViewInspectionAccessibilityService,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@ViewInspectionAccessibilityService,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
