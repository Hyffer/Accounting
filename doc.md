# Auto Accounting - Documentation

## Project Overview

**This is an Android application that automatically keeps accounts when the user pays bills.**

The application uses **Accessibility Services** to detect payment views on screen, extract transaction information, and record it automatically. This eliminates the need for manual bookkeeping when making payments through apps like WeChat Pay, Alipay, etc.

```
+-------------------------------------------------------------------------+
| SelectToSpeakService (disguised accessibility service)                  |
|   |- ViewInspectionAccessibilityService (base)                          |
|       |- onAccessibilityEvent() filters relevant events                 |
|           |- Uses PaymentAppHandlerFactory to route by package name     |
|           |- Calls handler.detectPaymentView() and extractTransaction() |
|           |- Serialized calls to processTransaction()                   |
|               |- Deduplication, matching and database updating          |
|               |- Shows notification via TransactionNotificationManager  |
+-------------------------------------------------------------------------+
```

For development purpose, the app can also capture and save view hierarchy for later inspection and analysis. This helps identify the structure of payment views in target apps.

```
+--------------------------------------------------------------+
|                        MainActivity                          |
|  - Permission management (Overlay + Accessibility)           |
|  - Start/Stop overlay service                                |
+--------------------------------------------------------------+
                              |
                              v
+--------------------------------------------------------------+
|                   FloatingOverlayService                     |
|  - Foreground service with notification                      |
|  - Manages floating button window                            |
|  - Sends broadcast on button click                           |
+--------------------------------------------------------------+
                              |
                              | broadcast: INSPECT_ACTION
                              v
+--------------------------------------------------------------+
|           SelectToSpeakService (disguised accessibility)     |
|           |- ViewInspectionAccessibilityService (base)       |
|  - Receives broadcast                                        |
|  - Captures rootInActiveWindow                               |
|  - Parses view tree with ViewTreeParser                      |
|  - Saves to file via FileStorageManager                      |
+--------------------------------------------------------------+
```

---

## File Structure

```
app/src/main/java/com/example/accounting/
|-- MainActivity.kt
|-- model/
|   |-- ViewNodeInfo.kt
|   |-- InspectionResult.kt
|   |-- TransactionInfo.kt
|-- service/
|   |-- ViewInspectionAccessibilityService.kt
|-- handler/
|   |-- PaymentAppHandler.kt
|   |-- WeChatPayHandler.kt
|   |-- AlipayHandler.kt
|   |-- PaymentAppHandlerFactory.kt
|   |-- PatternMatcher.kt
|   |-- ViewNodeUtils.kt
|-- notification/
|   |-- TransactionNotificationManager.kt
|-- overlay/
|   |-- FloatingOverlayService.kt
|   |-- FloatingOverlayManager.kt
|-- data/
|   |-- FileStorageManager.kt
|-- database/
|   |-- AppDatabase.kt
|   |-- dao/
|   |   |-- TransactionDao.kt
|   |-- converters/
|       |-- TransactionConverters.kt
|-- repository/
|   |-- TransactionRepository.kt
|-- util/
|   |-- PermissionHelper.kt
|   |-- ViewTreeParser.kt
|-- ui/
    |-- screen/
        |-- DevScreen.kt
        |-- MainScreen.kt
        |-- PermissionScreen.kt
        |-- RecentScreen.kt

app/src/main/java/com/google/android/accessibility/selecttospeak/
|-- SelectToSpeakService.kt

app/src/main/res/xml/
|-- accessibility_service_config.xml
```

### Core Model Classes

#### `model/ViewNodeInfo.kt`
Data class representing a single view node in the hierarchy. Used for simplifying AccessibilityNodeInfo tree.

#### `model/InspectionResult.kt`
Represents a captured screen of view. Used for wrapping view tree with metadata for analysis.

#### `model/TransactionInfo.kt`
**TransactionInfo:** Data model for extracted transaction information. Contains transaction details like amount and timestamp. Also used as Room entity for persistent storage with auto-generated ID.

**PaymentViewType:** Indicates what type of view (a payment request or a success page) is detected.

---

### Handler Classes

#### `handler/PaymentAppHandler.kt`
Interface defining the contract for payment app handlers.

```kotlin
interface PaymentAppHandler {
    val packageName: String
    fun detectPaymentView(rootNode: ViewNodeInfo): PaymentViewType
    fun extractTransaction(rootNode: ViewNodeInfo, viewType: PaymentViewType): TransactionInfo?
}
```

**Use Case:** Provides a clean abstraction for payment detection and extraction. Each payment app implements this interface.

#### `handler/PaymentAppHandlerFactory.kt`
Factory object for routing to appropriate payment handler.

```kotlin
object PaymentAppHandlerFactory {
    fun getHandler(packageName: String): PaymentAppHandler?
}
```

**Use Case:** Routes accessibility events to the correct handler based on package name. Returns null for unsupported apps.

