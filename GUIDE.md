# Auto Accounting - Project Guide

## Overall Goal

**Build an Android application that automatically keeps accounts when the user pays bills.**

The application uses **Accessibility Services** to detect payment views on screen, extract transaction information, and record it automatically. This eliminates the need for manual bookkeeping when making payments through apps like WeChat Pay, Alipay, etc.

## Project Phases

### Phase 1: View Inspector (Completed)
Build a floating button that inspects the current view hierarchy and saves it for later analysis. This helps identify the structure of payment views in target apps.

### Phase 2: Payment Detection (Completed)
Detect when a payment view is displayed on screen by matching package names and view patterns discovered in Phase 1. Currently supports WeChat Pay and Alipay with a factory pattern for extensibility.

### Phase 3: Transaction Extraction (Completed)
Extract transaction details (amount, merchant, time, etc.) from detected payment views using accessibility APIs. Shows notifications for testing instead of persisting to storage.

### Phase 4: Automatic Recording (Planned)
Automatically save extracted transaction information to local storage or sync to cloud services.

---

## Phase 1: View Inspector - Implementation Details

### Overview

A floating button overlay that captures view hierarchies from any app using Accessibility Services. When clicked, it saves the complete view tree structure to a JSON file for analysis.

### Key Features

- Floating button always visible on top of other apps
- Captures complete view hierarchy from any application
- Saves inspection results as JSON files
- Button can be dragged to reposition
- Service disguised as Google's "Select to Speak" to bypass app accessibility restrictions

### Architecture

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

### Data Flow

```
User taps floating button
  -> ACTION_UP detected (not a drag)
  -> FloatingOverlayManager calls onInspectionClick callback
  -> FloatingOverlayService sends broadcast: "com.example.accounting.INSPECT_ACTION"
  -> SelectToSpeakService receives broadcast
  -> performInspection() called
  -> rootInActiveWindow captured
  -> ViewTreeParser.parseNodeTree() recursively builds ViewNodeInfo
  -> InspectionResult created with metadata
  -> FileStorageManager saves to JSON file
  -> Toast notification shown
```

### JSON Output Format

```json
{
  "timestamp": 1707891234567,
  "packageName": "com.tencent.mm",
  "activityName": "com.tencent.mm.plugin.wallet.pay.ui.WalletPayUI",
  "totalViewCount": 127,
  "rootNode": {
    "className": "android.widget.FrameLayout",
    "text": null,
    "contentDescription": null,
    "viewIdResourceName": "android:id/content",
    "boundsInScreen": [0, 0, 1080, 2400],
    "isClickable": false,
    "isEnabled": true,
    "isFocusable": false,
    "isScrollable": false,
    "childCount": 1,
    "depth": 0,
    "children": [...]
  }
}
```

**Analysis Tips:**
- `packageName` identifies the target app
- Look for `text` fields containing amounts, merchant names
- `viewIdResourceName` helps identify specific UI elements
- `boundsInScreen` shows element positions

---

## Phase 2 & 3: Payment Detection and Extraction - Implementation Details

### Overview

Real-time detection and extraction of payment information from WeChat Pay and Alipay using accessibility events. Uses a factory pattern to cleanly separate detection and extraction logic for each payment app.

### Supported Apps

- **WeChat Pay** (`com.tencent.mm`)
  - Payment Request: Keywords "付款方式" + "支付" + amount
  - Payment Success: Keywords "支付成功" + "完成" + amount
  - Amount extraction: Largest text node containing currency pattern

- **Alipay** (`com.eg.android.AlipayGphone`)
  - Payment Request: Keywords "付款给" + "确认付款"
  - Payment Success: Keywords "支付成功" + "完成"
  - Amount extraction: contentDescription regex `付款金额([\d.]+)元` for request, largest amount text for success

### Architecture

```
+--------------------------------------------------------------+
|           SelectToSpeakService (disguised accessibility)     |
|           |- ViewInspectionAccessibilityService (base)       |
|  - onAccessibilityEvent() filters relevant events            |
|  - Uses PaymentAppHandlerFactory to route by package name    |
|  - Calls handler.detectPaymentView() and extractTransaction()|
|  - Shows notification via TransactionNotificationManager     |
+--------------------------------------------------------------+
                              |
                              v
+--------------------------------------------------------------+
|                  PaymentAppHandlerFactory                    |
|  - Routes to appropriate handler based on package name       |
|  - Returns null if package not supported                     |
+--------------------------------------------------------------+
                              |
                              v
+--------------------------------------------------------------+
|              PaymentAppHandler (interface)                   |
|  - detectPaymentView(rootNode): PaymentViewType              |
|  - extractTransaction(rootNode, viewType): TransactionInfo?  |
+--------------------------------------------------------------+
            |                                  |
            v                                  v
+---------------------------+    +---------------------------+
|    WeChatPayHandler       |    |      AlipayHandler        |
| - Detection patterns      |    | - Detection patterns      |
| - Amount extraction       |    | - Amount extraction       |
+---------------------------+    +---------------------------+
```

