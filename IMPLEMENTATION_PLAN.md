# Floating View Inspector - Implementation Plan

## Context

This plan outlines the implementation of a floating button overlay for an Android application that inspects view hierarchies from any app using Accessibility Services. The goal is to help the user analyze payment view structures by capturing the view tree and package information when the floating button is clicked.

**Primary Use Case:** When the user opens a payment app (like WeChat Pay, Alipay, etc.), they can click the floating button to capture the current view structure and save it to a JSON file for later analysis. This will help identify the correct package names and view tree structures needed for building an automated accounting feature.

**Key Requirements:**
- Floating button always displayed on top of other apps
- Inspect current view tree when clicked
- Save results to files (no database)
- Keep UI simple with no decoration
- Must be simple and functional for initial development

## Architecture Overview

### Core Components

1. **ViewInspectionAccessibilityService** - Accessibility service that captures view hierarchy from any app
2. **FloatingOverlayService** - Foreground service that manages the floating button overlay
3. **Permission Management** - Handle overlay and accessibility permissions in MainActivity
4. **File Storage** - Save inspection results as JSON files in app-private storage

### Data Flow

```
User clicks floating button
  → FloatingOverlayService sends broadcast
  → ViewInspectionAccessibilityService receives broadcast
  → Accessibility service captures current AccessibilityNodeInfo
  → ViewTreeParser converts node tree to ViewNodeInfo data structure
  → FileStorageManager saves to JSON file
  → Toast notification confirms save
```

## Required Permissions

### AndroidManifest.xml additions:

```xml
<!-- For displaying floating overlay -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- For foreground service (Android 9+) -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
```

**Note:** The `BIND_ACCESSIBILITY_SERVICE` permission is automatically added when declaring the accessibility service in the manifest.

## Implementation Steps

### Step 1: Add Dependencies

**File:** `app/build.gradle.kts`

Add JSON serialization library:
```kotlin
dependencies {
    // Add Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
}
```

### Step 2: Create Data Models

**Files to create:**
- `app/src/main/java/com/example/accounting/model/ViewNodeInfo.kt`
- `app/src/main/java/com/example/accounting/model/InspectionResult.kt`

**ViewNodeInfo.kt** - Data class representing a view node in the hierarchy:
```kotlin
data class ViewNodeInfo(
    val className: String?,
    val text: String?,
    val contentDescription: String?,
    val viewIdResourceName: String?,
    val boundsInScreen: Rect?,
    val isClickable: Boolean,
    val isEnabled: Boolean,
    val isFocusable: Boolean,
    val isScrollable: Boolean,
    val childCount: Int,
    val depth: Int,
    val children: List<ViewNodeInfo>
)
```

**InspectionResult.kt** - Complete inspection result:
```kotlin
data class InspectionResult(
    val timestamp: Long,
    val packageName: String,
    val activityName: String?,
    val rootNode: ViewNodeInfo,
    val totalViewCount: Int
)
```

### Step 3: Create Utility Classes

**Files to create:**
- `app/src/main/java/com/example/accounting/util/PermissionHelper.kt`
- `app/src/main/java/com/example/accounting/util/ViewTreeParser.kt`

**PermissionHelper.kt** - Check and request permissions:
```kotlin
object PermissionHelper {
    fun checkOverlayPermission(context: Context): Boolean
    fun checkAccessibilityPermission(context: Context): Boolean
    fun requestOverlayPermission(context: Context)
    fun requestAccessibilityPermission(context: Context)
}
```

**ViewTreeParser.kt** - Parse AccessibilityNodeInfo tree:
```kotlin
object ViewTreeParser {
    fun parseNodeTree(node: AccessibilityNodeInfo?, depth: Int = 0): ViewNodeInfo?
    fun countViews(node: ViewNodeInfo?): Int
}
```

### Step 4: Create File Storage Layer

**Files to create:**
- `app/src/main/java/com/example/accounting/data/FileStorageManager.kt`

**FileStorageManager.kt** - Manage JSON file I/O:
```kotlin
class FileStorageManager(private val context: Context) {
    fun saveInspection(result: InspectionResult): File
    fun getInspectionFiles(): List<File>
    fun deleteInspection(file: File): Boolean
}
```

**Storage Location:** `context.filesDir/inspections/inspection_<timestamp>.json`

### Step 5: Create Accessibility Service

**Files to create:**
- `app/src/main/java/com/example/accounting/service/ViewInspectionAccessibilityService.kt`
- `app/src/main/res/xml/accessibility_service_config.xml`

**ViewInspectionAccessibilityService.kt**:
- Extends `AccessibilityService`
- Listens for broadcast intent "com.example.accounting.INSPECT_ACTION"
- Captures `rootInActiveWindow` when triggered
- Uses `ViewTreeParser` to convert to `ViewNodeInfo`
- Saves to file via `FileStorageManager`
- Shows toast notification on save

