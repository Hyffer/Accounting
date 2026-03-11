# Navigation Refactoring Plan

## Overview

Refactor the current UI state-based navigation to use Jetpack Navigation with NavHost, enabling direct navigation to any screen via URI deep links.

## Current Architecture

### Main Screen (`MainScreen.kt`)
- Uses `mutableIntStateOf(0)` for tab selection
- Content switches via `when (selectedTabIndex)` block
- Tabs: Recent (0), Config (1), Dev (2)

### Recent Tab (`RecentScreen.kt`)
- Edit view shown as overlay via `AnimatedVisibility`
- Visibility controlled by `focusedTransaction` state from `TransactionViewModel`
- Not a separate navigation destination

### App-level (`MainActivity.kt`)
- NavHost handles: `permissionRequest` → `main`
- No nested navigation for tabs or edit view

---

## Proposed Architecture

### URI Structure

```
app://com.example.accounting/main         → Main screen (default: Recent tab)
app://com.example.accounting/recent       → Recent tab
app://com.example.accounting/config       → Config tab
app://com.example.accounting/dev          → Dev tab
app://com.example.accounting/recent/{id}  → Edit transaction with specific ID
```

### Navigation Hierarchy

```
MainActivity (NavHost)
└── main (NavHost - MainScreen)
    ├── recent (NavHost - RecentScreen)
    │   ├── recent/list       (transaction list)
    │   └── recent/{id}       (edit transaction)
    ├── config
    └── dev
```

---

## Implementation Steps

### Step 1: Define Route Constants

Create a new file `navigation/Routes.kt`:

```kotlin
object Routes {
    // App-level routes (existing)
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
            const val editRoute(id: Long) = "recent/$id"
        }
    }
}
```

### Step 2: Update Main Screen Navigation

**File: `MainScreen.kt`**

Replace:
```kotlin
var selectedTabIndex by remember { mutableIntStateOf(0) }
```

With:
```kotlin
val navController = rememberNavController()

NavHost(
    navController = navController,
    startDestination = Routes.Main.RECENT,
    modifier = modifier
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
```

**Bottom Navigation:**
- Use `navController.currentBackStackEntry` to determine selected tab
- Use `navController.navigate()` for tab switches
- Configure `popUpTo` to maintain single tab instance

### Step 3: Add Nested NavHost to Recent Screen

**File: `RecentScreen.kt`**

Replace overlay pattern with nested NavHost:

```kotlin
val navController = rememberNavController()

NavHost(
    navController = navController,
    startDestination = Routes.Main.Recent.LIST
) {
    composable(Routes.Main.Recent.LIST) {
        // Transaction list content
        TransactionListContent(
            onTransactionClick = { transaction ->
                navController.navigate(Routes.Main.Recent.editRoute(transaction.id))
            }
        )
    }
    composable(
        route = Routes.Main.Recent.EDIT,
        deepLinks = listOf(
            navDeepLink { uriPattern = "app://com.example.accounting/recent/{transactionId}" }
        ),
        arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
    ) {
        val transactionId = it.arguments?.getLong("transactionId")
        // Load transaction and show EditTransactionScreen
        EditTransactionScreen(
            onDismiss = { navController.popBackStack() },
            // ... other callbacks
        )
    }
}
```

### Step 4: Modify TransactionViewModel

**File: `TransactionViewModel.kt`**

Remove or deprecate:
- `_focusedTransaction` / `focusedTransaction` StateFlow
- `focusTransaction()` method
- `defocusTransaction()` method

Keep (still needed):
- `transactions` StateFlow
- `updateTransaction()` method
- `deleteTransaction()` method
- `uploadThenDeleteTransaction()` method

Add if needed:
- `getTransactionById(id: Long): TransactionInfo?` for loading transaction by ID

### Step 5: Handle Tab Selection from Navigation

**MainScreen.kt**

Synchronize bottom navigation with NavHost state:

```kotlin
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
        // ... icon and label
    )
    // Similar for CONFIG and DEV tabs
}
```

### Step 6: Remove Edit Overlay Logic

**RecentScreen.kt**

Remove:
- `focusedTransaction` collection
- `AnimatedVisibility` wrapper around `EditTransactionScreen`

Replace with:
- Navigation-based approach using NavHost
- `popBackStack()` for dismiss action

---

## Files to Modify

| File | Changes |
|------|---------|
| `navigation/Routes.kt` | **New** - Define route constants |
| `MainScreen.kt` | Add NavHost for tab navigation, sync bottom nav state |
| `RecentScreen.kt` | Add nested NavHost for edit view, remove overlay pattern |
| `TransactionViewModel.kt` | Remove focus/defocus logic, keep data operations |
| `MainActivity.kt` | Add deep link handling for main route |
| `AndroidManifest.xml` | Add deep link intent filter (if needed) |

---

## Migration Strategy

### Phase 1: Add Navigation Infrastructure
1. Create `Routes.kt` with route constants
2. Update `MainScreen.kt` to use NavHost for tabs
3. Test tab navigation works correctly

### Phase 2: Add Nested Navigation in Recent Tab
1. Add NavHost to `RecentScreen.kt`
2. Move list content to `TransactionListContent`
3. Configure edit route with transaction ID argument
4. Update `EditTransactionScreen` to work without view model focus state

### Phase 3: Deep Link Integration
1. Add deep link patterns to navigation graph
2. Update `AndroidManifest.xml` if needed
3. Test deep links navigate correctly

### Phase 4: Cleanup
1. Remove `focusedTransaction` from `TransactionViewModel`
2. Remove unused overlay code
3. Update tests if applicable

---

## Considerations

### State Preservation
- Navigation state is saved in NavController's back stack
- ViewModel survives configuration changes
- Transaction list scroll position: use `LazyListState` with `rememberSaveable`

### Back Navigation
- Edit screen: `popBackStack()` returns to list
- Tab switches: `popUpTo` maintains single instance per tab
- App-level: back from Main exits app (or goes to permission if applicable)

### Animation
- Tab switches: use default NavHost transitions or disable
- Edit screen: preserve existing slide-up animation via `enterTransition`/`exitTransition`

---