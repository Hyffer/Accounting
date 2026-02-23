package com.example.accounting.ui.screen

import androidx.activity.ComponentActivity
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.accounting.util.PermissionHelper

@Composable
fun PermissionRequestScreen(
    onPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        PermissionRequestScreenContent(
            onPermissionsGranted = onPermissionsGranted,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun PermissionRequestScreenContent(
    onPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    context as? ComponentActivity
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var hasOverlayPermission by remember {
        mutableStateOf(PermissionHelper.checkOverlayPermission(context))
    }
    var hasAccessibilityPermission by remember {
        mutableStateOf(PermissionHelper.checkAccessibilityPermission(context))
    }

    // Refresh permission status when the screen resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = PermissionHelper.checkOverlayPermission(context)
                hasAccessibilityPermission = PermissionHelper.checkAccessibilityPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-navigate when both permissions are granted
    if (hasOverlayPermission && hasAccessibilityPermission) {
        onPermissionsGranted()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Permission Request",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Permission Status Card
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

        // Instructions Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Please grant all required permissions to continue",
                    style = MaterialTheme.typography.bodyLarge
                )
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
