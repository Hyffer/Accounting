# Implementation Plan: Multi-Page Navigation Architecture

## Overview

This plan refactors the current single-activity architecture into a multi-page navigation system that separates permission requests from the main functionality and introduces a bottom navigation bar with two tabs.

---

## Current Architecture

```
┌─────────────────────────────────────────┐
│          MainActivity                    │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │ Permission Management              │ │
│  │  - Overlay Permission              │ │
│  │  - Accessibility Permission        │ │
│  └────────────────────────────────────┘ │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │ Service Control                    │ │
│  │  - Start/Stop Inspection Service   │ │
│  │  - Instructions                    │ │
│  └────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

**Issues:**
- Permission logic and service control are tightly coupled
- No navigation structure for future features
- All functionality in a single screen

---

## Proposed Architecture

### Activity Structure

```
┌──────────────────────────────────────────────────────────┐
│                    MainActivity                           │
│  (Single Activity - Navigation Host)                     │
│                                                           │
│  ┌─────────────────────────────────────────────────────┐ │
│  │  NavHost (Jetpack Compose Navigation)               │ │
│  │                                                      │ │
│  │  ┌──────────────┐        ┌────────────────────────┐ │ │
│  │  │ Permission   │        │  Main Screen           │ │ │
│  │  │ Request      │───────>│  (Bottom Nav Host)     │ │ │
│  │  │ Screen       │        │                        │ │ │
│  │  └──────────────┘        │  ┌──────────────────┐  │ │ │
│  │                          │  │ Recent Tab       │  │ │ │
│  │                          │  │ (Placeholder)    │  │ │ │
│  │                          │  └──────────────────┘  │ │ │
│  │                          │  ┌──────────────────┐  │ │ │
│  │                          │  │ Dev Tab          │  │ │ │
│  │                          │  │ (Original        │  │ │ │
│  │                          │  │  functionality)  │  │ │ │
│  │                          │  └──────────────────┘  │ │ │
│  │                          └────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

### User Flow Diagram

```
┌──────────────┐
│ App Launch   │
└──────┬───────┘
       │
       v
┌──────────────────────┐
│ Check Permissions    │
│ - Overlay?           │
│ - Accessibility?     │
└──────┬────────┬──────┘
       │        │
       v        v
       │        └─────────────────────────┐
       │                                  │
    Missing                               │ All Granted
       │                                  │
       v                                  v
┌──────────────────┐             ┌──────────────────┐
│ Permission       │             │ Main Screen      │
│ Request Screen   │             │ (Bottom Nav)     │
│                  │             │                  │
│ [Grant Overlay]  │             │ ┌──────────────┐ │
│ [Grant Access.]  │             │ │ Recent Tab   │ │
│                  │             │ │ (placeholder)│ │
│ After granting   │             │ └──────────────┘ │
│ both permissions │             │ ┌──────────────┐ │
│      │           │             │ │ Dev Tab      │ │
│      │           │             │ │ (Start/Stop) │ │
│      └───────────┼────────────>│ └──────────────┘ │
└──────────────────┘             └──────────────────┘
```

### Navigation Flow

```
Start Destination
       │
       ├── Missing Permissions ──> permissionRequest
       │                                 │
       │                                 │ (Permission granted)
       │                                 v
       │                            mainScreen
       │
       └── All Granted ──────────> mainScreen
                                          │
                                          ├── Tab 0: recentScreen (placeholder)
                                          └── Tab 1: devScreen (start/stop service)
```

---

## Screen Specifications

### 1. Permission Request Screen

**Purpose:** Handle all permission requests before allowing access to main functionality

**Content:**
- App title/header
- Permission status card:
  - Overlay permission status
  - Accessibility permission status
- Grant buttons (shown only when specific permission missing)
- Instructions/tips for granting permissions

**Behavior:**
- Automatically navigates to Main Screen once all permissions granted
- Monitors permission state on resume
- Cannot be navigated away from until permissions granted

### 2. Main Screen (Bottom Navigation Container)

**Purpose:** Host bottom navigation and display selected tab content

