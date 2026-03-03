# Implementation Plan: Backend Integration

This plan outlines the steps to integrate backend server functionality into the Auto Accounting app.

---

## Overview

Three main tasks:
1. Add a Config Tab for backend configuration (URL and TOKEN)
2. Create an API library for backend communication
3. Modify Recent Tab to sync transactions to backend via swipe-right gesture

---

## Task 1: Config Tab Implementation

### 1.1 Add Dependencies
Add DataStore preference dependency to `app/build.gradle.kts`:
```kotlin
implementation("androidx.datastore:datastore-preferences:1.0.0")
```

### 1.2 DataStore for Configuration Storage
Create `datastore/ConfigDataStore.kt`:
A DataStore-based repository for storing backend configuration as key-value pairs.

```kotlin
object ConfigDataStore {
    // Keys
    object Keys {
        val URL = stringPreferencesKey("backend_url")
        val TOKEN = stringPreferencesKey("auth_token")
    }

    // Flow-based getters
    fun getUrl(context: Context): Flow<String?>
    fun getToken(context: Context): Flow<String?>

    // Suspend setters
    suspend fun setUrl(context: Context, url: String)
    suspend fun setToken(context: Context, token: String)
}
```

### 1.3 Create ConfigScreen
Create `ui/screen/ConfigScreen.kt`:

```
+----------------------------------------+
|         Backend Configuration          |
|                                        |
|  Server URL                            |
|  [________________________]            |
|                                        |
|  Auth Token                            |
|  [________________________]            |
|                                        |
|                 [Save Configuration]   |
|                                        |
+----------------------------------------+
```

UI Components:
- Two `OutlinedTextField` for URL and TOKEN input
- A save button that persists to DataStore

### 1.4 Update MainScreen
Modify `MainScreen.kt`:
- Add new tab index: Recent(0) -> Config(1) -> Dev(2)
- Add ConfigScreen to the `when` block
- Add `NavigationBarItem` for Config tab with appropriate icon (e.g., `Icons.Filled.Build`)
- Update BackHandler logic: Dev -> Config -> Recent

---

## Task 2: API Library Implementation

### 2.1 Add Network Dependencies
Add OkHttp dependency to `app/build.gradle.kts`:
```kotlin
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

### 2.2 Create ApiException
Create `api/ApiException.kt`:
```kotlin
sealed class ApiException(message: String) : Exception(message) {
    class ConfigMissingException : ApiException("Backend URL or TOKEN not configured")
    class NetworkException(cause: Throwable) : ApiException("Network error: ${cause.message}")
    class ServerException(code: Int) : ApiException("Server returned error code: $code")
    class ParseException(cause: Throwable) : ApiException("Failed to parse response: ${cause.message}")
}
```

### 2.3 Create TransactionApi
Create `api/TransactionApi.kt`:

```kotlin
class TransactionApi(private val configDataStore: ConfigDataStore) {

    /**
     * Posts a transaction to the backend server.
     * @throws ApiException.ConfigMissingException if URL or TOKEN is not configured
     * @throws ApiException.NetworkException if network request fails
     * @throws ApiException.ServerException if server returns non-201 response
     */
    suspend fun postTransaction(transaction: TransactionInfo): Result<Unit>

    private fun formatTransactionForApi(transaction: TransactionInfo): TransactionRequest
}

// Request body structure
data class TransactionRequest(
    val amount: Double,
    val merchant: String?,
    val payment_method: String?,
    val occurred_at: String  // ISO 8601 format: "2024-03-01T10:15:30Z"
)
```

Key implementation details:
- Read URL and TOKEN from ConfigDataStore
- Throw `ConfigMissingException` if either is empty or null
- Build request URL: `${URL}/transaction_info`
- Add `Authorization: Bearer ${TOKEN}` header
- Convert `TransactionInfo.timestamp` (Long) to ISO 8601 format for `occurred_at`
- Parse `amount` (BigDecimal) to Double for JSON serialization
- Expect HTTP 201 response code, throw `ServerException` otherwise
- Wrap network errors in `NetworkException`

### 2.4 Create ApiClient (Optional Singleton)
Create `api/ApiClient.kt` for managing OkHttp client:
```kotlin
object ApiClient {
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun getClient(): OkHttpClient = okHttpClient
}
```

---

## Task 3: Recent Tab Swipe-to-Sync

### 3.1 Update TransactionViewModel
Add API functionality to `TransactionViewModel`:
```kotlin
class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    // Existing code...

    private val transactionApi: TransactionApi

    // For observing sync result
    private val _syncResult = MutableSharedFlow<SyncResult>()
    val syncResult: SharedFlow<SyncResult> = _syncResult.asSharedFlow()

    /**
     * Sync a transaction to backend.
     * On success: deletes from local database
     * On failure: emits error message via syncResult
     */
    suspend fun syncTransactionToBackend(transaction: TransactionInfo)
}