**accessibility_service_config.xml**:
```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100"
    android:settingsActivity="com.example.accounting.MainActivity" />
```

### Step 6: Create Overlay Service

**Files to create:**
- `app/src/main/java/com/example/accounting/overlay/FloatingOverlayService.kt`
- `app/src/main/java/com/example/accounting/overlay/FloatingOverlayManager.kt`

**FloatingOverlayService.kt**:
- Foreground service with persistent notification
- Creates and manages floating button window
- Handles button clicks to send inspection broadcast
- Handles button drag for repositioning

**FloatingOverlayManager.kt**:
- Manages `WindowManager` and `LayoutParams`
- Creates Compose-based floating button UI
- Window type: `TYPE_APPLICATION_OVERLAY`
- Handles touch events for dragging

**Floating Button UI:**
- Simple circular button (48dp diameter)
- Basic Material3 styling
- Click handler to trigger inspection
- Minimal decoration as per requirements

### Step 7: Update AndroidManifest.xml

**Add permissions:**
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
```

**Register services:**
```xml
<service
    android:name=".service.ViewInspectionAccessibilityService"
    android:exported="false"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>

<service
    android:name=".overlay.FloatingOverlayService"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_ALLOWED_SUBTYPE"
        android:value="view_inspection" />
</service>
```

### Step 8: Update MainActivity

**File:** `app/src/main/java/com/example/accounting/MainActivity.kt`

**Implementation:**
1. Check overlay and accessibility permissions on launch
2. Show permission request UI if permissions not granted:
   - "Grant Overlay Permission" button
   - "Grant Accessibility Permission" button
   - Clear instructions for each permission
3. Check permission status in `onResume()`
4. Once both permissions granted, show "Start Inspection" button
5. Start/Stop `FloatingOverlayService` via `startForegroundService()`
6. Display current service status

**UI Design:**
- Simple vertical layout (Column)
- Status text showing permission states
- Action buttons for permission requests
- Start/Stop service button
- No complex decorations or styling

### Step 9: Add String Resources

**File:** `app/src/main/res/values/strings.xml`

```xml
<string name="accessibility_service_description">Allows this app to inspect view hierarchies for debugging and analyzing payment interfaces.</string>
<string name="overlay_notification_title">View Inspector Active</string>
<string name="overlay_notification_content">Inspection overlay is running</string>
<string name="overlay_permission_required">Overlay permission is required to display the floating button</string>
<string name="accessibility_permission_required">Accessibility permission is required to inspect views</string>
<string name="inspection_saved">Inspection saved to file</string>
```

### Step 10: Create Notification Channel

**In FloatingOverlayService.onCreate():**
```kotlin
private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "view_inspector",
            "View Inspector",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
```

## File Structure Summary

```
app/src/main/java/com/example/accounting/
├── MainActivity.kt (modified)
├── model/
│   ├── ViewNodeInfo.kt (new)
│   └── InspectionResult.kt (new)
├── service/
│   └── ViewInspectionAccessibilityService.kt (new)
├── overlay/
│   ├── FloatingOverlayService.kt (new)
│   └── FloatingOverlayManager.kt (new)
├── data/
│   └── FileStorageManager.kt (new)
└── util/
    ├── PermissionHelper.kt (new)
    └── ViewTreeParser.kt (new)

app/src/main/res/
├── xml/
│   └── accessibility_service_config.xml (new)
└── values/
    └── strings.xml (modified)
