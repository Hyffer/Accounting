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
import com.example.accounting.navigation.Routes
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
        Routes.MAIN
    } else {
        Routes.PERMISSION_REQUEST
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.PERMISSION_REQUEST) {
            PermissionScreen(
                onPermissionsGranted = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.PERMISSION_REQUEST) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MAIN) {
            MainScreen(
                onPermissionsRevoked = {
                    navController.navigate(Routes.PERMISSION_REQUEST) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            )
        }
    }
}
