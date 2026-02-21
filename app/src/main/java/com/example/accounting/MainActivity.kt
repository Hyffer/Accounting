package com.example.accounting

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.accounting.overlay.FloatingOverlayService
import com.example.accounting.ui.theme.AccountingTheme
import com.example.accounting.util.PermissionHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AccountingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    context as? ComponentActivity
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var isServiceRunning by remember { mutableStateOf(FloatingOverlayService.isRunning) }
    var hasOverlayPermission by remember {
        mutableStateOf(
            PermissionHelper.checkOverlayPermission(
                context
            )
        )
    }
    var hasAccessibilityPermission by remember {
        mutableStateOf(
            PermissionHelper.checkAccessibilityPermission(
                context
            )
        )
    }
    val allPermissionsGranted = hasOverlayPermission && hasAccessibilityPermission

    // Refresh permission and service status when the activity resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = PermissionHelper.checkOverlayPermission(context)
                hasAccessibilityPermission = PermissionHelper.checkAccessibilityPermission(context)
                isServiceRunning = FloatingOverlayService.isRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "View Inspector",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Permission Status Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Permission Status",
                    style = MaterialTheme.typography.titleMedium
                )

                PermissionStatusRow(
                    label = "Overlay Permission",
                    isGranted = hasOverlayPermission
                )

                PermissionStatusRow(
                    label = "Accessibility Permission",
                    isGranted = hasAccessibilityPermission
                )
            }
        }

        // Permission Request Buttons
        if (!hasOverlayPermission) {
            Button(
                onClick = {
                    PermissionHelper.requestOverlayPermission(context)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Overlay Permission")
            }
        }

        if (!hasAccessibilityPermission) {
            Button(
                onClick = {
                    PermissionHelper.requestAccessibilityPermission(context)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Accessibility Permission")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Service Control Section
        if (allPermissionsGranted) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isServiceRunning) "Service is running" else "Service is stopped",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(context, FloatingOverlayService::class.java)
                                ContextCompat.startForegroundService(context, intent)
                                isServiceRunning = true
                            },
                            enabled = !isServiceRunning
                        ) {
                            Text("Start Inspection")
                        }

                        Button(
                            onClick = {
                                val intent = Intent(context, FloatingOverlayService::class.java)
                                context.stopService(intent)
                                isServiceRunning = false
                            },
                            enabled = isServiceRunning
                        ) {
                            Text("Stop Inspection")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Instructions",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "1. Tap 'Start Inspection' to show the floating button\n" +
                                "2. Navigate to any app you want to inspect\n" +
                                "3. Tap the floating 'V' button to capture the view hierarchy\n" +
                                "4. Toast will confirm the inspection is saved\n" +
                                "5. Files are saved to: /data/data/com.example.accounting/files/inspections/",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Please grant all required permissions to continue",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionStatusRow(
    label: String,
    isGranted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = if (isGranted) "Granted" else "Not Granted",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isGranted)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error
        )
    }
}