```

## JSON Output Format

**File location:** `/data/data/com.example.accounting/files/inspections/inspection_1707891234567.json`

**Example structure:**
```json
{
  "timestamp": 1707891234567,
  "packageName": "com.example.paymentapp",
  "activityName": "PaymentActivity",
  "totalViewCount": 42,
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
    "children": [
      {
        "className": "android.widget.LinearLayout",
        "text": null,
        "contentDescription": null,
        "viewIdResourceName": "com.example.paymentapp:id/root",
        "boundsInScreen": [0, 0, 1080, 2400],
        "isClickable": false,
        "isEnabled": true,
        "isFocusable": false,
        "isScrollable": false,
        "childCount": 3,
        "depth": 1,
        "children": [...]
      }
    ]
  }
}
```

## User Flow

1. **First Launch:**
   - App opens to MainActivity
   - Shows permission request screen with clear instructions
   - User taps "Grant Overlay Permission" → Opens Settings
   - User grants permission and returns to app
   - User taps "Grant Accessibility Permission" → Opens Accessibility Settings
   - User finds "Accounting" app and enables accessibility service
   - User returns to app

2. **Starting Inspection:**
   - Both permissions now show as "Granted"
   - "Start Inspection" button becomes enabled
   - User taps button → Floating button appears on screen

3. **Using Floating Button:**
   - User navigates to payment app (e.g., WeChat Pay)
   - Opens payment view
   - Taps floating button
   - Toast shows "Inspection saved to file"
   - User navigates to next payment screen
   - Taps button again to capture another view

4. **Viewing Results:**
   - User can use Device File Explorer in Android Studio
   - Navigate to `/data/data/com.example.accounting/files/inspections/`
   - Export JSON files for analysis
   - Each file contains timestamp, package name, and complete view tree

## Verification Steps

### 1. Test Permission Flow
- Install app on device/emulator
- Verify permission request UI displays correctly
- Test overlay permission request → Settings opens
- Test accessibility permission request → Accessibility Settings opens
- Verify permission status updates when returning to app

### 2. Test Overlay Service
- After granting permissions, tap "Start Inspection"
- Verify floating button appears on screen
- Verify button can be dragged to different positions
- Verify button stays visible when navigating between apps
- Verify notification appears in notification drawer

### 3. Test View Inspection
- Open a test app (e.g., Settings app)
- Tap floating button
- Verify toast message "Inspection saved to file" appears
- Check that JSON file was created in `/files/inspections/`
- Verify JSON format includes all expected fields
- Verify view tree structure accurately reflects the UI

### 4. Test Different Apps
- Open various apps and tap inspection button
- Verify each app's package name is captured correctly
- Verify view structures are different for different apps
- Test with payment apps (final use case)

### 5. Test Edge Cases
- Test with apps that have very deep view hierarchies
- Test with apps that have minimal view hierarchies
- Test when accessibility service is disabled mid-use
- Test when overlay permission is revoked mid-use
- Test button drag functionality across different screen positions

## Technical Considerations

### Minimum SDK Version
- Current: minSdk 24 (Android 7.0)
- All features are compatible with API 24+

### Target SDK Version
- Current: targetSdk 36
- Must use `foregroundServiceType` for Android 14+ (API 34+)
- Notification channels required for Android 8.0+ (API 26+)

### Performance Considerations
- View tree parsing should be fast (occurs on main thread)
- JSON serialization should be done on background thread (use coroutines)
- File I/O should be done on background thread
- Floating button should be lightweight to avoid lag

### Security Considerations
- All files stored in app-private storage (no external storage permission needed)
- No network access required
- User must explicitly enable accessibility service in Settings
- Clear explanation of why accessibility permission is needed

### Accessibility Service Configuration Flags
- `canRetrieveWindowContent="true"` - Required to access view hierarchy
- `accessibilityEventTypes="typeWindowStateChanged"` - Track window changes for context
- `accessibilityFeedbackType="feedbackGeneric"` - Standard feedback type

## Known Limitations

1. **Privacy Restrictions:** Some apps may hide sensitive content from accessibility services (e.g., payment amounts might be masked)
2. **Secure Views:** Views with `FLAG_SECURE` cannot be captured
3. **System Alerts:** Some system dialogs may not be fully accessible
4. **Background Limits:** Accessibility service may be throttled in background on some devices
5. **Manufacturer Customization:** Some OEM skins may have different view structures

## Future Enhancements (Out of Scope)

These features are intentionally excluded from the initial implementation:
- In-app viewer for inspection files
- Search/filter capabilities
- Export to external storage
- Screenshot capture alongside view tree
- Comparison between inspections
- View filtering options (e.g., only clickable views)
- Database storage instead of file-based
- Cloud sync for inspection data
- Automatic analysis of payment views

## Dependencies Summary

**Existing:** No changes to existing dependencies

**New:**
- Gson 2.10.1 (for JSON serialization)

**Total additions:** 1 dependency

## Estimated Code Volume

- **New Files:** 9 Kotlin files
- **Modified Files:** 3 files (MainActivity, AndroidManifest, strings.xml, build.gradle)
- **Total Lines:** ~1000-1200 lines of Kotlin code

## Build and Run Instructions

1. Open project in Android Studio
2. Sync Gradle files (will download Gson dependency)
3. Build and install on device/emulator
4. Launch app
5. Grant overlay permission in Settings
6. Grant accessibility permission in Accessibility Settings
7. Tap "Start Inspection" in app
8. Navigate to any app and tap floating button to inspect

## Testing Strategy

Since this is an accessibility-based tool, testing should be done manually on a physical device or emulator:

1. **Unit Tests:**
   - Test ViewTreeParser with mock AccessibilityNodeInfo
   - Test FileStorageManager serialization/deserialization
   - Test PermissionHelper methods

2. **Instrumented Tests:**
   - Test file storage operations
   - Test permission checking methods

3. **Manual Tests:**
   - Permission flow on fresh install
   - Floating button interaction and dragging
   - View inspection accuracy across different apps
   - File generation and format verification

---

**Implementation Ready:** This plan provides all necessary details to begin implementation. The step-by-step approach ensures each component is built incrementally and can be tested independently before integration.