### Detection Patterns

The app uses fuzzy keyword matching instead of relying on fragile resource IDs or exact view hierarchy:

**WeChat Pay:**
- Payment Request: "付款方式" AND "支付" AND contains amount
- Payment Success: "支付成功" AND "完成" AND contains amount

**Alipay:**
- Payment Request: "付款给" AND "确认付款"
- Payment Success: "支付成功" AND "完成"

**Why fuzzy matching?**
- UI layouts change between app versions
- Resource IDs are not stable across releases
- Keywords are more resilient to minor UI changes

### Transaction Extraction

**Amount Detection:**
- Currency pattern regex: `[￥¥$][\d,]+\.?\d*`
- WeChat Pay: Find text node with largest bounds containing amount pattern
- Alipay Request: Parse contentDescription with regex `付款金额([\d.]+)元`
- Alipay Success: Find text node with largest bounds containing amount pattern

**Duplicate Prevention:**
- 2-second debounce on accessibility events
- Transaction hash (amount + viewType) tracking
- Prevents duplicate notifications from frequent events

---

## Next Steps (Phase 4)

1. **Persist Transactions:** Save TransactionInfo to local database (Room)
2. **Cloud Sync:** Implement optional sync to cloud services
3. **Transaction History UI:** Build interface to view and manage recorded transactions
4. **Category Management:** Add support for transaction categorization and tagging

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
|-- notification/
|   |-- TransactionNotificationManager.kt
|-- overlay/
|   |-- FloatingOverlayService.kt
|   |-- FloatingOverlayManager.kt
|-- data/
|   |-- FileStorageManager.kt
|-- util/
    |-- PermissionHelper.kt
    |-- ViewTreeParser.kt

app/src/main/java/com/google/android/accessibility/selecttospeak/
|-- SelectToSpeakService.kt

app/src/main/res/xml/
|-- accessibility_service_config.xml
```

### Core Model Classes

#### `model/ViewNodeInfo.kt`
Data class representing a single view node in the hierarchy.

```kotlin
data class ViewNodeInfo(
    val className: String?,           // View class (e.g., "android.widget.TextView")
    val text: String?,                // Visible text content
    val contentDescription: String?,  // Accessibility description
    val viewIdResourceName: String?,  // Resource ID (e.g., "com.app:id/button")
    val boundsInScreen: Rect?,        // Screen coordinates
    val isClickable: Boolean,         // Can be clicked
    val isEnabled: Boolean,           // Enabled state
    val isFocusable: Boolean,         // Can receive focus
    val isScrollable: Boolean,        // Can be scrolled
    val childCount: Int,              // Number of children
    val depth: Int,                   // Depth in tree (0 = root)
    val children: List<ViewNodeInfo>  // Child nodes
)
```

**Use Case:** Serializes the AccessibilityNodeInfo tree into a format that can be saved as JSON.

---

#### `model/InspectionResult.kt`
Complete inspection result for a single view capture.

```kotlin
data class InspectionResult(
    val timestamp: Long,              // Capture time (milliseconds)
    val packageName: String,          // App package (e.g., "com.tencent.mm")
    val activityName: String?,        // Activity identifier
    val rootNode: ViewNodeInfo,       // Root of view tree
    val totalViewCount: Int           // Total nodes in tree
)
```

**Use Case:** Wraps the view tree with metadata for analysis. Saved as one JSON file per inspection.

---

#### `model/TransactionInfo.kt`
Data model for extracted transaction information.

```kotlin
data class TransactionInfo(
    val amount: String,              // Transaction amount (e.g., "￥12.50")
    val merchant: String?,           // Merchant name (currently null)
    val paymentMethod: String?,      // Payment method (currently null)
    val timestamp: Long,             // Detection timestamp
    val viewType: PaymentViewType    // Request or Success
)

