package com.example.accounting.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import com.example.accounting.R
import com.example.accounting.data.FileStorageManager
import com.example.accounting.model.InspectionResult
import com.example.accounting.util.ViewTreeParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class ViewInspectionAccessibilityService : AccessibilityService() {

    companion object {
        const val INSPECT_ACTION = "com.example.accounting.INSPECT_ACTION"
    }

    private val fileStorageManager by lazy { FileStorageManager(this) }
    private val scope = CoroutineScope(Dispatchers.Main)

    private val inspectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == INSPECT_ACTION) {
                performInspection()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        val filter = IntentFilter(INSPECT_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(inspectReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(inspectReceiver, filter)
        }
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // We don't need to handle events for this service
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(inspectReceiver)
        } catch (e: Exception) {
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
                    totalViewCount = ViewTreeParser.countViews(viewNodeInfo)
                )

                val file = fileStorageManager.saveInspection(result)

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