sealed class SyncResult {
    data class Success(val transactionId: Long) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
```

### 3.2 Modify TransactionItem Component
Update the swipe gesture handling in `RecentScreen.kt`:

Current behavior:
- Swipe right (StartToEnd): Reserved, returns `true` but does nothing

New behavior:
- Swipe right (StartToEnd): Triggers API sync
- On success: Item is removed from list (viewModel deletes from DB)
- On failure: Show toast, reset dismissState to `Settled`

Implementation approach:
1. Change `confirmValueChange` callback to handle `StartToEnd` differently
2. Use `rememberCoroutineScope` to launch async API call
3. Use `LaunchedEffect` or callback to handle the async result
4. Use `Toast` or `Snackbar` for error messages

```kotlin
@Composable
private fun TransactionItem(
    transaction: TransactionInfo,
    onDelete: () -> Unit,
    onSync: suspend () -> SyncResult,  // New callback
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // State to trigger reset
    var shouldReset by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Don't confirm immediately - trigger async operation
                    // We need to handle this differently
                    false  // Return false to not auto-dismiss
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        // ... existing thresholds
    )

    // Handle swipe-right via snapshotFlow or LaunchedEffect
    LaunchedEffect(dismissState) {
        snapshotFlow { dismissState.targetValue }
            .collect { target ->
                if (target == SwipeToDismissBoxValue.StartToEnd) {
                    // Trigger sync
                    val result = onSync()
                    when (result) {
                        is SyncResult.Success -> {
                            // Delete from DB (handled by ViewModel)
                        }
                        is SyncResult.Error -> {
                            // Reset state and show toast
                            dismissState.reset()
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }
}
```

---

## File Structure After Implementation

```
app/src/main/java/com/example/accounting/
|-- ...
|-- api/                              # NEW
|   |-- ApiException.kt               # NEW
|   |-- TransactionApi.kt             # NEW
|   |-- ApiClient.kt                  # NEW (optional)
|-- datastore/
|   |-- ConfigDataStore.kt           # NEW
|-- ui/
    |-- screen/
    |   |-- DevScreen.kt
    |   |-- MainScreen.kt             # MODIFIED
    |   |-- PermissionScreen.kt
    |   |-- RecentScreen.kt           # MODIFIED
    |   |-- ConfigScreen.kt           # NEW
    |-- viewmodel/
        |-- TransactionViewModel.kt   # MODIFIED
        |-- ConfigViewModel.kt        # NEW
```

---

## Implementation Order

1. **Config Tab** (Task 1)
   - Add DataStore dependency
   - Create ConfigDataStore
   - Create ConfigViewModel
   - Create ConfigScreen
   - Update MainScreen

2. **API Library** (Task 2)
   - Add OkHttp dependency
   - Create ApiException
   - Create TransactionApi

3. **Swipe-to-Sync** (Task 3)
   - Update TransactionViewModel
   - Modify RecentScreen TransactionItem

---

## Testing Considerations

- Unit test for ConfigDataStore (DataStore operations)
- Unit test for TransactionApi (mock OkHttp responses)
- UI test for ConfigScreen
- Manual test for swipe-to-sync behavior
- Test error scenarios:
  - Missing configuration
  - Network timeout
  - Server returning 4xx/5xx errors
  - Invalid token

---

## Dependencies to Add

```kotlin
// In app/build.gradle.kts

// DataStore for preferences
implementation("androidx.datastore:datastore-preferences:1.0.0")

// OkHttp for network requests
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

---

## Questions for Review

1. Should we add a "Test Connection" button in ConfigScreen?
2. Should synced transactions be marked differently before deletion, or deleted immediately on success?
3. Should we show a success toast/snackbar when sync completes?
4. Should we add retry logic for failed syncs?
5. Should we persist failed sync attempts for later retry?