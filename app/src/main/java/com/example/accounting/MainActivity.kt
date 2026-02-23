package com.example.accounting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.accounting.ui.screen.MainScreen
import com.example.accounting.ui.screen.PermissionScreen
import com.example.accounting.ui.theme.AccountingTheme
import com.example.accounting.util.PermissionHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AccountingTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()

    val hasOverlayPermission = PermissionHelper.checkOverlayPermission(context)
    val hasAccessibilityPermission = PermissionHelper.checkAccessibilityPermission(context)
    val startDestination = if (hasOverlayPermission && hasAccessibilityPermission) {
        "main"
    } else {
        "permissionRequest"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("permissionRequest") {
            PermissionScreen(
                onPermissionsGranted = {
                    navController.navigate("main") {
                        popUpTo("permissionRequest") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScreen(
                onPermissionsRevoked = {
                    navController.navigate("permissionRequest") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}
