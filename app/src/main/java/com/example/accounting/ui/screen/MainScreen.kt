package com.example.accounting.ui.screen

import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.accounting.navigation.Routes
import com.example.accounting.util.PermissionHelper

@Composable
fun MainScreen(
    onPermissionsRevoked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    context as? ComponentActivity
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val navController = rememberNavController()

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            MainBottomNavigation(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Main.RECENT,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Main.RECENT) {
                RecentScreen()
            }
            composable(Routes.Main.CONFIG) {
                ConfigScreen()
            }
            composable(Routes.Main.DEV) {
                DevScreen()
            }
        }
    }
}

@Composable
private fun MainBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Routes.Main.RECENT,
            onClick = {
                if (currentRoute != Routes.Main.RECENT) {
                    navController.navigate(Routes.Main.RECENT) {
                        popUpTo(Routes.Main.RECENT) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Recent"
                )
            },
            label = { Text("Recent") }
        )
        NavigationBarItem(
            selected = currentRoute == Routes.Main.CONFIG,
            onClick = {
                if (currentRoute != Routes.Main.CONFIG) {
                    navController.navigate(Routes.Main.CONFIG) {
                        popUpTo(Routes.Main.RECENT) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            },
            icon = {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Config")
            },
            label = { Text("Config") }
        )
        NavigationBarItem(
            selected = currentRoute == Routes.Main.DEV,
            onClick = {
                if (currentRoute != Routes.Main.DEV) {
                    navController.navigate(Routes.Main.DEV) {
                        popUpTo(Routes.Main.RECENT) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            },
            icon = {
                Icon(imageVector = Icons.Default.Build, contentDescription = "Dev")
            },
            label = { Text("Dev") }
        )
    }
}
