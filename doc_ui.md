# Auto Accounting - UI Documentation

## Overview

This documentation illustrates UI design and user interaction.

---

## Architecture

### Component Structure

```
AppNavigation
|-- PermissionScreen
|-- MainScreen
    |-- RecentScreen
    |-- DevScreen
```

### Navigation Flow Diagram

```
App Launch
    │
    ├── Missing Permissions ──> Permission Screen
    │                               │
    │                               │ (Permission granted)
    │                               v
    └── All Granted ──────────> Main Screen
                                    │
                                    ├── Tab 0: Recent Screen
                                    └── Tab 1: Dev Screen
```

---

## Detailed Component Design

### 1. MainActivity (Navigation Controller)

- Define navigation graph
- Set start destination based on permission state
- Handle navigation between screens

---

### 2. Permission Screen

**Responsibility:** Handle all permission requests and display status.

**UI Diagram:**
```
+------------------------------------------+
|         Permission Request               |
|                                          |
|  Card: Permission Status                 |
|  +------------------------------------+  |
|  | Overlay Permission    [Granted]    |  |
|  | Accessibility Permiss [Not Granted]|  |
|  +------------------------------------+  |
|                                          |
|  [Grant Overlay Permission]              |
|  [Grant Accessibility Permission]        |
|                                          |
|  Card: Instructions                      |
|  +------------------------------------+  |
|  | Why we need these permissions...   |  |
|  +------------------------------------+  |
+------------------------------------------+
```

**Behavior:**
- Automatically navigates to Main Screen once all permissions granted
- Monitors permission state on resume
- Cannot be navigated away from until permissions granted

---

### 3. Main Screen

**Responsibility:** Host bottom navigation and display selected tab content.

**UI Diagram:**
```
+------------------------------------------+
|                                          |
|  +------------------------------------+  |
|  |                                    |  |
|  |      Tab Content Area              |  |
|  |      (RecentTab or DevTab)         |  |
|  |                                    |  |
|  |                                    |  |
|  +------------------------------------+  |
|                                          |
|  +------------------------------------+  |
|  |    [Recent]      |     [Dev]       |  |
|  +------------------------------------+  |
+------------------------------------------+
```

**Navigation Logic:**
- Host current tab content
- Back button behavior:
    - If on Dev tab → go to Recent tab
    - If on Recent tab → exit app
    - If permissions revoked → return to Permission Screen

---

### 4. Recent Tab

Reserved for future transaction history feature

---

### 5. Dev Tab

**Responsibility:** Inspection service management.

- Start/Stop FloatingOverlayService
- Monitor service running state

**UI Diagram:**
```
+------------------------------------------+
|         View Inspector                   |
|                                          |
|  Card: Service Status                    |
|  +------------------------------------+  |
|  | Service is running                 |  |
|  +------------------------------------+  |
|                                          |
|  [Start Inspection]  [Stop Inspection]   |
|                                          |
|  Card: Instructions                      |
|  +------------------------------------+  |
|  | 1. Tap 'Start Inspection'          |  |
|  | 2. Navigate to app                 |  |
|  | 3. Tap floating button             |  |
|  | 4. Check saved files               |  |
|  +------------------------------------+  |
+------------------------------------------+
```