sealed class PaymentViewType {
    object Request : PaymentViewType()  // Payment authentication screen
    object Success : PaymentViewType()  // Payment success screen
    object None : PaymentViewType()     // Not a payment view
}
```

**TransactionInfo:** Represents a detected and extracted transaction. Currently shown as notification, will be persisted in Phase 4.

**PaymentViewType:** Used by handlers to indicate what type of payment view was detected. Determines notification content and future behavior.

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

---

#### `handler/PaymentAppHandlerFactory.kt`
Factory object for routing to appropriate payment handler.

```kotlin
object PaymentAppHandlerFactory {
    fun getHandler(packageName: String): PaymentAppHandler?
}
```

**Use Case:** Routes accessibility events to the correct handler based on package name. Returns null for unsupported apps.

---

#### `handler/WeChatPayHandler.kt`
Handler implementation for WeChat Pay detection and extraction.

**Detection Logic:**
- Collects all text from view hierarchy
- Checks for keyword combinations: "付款方式" + "支付" + amount for request
- Checks for keyword combinations: "支付成功" + "完成" + amount for success

**Extraction Logic:**
- Collects all text nodes
- Filters nodes containing currency amounts
- Returns text from node with largest screen bounds

---

#### `handler/AlipayHandler.kt`
Handler implementation for Alipay detection and extraction.

**Detection Logic:**
- Collects all text from view hierarchy
- Checks for keyword combinations: "付款给" + "确认付款" for request
- Checks for keyword combinations: "支付成功" + "完成" for success

**Extraction Logic:**
- For request views: Parses contentDescription using regex `付款金额([\d.]+)元`
- For success views: Finds largest text node with currency amount

---

#### `handler/PatternMatcher.kt`
Utility object for pattern matching in text.

**Functions:**
- `containsAmount(text)`: Returns true if text contains currency pattern `[￥¥$][\d,]+\.?\d*`
- `extractAmount(text)`: Extracts first amount match from text
- `containsKeywords(text, keywords)`: Checks if text contains any of the keywords

**Use Case:** Used by handlers to detect amounts and keywords in view text.

---

### Notification Classes

#### `notification/TransactionNotificationManager.kt`
Manages Android notifications for detected transactions.

**Features:**
- Creates notification channel for Android O+
- Shows different titles for Request vs Success views
- Includes amount and status information
- Auto-cancel on tap

**Notification Content:**
- Request: "检测到支付请求" + amount + "状态: 等待确认"
- Success: "支付成功" + amount

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

---

#### `util/ViewTreeParser.kt`
Converts AccessibilityNodeInfo tree to ViewNodeInfo data structure.

**Functions:**
- `parseNodeTree(node, depth)`: Recursively parses AccessibilityNodeInfo into ViewNodeInfo
- `countViews(node)`: Counts total nodes in tree

---

### Data Layer

#### `data/FileStorageManager.kt`
Manages JSON file I/O for inspection results.

**Storage Location:** `/data/data/com.example.accounting/files/inspections/inspection_<timestamp>.json`

**Key Features:**
- Custom Gson TypeAdapter for `Rect` serialization (saves as array: `[left, top, right, bottom]`)
- Pretty-printed JSON output for readability
- Automatically creates inspection directory on first use

**Functions:**
- `saveInspection(result)`: Saves InspectionResult to timestamped JSON file
- `getInspectionFiles()`: Returns list of all inspection files, newest first
- `deleteInspection(file)`: Deletes a specific inspection file

---

### Accessibility Service

#### `service/ViewInspectionAccessibilityService.kt`
Base accessibility service that handles view inspection and payment detection.

**Key Components:**

1. **BroadcastReceiver** - Listens for `INSPECT_ACTION` broadcast (Phase 1)
2. **performInspection()** - Captures and saves view tree (Phase 1)
3. **onAccessibilityEvent()** - Real-time payment detection and extraction (Phase 2 & 3)

**Payment Detection Flow:**
1. Receives `WINDOW_CONTENT_CHANGED` or `WINDOW_STATE_CHANGED` events
2. Checks if package name is supported via `PaymentAppHandlerFactory`
3. Parses view tree using `ViewTreeParser`
4. Calls `handler.detectPaymentView()` to check if it's a payment view
5. If payment view detected, calls `handler.extractTransaction()`
6. Shows notification via `TransactionNotificationManager`
7. Uses debounce (2 seconds) and hash tracking to prevent duplicates

**Important:** The class is marked `open` so it can be extended by the disguised service.

---

#### `com.google.android.accessibility.selecttospeak/SelectToSpeakService.kt`
Disguised accessibility service that extends the base service.

**Purpose:** Some apps (like WeChat) restrict accessibility services from unknown packages. By disguising as Google's "Select to Speak" service, we can bypass these restrictions.

**Implementation:**
```kotlin
package com.google.android.accessibility.selecttospeak

import com.example.accounting.service.ViewInspectionAccessibilityService