**Content:**
- Top app bar with title (changes based on selected tab)
- Main content area (displays current tab)
- Bottom navigation bar with 2 items:
  - **Recent** (left)
  - **Dev** (right)

**Navigation Logic:**
- Back button behavior:
  - If on Dev tab → go to Recent tab
  - If on Recent tab → exit app
  - If permissions revoked → return to Permission Request Screen

### 3. Recent Tab (Placeholder)

**Purpose:** Reserved for future transaction history feature

**Content:**
- Placeholder UI (e.g., "Coming Soon" message or empty state)
- Future: Will display list of recent transactions

**Status:** Empty/placeholder for now

### 4. Dev Tab

**Purpose:** Original inspection service management functionality

**Content:**
- Service status card:
  - "Service is running" / "Service is stopped"
  - Start Inspection button
  - Stop Inspection button
- Instructions card:
  - How to use the floating button
  - Where files are saved

**Behavior:**
- Start/Stop FloatingOverlayService
- Monitor service running state
- Exact functionality from original MainActivity

---

## Technical Implementation Details

### Dependencies Required

Add to `app/build.gradle.kts`:

```kotlin
// Navigation Compose
implementation("androidx.navigation:navigation-compose:2.7.7")
```

### File Structure

```
app/src/main/java/com/example/accounting/
├── MainActivity.kt                    (Refactored - NavHost only)
├── ui/
│   ├── screen/
│   │   ├── PermissionRequestScreen.kt   (NEW)
│   │   ├── MainScreen.kt                 (NEW - Bottom Nav Host)
│   │   ├── RecentScreen.kt               (NEW - Placeholder)
│   │   └── DevScreen.kt                  (NEW - Service Control)
│   ├── components/
│   │   ├── PermissionStatusCard.kt       (NEW - Reusable component)
│   │   └── BottomNavigationBar.kt        (NEW)
│   └── navigation/
│       └── AppNavigation.kt              (NEW - Navigation graph)
└── ... (existing files unchanged)
```

### Key Components

#### 1. AppNavigation.kt
- Define navigation graph
- Set start destination based on permission state
- Handle navigation between screens

#### 2. PermissionRequestScreen.kt
- Extract permission UI logic from MainActivity
- Monitor permission state changes
- Auto-navigate on permission grant

#### 3. MainScreen.kt
- Scaffold with bottom navigation
- Manage selected tab state
- Host current tab content

#### 4. DevScreen.kt
- Extract service control logic from MainActivity
- Start/Stop FloatingOverlayService
- Display instructions

#### 5. RecentScreen.kt
- Placeholder UI
- Prepared for future transaction list implementation

### MainActivity.kt Changes

**Before:**
- Contains all UI logic (permissions + service control)

**After:**
- Minimal setup
- Sets up NavHost
- Delegates to navigation graph

```kotlin
// Pseudocode structure
@Composable
fun MainActivity() {
    val navController = rememberNavController()
    val startDestination = determineStartDestination()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("permissionRequest") {
            PermissionRequestScreen(
                onPermissionsGranted = {
                    navController.navigate("main") {
                        popUpTo("permissionRequest") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScreen()
        }
    }
}
```

### Permission State Management

**Strategy:**
- Check permissions in `MainActivity.onCreate()` and on resume
- Use `DisposableEffect` to monitor lifecycle changes
- Store permission state in ViewModel or remember blocks
- Navigate automatically when permissions change

**Edge Cases:**
- User revokes permission while in Main Screen:
  - Detect on resume
  - Navigate back to Permission Request Screen
- User denies permission multiple times:
  - Show persistent UI with manual settings link

### Bottom Navigation Implementation

**State:**
- Use `remember` to track selected tab index
- Default to "Recent" tab (index 0)

**UI:**
- Material3 BottomNavigationBar
- Two BottomNavigationItem widgets:
  - Icon + Label for "Recent"
  - Icon + Label for "Dev"

**Content Switching:**
- Simple when expression to show correct screen based on selected index
- No nested navigation needed for tabs (just content swapping)

---

## Implementation Steps

