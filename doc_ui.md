# Auto Accounting - UI Documentation

## Overview

This documentation illustrates UI design and user interaction.

---

## Architecture

### Component Structure

```
MainActivity (NavHost)
|-- PermissionScreen
|-- MainScreen (NavHost)
    |-- RecentScreen (NavHost)
    |   |-- TransactionList
    |   |-- EditTransactionScreen
    |-- ConfigScreen
    |-- DevScreen
```

### Navigation Architecture

The app uses **Jetpack Navigation Compose** with nested NavHost for hierarchical navigation. This enables direct navigation to any screen via URI deep links.

Route constants are defined at `ui/navigation/Routes.kt`.

**Deep Link URIs:**
```
app://com.example.accounting/recent/{transactionId}  -> Edit transaction with specific ID
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
                                    │             ├── Transaction List
                                    │             └── Transaction Edit Screen
                                    ├── Tab 1: Config Screen
                                    └── Tab 2: Dev Screen
```

---

## View Models

### 1. ConfigViewModel

**Responsibility:** Manage backend server configuration state.

**State:**
- `url: StateFlow<String>` - Backend server URL
- `token: StateFlow<String>` - Authentication token

**Actions:**
- `loadConfig()` - Load existing configuration from DataStore
- `saveConfig()` - Persist configuration to DataStore

---

### 2. TransactionViewModel

**Responsibility:** Manage transaction list and upload operations.

**State:**
- `transactions: StateFlow<List<TransactionInfo>>` - All transactions ordered by timestamp descending
- `uploadingIds: StateFlow<Set<Long>>` - IDs of transactions currently being uploaded

**Actions:**
- `updateTransaction(TransactionInfo)` - Update a transaction in the database
- `deleteTransaction(TransactionInfo)` - Delete a transaction from the database
- `uploadThenDeleteTransaction(TransactionInfo)` - Upload transaction to backend and delete on success

**Upload State Management:**
- Upload state is tracked in `uploadingIds` using a `Set<Long>`
- Thread-safe atomic updates using `MutableStateFlow.update { }`
- Prevents duplicate uploads for the same transaction
- Upload progress persists across navigation (ViewModel lifecycle)
- On upload failure, a toast is shown using application context

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

**Responsibility:** Host bottom navigation and navigate to selected tab.

**UI Diagram:**
```
+------------------------------------------+
|                                          |
|  +------------------------------------+  |
|  |                                    |  |
|  |      Tab Content Area              |  |
|  |   (Recent, Config, or Dev)         |  |
|  |                                    |  |
|  |                                    |  |
|  +------------------------------------+  |
|                                          |
|  +------------------------------------+  |
|  |  [Recent]  |  [Config]  |  [Dev]   |  |
|  +------------------------------------+  |
+------------------------------------------+
```

**Navigation Logic:**
- Host current tab content
- If permissions revoked, return to Permission Screen

---

### 4. Config Tab

**Responsibility:** Configure backend server connection parameters.

**UI Diagram:**
```
+------------------------------------------+
|                                          |
|  Card: Backend Configuration             |
|  +------------------------------------+  |
|  | Server URL                         |  |
|  | [________________________]         |  |
|  |                                    |  |
|  | Auth Token                         |  |
|  | [________________________]         |  |
|  |                                    |  |
|  |                    [Save Config]   |  |
|  +------------------------------------+  |
|                                          |
+------------------------------------------+
```

**Behavior:**
- Loads existing config on screen start
- Save button persists URL and TOKEN to DataStore

---

### 5. Recent Tab

**Responsibility:** Display and manage recent transactions with nested navigation for editing.

**Features:**
- Display transactions in a list ordered by timestamp descending, or a "No transactions yet" message
- Click transaction to edit
- Swipe left to delete transaction
- Swipe right to upload transaction to backend server

**Architecture:**
```
RecentScreen (NavHost)
|-- composable(recent/list)
|   |-- TransactionItem (Card with swipe gestures)
|       |-- SwipeToDismissBox
|           |-- backgroundContent (colored background with icon)
|           |-- content (transaction details)
|-- composable(recent/{transactionId}) (uri: "app://com.example.accounting/recent/{transactionId}")
    |-- EditTransactionScreen
```

**Transaction Item Layout:**
```
+------------------------------------------+
| ¥{amount}              {viewType} {time} |
| {merchant}               {paymentMethod} |
+------------------------------------------+
```

**UI Diagram:**
```
+------------------------------------------+
|         Recent Transactions              |
|                                          |
|  +------------------------------------+  |
|  | ¥25.00             Success  14:30  |  |
|  | Store Name            Credit card  |  |
|  +------------------------------------+  |
|                                          |
|  +------------------------------------+  |
|  | ¥100.00            Request  12:15  |  |
|  | Merchant                           |  |
|  +------------------------------------+  |
|                                          |
+------------------------------------------+
```

**Interaction:**
- Click: Navigates to EditTransactionScreen for editing the transaction
- Swipe left (EndToStart): Red background with delete icon, triggers deletion
- Swipe right (StartToEnd): Green background with check icon, triggers upload
    - On success: Transaction is deleted from local database
    - On failure: Error toast shown, item resets to settled position
    - While uploading: Item stays in swiped position, gestures disabled until operation completes

---

### 6. Edit Screen

**Responsibility:** Edit transaction properties.

**Features:**
- Editable fields: amount, optional divisor, merchant, payment method, timestamp, view type
- Amount divisor for splitting bills (final amount = amount / divisor)
- Appropriate input methods: numeric keyboard for amount, date and time picker for timestamp, SegmentedButton for view type
- Real-time validation with visual indication and error messages

**UI Diagram:**
```
+------------------------------------------+
|         Edit Transaction                 |
|                                          |
|  +------------------------------------+  |
|  | [Amount] ÷ [Divisor]               |  |
|  | Result: ¥{calculated}              |  |
|  +------------------------------------+  |
|                                          |
|  Merchant: [__________________________]  |
|                                          |
|  Payment Method: [____________________]  |
|                                          |
|  Timestamp: [2024-01-15 14:30] [📅][🕐]  |
|                                          |
|  View Type: [Success] | [Request]        |
|                                          |
|  [✓ Upload]        [🗑 Delete]           |
+------------------------------------------+
```

**Behavior:**
- Loads existing transaction values as defaults
- Navigation back: saves valid changes and returns to transaction list
- Upload button: saves changes, triggers upload and returns to transaction list
- Delete button: deletes transaction and returns to transaction list

---

### 7. Dev Tab

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
