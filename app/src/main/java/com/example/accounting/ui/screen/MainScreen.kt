package com.example.accounting.ui.screen

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.accounting.util.PermissionHelper

@Composable
fun MainScreen(
    onPermissionsRevoked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    context as? ComponentActivity
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var hasOverlayPermission by remember {
        mutableStateOf(PermissionHelper.checkOverlayPermission(context))
    }
    var hasAccessibilityPermission by remember {
        mutableStateOf(PermissionHelper.checkAccessibilityPermission(context))
    }

    // Monitor permission status on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = PermissionHelper.checkOverlayPermission(context)
                hasAccessibilityPermission = PermissionHelper.checkAccessibilityPermission(context)

                // Navigate back to permission screen if permissions are revoked
                if (!hasOverlayPermission || !hasAccessibilityPermission) {
                    onPermissionsRevoked()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Back button behavior: Dev tab → Config tab, Config tab → Recent tab, Recent tab → exit app
    BackHandler(enabled = selectedTabIndex > 0) {
        selectedTabIndex--
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Recent"
                        )
                    },
                    label = { Text("Recent") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Config")
                    },
                    label = { Text("Config") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = {
                        Icon(imageVector = Icons.Default.Build, contentDescription = "Dev")
                    },
                    label = { Text("Dev") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTabIndex) {
            0 -> RecentScreen(
                modifier = Modifier.padding(innerPadding)
            )

            1 -> ConfigScreen(
                modifier = Modifier.padding(innerPadding)
            )

            2 -> DevScreen(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
