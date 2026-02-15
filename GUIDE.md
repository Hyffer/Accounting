# Auto Accounting - Project Guide

## Overall Goal

**Build an Android application that automatically keeps accounts when the user pays bills.**

The application uses **Accessibility Services** to detect payment views on screen, extract transaction information, and record it automatically. This eliminates the need for manual bookkeeping when making payments through apps like WeChat Pay, Alipay, etc.

## Project Phases

### Phase 1: View Inspector (Completed)
Build a floating button that inspects the current view hierarchy and saves it for later analysis. This helps identify the structure of payment views in target apps.

### Phase 2: Payment Detection (Planned)
Detect when a payment view is displayed on screen by matching package names and view patterns discovered in Phase 1.

### Phase 3: Transaction Extraction (Planned)
Extract transaction details (amount, merchant, time, etc.) from detected payment views using accessibility APIs.

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

---

## File Structure

```
app/src/main/java/com/example/accounting/
|-- MainActivity.kt
|-- model/
|   |-- ViewNodeInfo.kt
|   |-- InspectionResult.kt
|-- service/
|   |-- ViewInspectionAccessibilityService.kt
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

**Implementation Details:**
```kotlin
fun parseNodeTree(node: AccessibilityNodeInfo?, depth: Int = 0): ViewNodeInfo? {
    if (node == null) return null

    val bounds = Rect()
    node.getBoundsInScreen(bounds)

    val children = mutableListOf<ViewNodeInfo>()
    for (i in 0 until node.childCount) {
        val childNode = node.getChild(i)
        val childInfo = parseNodeTree(childNode, depth + 1)
        if (childInfo != null) {
            children.add(childInfo)
        }
        childNode?.recycle()  // Important: recycle to avoid memory leaks
    }

    return ViewNodeInfo(...)
}
```

**Memory Management:** Every `getChild()` call creates a new AccessibilityNodeInfo that must be recycled. The parser handles this automatically.

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
Base accessibility service that handles view inspection.

**Key Components:**

1. **BroadcastReceiver** - Listens for `INSPECT_ACTION` broadcast
2. **performInspection()** - Captures and saves view tree
3. **Error Handling** - Warns user if view hierarchy is empty or restricted

**Implementation Details:**
```kotlin
open class ViewInspectionAccessibilityService : AccessibilityService() {
    companion object {
        const val INSPECT_ACTION = "com.example.accounting.INSPECT_ACTION"
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    // Uses coroutines for background file I/O
    private fun performInspection() {
        scope.launch(Dispatchers.IO) {
            val viewNodeInfo = ViewTreeParser.parseNodeTree(rootNode)
            val file = fileStorageManager.saveInspection(result)
            // Show toast on Main thread
        }
    }
}
```

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
}
```

**Key Trick:** The package name `com.google.android.accessibility.selecttospeak` appears in the accessibility settings, making it look like an official Google service.

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

### Capturing Views

1. Tap "Start Inspection" button
2. Floating "V" button appears on screen
3. Navigate to target app (e.g., WeChat Pay)
4. Tap floating button when on desired screen
5. Toast confirms inspection saved
6. Repeat for other screens

### Viewing Results

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

## JSON Output Format

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

## Next Steps (Phase 2)

1. **Analyze Inspections:** Review captured JSON files to identify patterns in payment views
2. **Define Patterns:** Create rules to detect payment screens (package name + view structure)
3. **Implement Detection:** Add real-time monitoring for payment view display
4. **Test on Multiple Apps:** Validate detection works across WeChat Pay, Alipay, etc.

---

## Dependencies

- Gson 2.10.1 (JSON serialization)
- AndroidX Core (ContextCompat for service starting)
- Jetpack Compose (UI)
- Coroutines (background threading)
