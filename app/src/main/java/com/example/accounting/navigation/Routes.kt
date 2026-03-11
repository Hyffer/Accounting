package com.example.accounting.navigation

object Routes {
    // App-level routes
    const val PERMISSION_REQUEST = "permissionRequest"
    const val MAIN = "main"

    // Main screen tabs
    object Main {
        const val RECENT = "recent"
        const val CONFIG = "config"
        const val DEV = "dev"

        // Nested routes for recent tab
        object Recent {
            const val LIST = "recent/list"
            const val EDIT = "recent/{transactionId}"

            fun editRoute(id: Long) = "recent/$id"
        }
    }
}