class SelectToSpeakService: ViewInspectionAccessibilityService() {
    // Inherits all functionality from base class
    // Empty wrapper for package name disguise
}
```

**Important:** Do not add code to this class. It is only a wrapper for the package name disguise. All logic should be in `ViewInspectionAccessibilityService`.

---

### Overlay Service

#### `overlay/FloatingOverlayService.kt`
Foreground service that hosts the floating button.

**Responsibilities:**
1. Creates notification channel and foreground notification
2. Instantiates `FloatingOverlayManager`
3. Sends inspection broadcast when button is clicked

**Implementation Note:**
```kotlin
private fun sendInspectionBroadcast() {
    val intent = Intent(ViewInspectionAccessibilityService.INSPECT_ACTION)
    intent.setPackage(packageName)  // Explicit package for safe delivery
    sendBroadcast(intent)
}
```

The `setPackage()` call ensures the broadcast is only received by our app.

---

#### `overlay/FloatingOverlayManager.kt`
Creates and manages the floating button window.

**Key Features:**
- Uses traditional Android `FrameLayout` + `TextView` (not Compose) to avoid lifecycle issues
- Button size: 64dp diameter circular button
- Draggable anywhere on screen
- Click vs drag detection (10px threshold)

**Touch Event Handling:**
```kotlin
ACTION_DOWN -> record initial position
ACTION_MOVE -> update window position
ACTION_UP -> check if moved < 10px, if so trigger click
```

**Window Parameters:**
```kotlin
LayoutParams(
    WRAP_CONTENT, WRAP_CONTENT,
    TYPE_APPLICATION_OVERLAY,  // Required for overlay
    FLAG_NOT_FOCUSABLE or FLAG_WATCH_OUTSIDE_TOUCH,
    PixelFormat.TRANSLUCENT
)
```

---

### UI

#### `MainActivity.kt`
Jetpack Compose UI for permission management and service control.

**Workflow:**
1. On launch: Check permission status
2. If permissions missing: Show "Grant" buttons
3. On resume: Re-check permissions (user may have changed settings)
4. If all granted: Show Start/Stop service buttons

**State Management:**
```kotlin
var isServiceRunning by remember { mutableStateOf(false) }

// Service status doesn't persist across activity recreation
// User sees "Service stopped" if they return to app
```

---

### Configuration Files

#### `res/xml/accessibility_service_config.xml`
Accessibility service configuration for the disguised service.

**Key Settings:**
```xml
android:accessibilityEventTypes="typeWindowContentChanged|typeWindowStateChanged|typeAllMask"
android:accessibilityFlags="flagRetrieveInteractiveWindows|flagIncludeNotImportantViews|flagReportViewIds|flagRequestEnhancedWebAccessibility"
android:canRetrieveWindowContent="true"
```

**These flags maximize view tree visibility:**
- `flagRetrieveInteractiveWindows` - Access all windows, not just active
- `flagIncludeNotImportantViews` - Include views marked as not important
- `flagReportViewIds` - Include resource IDs
- `flagRequestEnhancedWebAccessibility` - Better web content access

---

#### `AndroidManifest.xml`
Permissions and service declarations.

**Permissions:**
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
```

**Service Declarations:**
```xml
<!-- Disguised accessibility service -->
<service
    android:name="com.google.android.accessibility.selecttospeak.SelectToSpeakService"
    android:exported="false"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>

<!-- Floating overlay service -->
<service
    android:name=".overlay.FloatingOverlayService"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_ALLOWED_SUBTYPE"
        android:value="view_inspection" />
</service>
```

---

## Usage Instructions

### First Time Setup

1. Install APK on device
2. Launch app
3. Tap "Grant Overlay Permission" - Enable in Settings
4. Return to app
5. Tap "Grant Accessibility Permission" - Open Accessibility Settings
6. Find "Accounting" and enable it
7. Return to app

### Capturing Views (for developer)

1. Tap "Start Inspection" button
2. Floating "V" button appears on screen
3. Navigate to target app (e.g., WeChat Pay)
4. Tap floating button when on desired screen
5. Toast confirms inspection saved
6. Repeat for other screens

#### Viewing Results

**Using ADB:**
```bash
adb shell run-as com.example.accounting
cd files/inspections
ls -la
cat inspection_1707891234567.json
```

**Using Android Studio:**
1. Open Device File Explorer
2. Navigate to `/data/data/com.example.accounting/files/inspections/`
3. Right-click file - Save As
4. Open JSON file in text editor

---

## Dependencies

- Gson 2.10.1 (JSON serialization)
- AndroidX Core (ContextCompat for service starting)
- Jetpack Compose (UI)
- Coroutines (background threading)