#### `handler/PatternMatcher.kt`
Utility object for pattern matching in text. Used by handlers to detect amounts and keywords in view text.

```kotlin
object PatternMatcher {
    // amount pattern: `[￥¥$]([\d,]+\.?\d*)`
    fun containsAmount(text: String): Boolean
    fun extractAmount(text: String): BigDecimal?
    fun containsKeywords(text: String, keywords: List<String>): Boolean
}
```

#### `handler/ViewNodeUtils.kt`
Utility object for common view node operations shared across payment handlers.

```kotlin
object ViewNodeUtils {
    fun collectAllText(node: ViewNodeInfo): List<String>
    fun collectTextNodesWithContentDesc(node: ViewNodeInfo): List<ViewNodeInfo>
    fun collectButtonNodes(node: ViewNodeInfo): List<ViewNodeInfo>
    fun hasButtonWithText(node: ViewNodeInfo, searchText: String): Boolean
    fun hasText(node: ViewNodeInfo, searchText: String): Boolean
}
```

---

### Utility Classes

#### `util/PermissionHelper.kt`
Checks and requests required permissions.

**Functions:**
- `checkOverlayPermission(context)`: Returns true if `SYSTEM_ALERT_WINDOW` permission granted
- `checkAccessibilityPermission(context)`: Returns true if the disguised `SelectToSpeakService` is enabled
- `requestOverlayPermission(context)`: Opens Settings > Overlay permission screen
- `requestAccessibilityPermission(context)`: Opens Accessibility Settings

**Implementation Note:** The accessibility check specifically looks for the disguised service name:
```kotlin
val serviceName = "${context.packageName}/com.google.android.accessibility.selecttospeak.SelectToSpeakService"
```

#### `util/ViewTreeParser.kt`
Recursively parses AccessibilityNodeInfo into ViewNodeInfo, calculating height and size properties for each node

---

### Data Layer

#### SQLite Database
The application uses Room database for persistent storage of transaction information.

**Components:**
- **AppDatabase**: Main database class with singleton pattern
- **TransactionDao**: Data Access Object for CRUD operations on transactions
- **TransactionConverters**: Handles type conversion of TransactionInfo properties (BigDecimal and PaymentViewType)
- **TransactionRepository**: Repository pattern implementation for data access

#### `data/FileStorageManager.kt`
Manages JSON file I/O for inspection results.

**Storage Location:** `/data/data/com.example.accounting/files/inspections/inspection_<timestamp>.json`

**Functions:**
- `saveInspection(result)`: Saves InspectionResult to timestamped JSON file
- `getInspectionFiles()`: Returns list of all inspection files, newest first
- `deleteInspection(file)`: Deletes a specific inspection file

---

### Accessibility Service

#### `service/ViewInspectionAccessibilityService.kt`
Base accessibility service that handles view inspection and payment detection.

**Key Components:**

1. **BroadcastReceiver** - Listens for `INSPECT_ACTION` broadcast
2. **performInspection()** - Captures and saves view tree
3. **onAccessibilityEvent()** - Real-time payment detection and extraction
4. **processTransaction()** - transaction deduplication and matching

**Payment Detection Flow:**
1. Receives `WINDOW_CONTENT_CHANGED` or `WINDOW_STATE_CHANGED` events
2. Checks if package name is supported via `PaymentAppHandlerFactory`
3. Parses view tree using `ViewTreeParser`
4. Calls `handler.detectPaymentView()` to check if it's a payment view
5. If payment view detected, calls `handler.extractTransaction()`
6. Searches database for recent transactions to prevent duplicates
7. Saves transaction to database
8. Shows notification via `TransactionNotificationManager`

**Important:** The class is marked `open` so it can be extended by the disguised service.

---

#### `com.google.android.accessibility.selecttospeak/SelectToSpeakService.kt`
Disguised accessibility service that extends the base service.

```kotlin
class SelectToSpeakService: ViewInspectionAccessibilityService() {
    // Inherits all functionality from base class
    // Empty wrapper for package name disguise
}
```

**Purpose:** Some apps (like WeChat) restrict accessibility services from unknown packages. By disguising as Google's "Select to Speak" service, we can bypass these restrictions.

**Important:** Do not add code to this class. It is only a wrapper for the package name disguise. All logic should be in `ViewInspectionAccessibilityService`.

---

### Overlay Service

#### `overlay/FloatingOverlayService.kt`
Foreground service that hosts the floating button.

**Responsibilities:**
1. Creates notification channel and foreground notification
2. Instantiates `FloatingOverlayManager`
3. Sends inspection broadcast when button is clicked

#### `overlay/FloatingOverlayManager.kt`
Creates and manages the floating button window.

**Touch Event Handling:**
```
ACTION_DOWN -> record initial position
ACTION_MOVE -> update window position
ACTION_UP -> check if moved < 10px, if so trigger click
```

---

### UI

There is a dedicated file "doc_ui.md" for UI related documentation.