### Phase 1: Setup Navigation Infrastructure
1. Add navigation dependencies to `build.gradle.kts`
2. Create `ui/navigation/AppNavigation.kt`
3. Create `ui/screen/` directory structure
4. Add navigation graph and destination definitions

### Phase 2: Extract Permission UI
1. Create `PermissionRequestScreen.kt`
2. Extract permission UI components from MainActivity
3. Add navigation logic from permission screen to main screen
4. Update MainActivity to use navigation graph

### Phase 3: Implement Main Screen Structure
1. Create `MainScreen.kt` with bottom navigation
2. Create `RecentScreen.kt` placeholder
3. Create `DevScreen.kt` with existing service control logic
4. Test bottom navigation tab switching

### Phase 4: Refactor MainActivity
1. Remove UI logic from MainActivity
2. Set up NavHost in MainActivity
3. Add permission check for start destination
4. Add lifecycle monitoring for permission changes

### Phase 5: Polish and Testing
1. Add transitions between screens
2. Test permission revoke/restore scenarios
3. Test back button navigation
4. Verify service control still works correctly
5. Update documentation

---

## Migration Strategy

### Preserving Existing Functionality

The **Dev Tab** will contain all original functionality from MainActivity:
- Service status display
- Start/Stop buttons
- Instructions card

**No changes to:**
- FloatingOverlayService
- ViewInspectionAccessibilityService
- Any other services or utilities

### Backward Compatibility

- All existing intent filters remain in MainActivity
- No changes to AndroidManifest.xml (except potential new activities if needed)
- Service communication remains unchanged

---

## UI/UX Improvements

### Permission Request Screen
- Clear visual hierarchy
- Status indicators (checkmarks/checks for granted permissions)
- Helpful instructions for users
- Immediate feedback when permissions granted

### Main Screen Bottom Navigation
- Icons for each tab:
  - Recent: List/History icon
  - Dev: Settings/Developer icon
- Selected tab highlighting
- Smooth transitions between tabs
- Top app bar title updates with selected tab

### Recent Tab (Future)
- Empty state design ready
- Placeholder text: "Recent transactions will appear here"
- Prepared for future list implementation

---

## Testing Checklist

- [ ] App launches to Permission Screen when permissions missing
- [ ] App launches directly to Main Screen when all permissions granted
- [ ] Granting permissions navigates to Main Screen automatically
- [ ] Revoking permissions navigates back to Permission Screen
- [ ] Bottom navigation switches between tabs correctly
- [ ] Dev tab Start/Stop service functionality works
- [ ] Back button behaves correctly:
  - [ ] Dev tab → Recent tab
  - [ ] Recent tab → Exit app
  - [ ] Permission screen → Exit app
- [ ] Service state persists across tab switches
- [ ] Screen rotation preserves state
- [ ] Deep links work correctly (if applicable)

---

## Future Considerations

1. **Recent Tab Implementation:**
   - Transaction list UI
   - Database integration for transaction storage
   - Transaction detail view

2. **Additional Tabs:**
   - Analytics/Statistics tab
   - Settings tab
   - Account/Profile tab

3. **ViewModel Integration:**
   - Move state management to ViewModels
   - Use shared ViewModel for cross-tab data

4. **Navigation Enhancements:**
   - Deep link support
   - Argument passing between screens
   - Nested navigation graphs for complex features

---

## Risk Assessment

### Low Risk
- UI extraction and reorganization
- Navigation setup
- Service control functionality migration

### Medium Risk
- Permission state management across navigation
- Back button behavior consistency
- Lifecycle state preservation

### Mitigation Strategies
- Thorough testing of permission state transitions
- Clear separation of concerns between screens
- Comprehensive logging for debugging navigation issues

---

## Success Criteria

1. ✅ Permission request screen shown only when needed
2. ✅ Automatic navigation based on permission state
3. ✅ Bottom navigation with 2 functional tabs
4. ✅ Dev tab preserves all original functionality
5. ✅ Recent tab placeholder ready for future development
6. ✅ Clean, maintainable code structure
7. ✅ Smooth user experience with intuitive navigation
