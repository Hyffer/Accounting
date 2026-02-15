# Implementation Plan: Payment Detection & Transaction Extraction (Phase 2 & 3)

## Overview

Implement automatic detection of WeChat payment views and extraction of transaction information. For testing purposes, display detected transactions via Android notifications instead of persisting to storage.

## Analyzed Payment View Patterns

### 1. Payment Authentication View
**Key Characteristics:**
- Package: `com.tencent.mm`
- Contains text "付款方式" (Payment Method)
- Contains merchant name (e.g., "硅基流动")
- Contains amount with currency symbol (e.g., "￥50.00")
- Contains "支付" button (Pay button)

### 2. Payment Success View
**Key Characteristics:**
- Package: `com.tencent.mm`
- Contains distinctive text "支付成功" (Payment Success)
- Contains same merchant name and amount
- Contains "完成" button (Complete button)

## Detection Strategy

### Robust Pattern Matching Principles

1. **Package Name Filtering**: Only process views from `com.tencent.mm`
2. **Feature-Based Detection**: Identify views by their semantic content, not resource IDs
3. **Multiple Indicators**: Use combination of text patterns to reduce false positives
4. **Tolerance for Change**: Avoid strict hierarchy dependencies

### Detection Rules

The rules described below are fuzzy, e.g. "button with text ..." might refer to a button with text as its child element.

#### Payment Authentication Detection
```
IF packageName == "com.tencent.mm"
   AND exists text containing "付款方式"
   AND exists text matching amount pattern "￥[\d,.]+"
   AND exists button with text "支付"
THEN -> Payment Authentication View
```

#### Payment Success Detection
```
IF packageName == "com.tencent.mm"
   AND exists text "支付成功"
   AND exists text matching amount pattern "￥[\d,.]+"
   AND exists button with text "完成"
THEN -> Payment Success View
```

## Architecture Design

### New Components

```
app/src/main/java/com/example/accounting/
|-- detector/
|   |-- PaymentViewDetector.kt           (Detects payment views from node tree)
|   |-- PatternMatcher.kt                (Text pattern matching utilities)
|-- extractor/
|   |-- TransactionExtractor.kt          (Extracts transaction info from views)
|   |-- AmountParser.kt                  (Parses currency amounts)
|-- model/
|   |-- TransactionInfo.kt               (Payment transaction data model)
|-- notification/
|   |-- TransactionNotificationManager.kt (Android notification builder)
```

### Data Flow

```
Accessibility Event (WindowContentChanged/WindowStateChanged)
  -> Filter by package name (com.tencent.mm)
  -> PaymentViewDetector.detect(rootNode)
  -> IF Payment Authentication View detected:
       -> TransactionExtractor.extractAuthInfo(rootNode)
       -> TransactionNotificationManager.showAuthNotification(info)
  -> IF Payment Success View detected:
       -> TransactionExtractor.extractSuccessInfo(rootNode)
       -> TransactionNotificationManager.showSuccessNotification(info)
```

## Implementation Details

### Phase 2: Payment Detection

#### 1. PatternMatcher.kt
Utility object for text pattern matching.

**Functions:**
- `containsAmount(text: String): Boolean` - Check if text contains currency amount pattern
- `extractAmount(text: String): String?` - Extract amount string from text
- `containsKeywords(text: String, keywords: List<String>): Boolean` - Check for keyword presence

**Amount Pattern:** `Regex("""[￥¥$][\d,]+\.?\d*""")`

#### 2. PaymentViewDetector.kt
Detects payment views by analyzing view node tree.

**Detection Method:**
- Traverse view tree to collect all text nodes
- Apply detection rules based on collected text patterns
- Return detection result with view type

**Functions:**
```kotlin
sealed class PaymentViewType {
    object Authentication : PaymentViewType()
    object Success : PaymentViewType()
    object None : PaymentViewType()
}

fun detectPaymentView(rootNode: ViewNodeInfo): PaymentViewType
```

**Implementation Strategy:**
- Flatten tree to extract all text content (ignore hierarchy)
- Check for indicator combinations
- Minimize false positives by requiring multiple matching criteria

### Phase 3: Transaction Extraction

#### 1. model/TransactionInfo.kt
Data model for transaction information.

```kotlin
data class TransactionInfo(
    val amount: String,              // e.g., "￥50.00"
    val merchant: String?,           // e.g., "硅基流动"
    val paymentMethod: String?,      // e.g., "零钱"
    val timestamp: Long,             // Detection time
    val viewType: PaymentViewType    // Authentication or Success
)
```

#### 2. AmountParser.kt
Parses and normalizes currency amounts.

**Functions:**
- `parseAmount(text: String): BigDecimal?` - Convert "￥50.00" to numeric value
- `normalizeAmount(text: String): String` - Standardize amount format

#### 3. TransactionExtractor.kt
Extracts transaction details from payment views.

**Extraction Strategy:**
1. **Amount**: Search for text matching amount pattern, prefer larger font (by bounds)
2. **Merchant Name**: Text before amount or text with specific patterns
3. **Payment Method**: Text after "付款方式" label

**Functions:**
```kotlin
fun extractTransaction(rootNode: ViewNodeInfo, viewType: PaymentViewType): TransactionInfo?
```

**Heuristics:**
- Amount is typically the largest text by bounds
- Merchant name appears before amount in view hierarchy
- Payment method appears with "付款方式" label

#### 4. TransactionNotificationManager.kt
Creates and displays Android notifications for detected transactions.

**Notification Types:**
- **Authentication Detected**: Show extracted info with "Waiting for confirmation" status
- **Success Detected**: Show "Payment successful" with full transaction details

**Implementation:**
- Use NotificationCompat for compatibility
- Create notification channel on Android O+
- Simple, text-based notifications (no decoration)

### Integration with Accessibility Service

#### Modify SelectToSpeakService.kt

Add event monitoring in `onAccessibilityEvent()`:

```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
        event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

        // Skip if not WeChat
        if (event.packageName != "com.tencent.mm") return

        // Get root node
        val rootNode = rootInActiveWindow ?: return

        // Detect payment view
        val viewType = PaymentViewDetector.detectPaymentView(rootNode)

        // Extract and notify if payment view detected
        if (viewType != PaymentViewType.None) {
            val transaction = TransactionExtractor.extractTransaction(rootNode, viewType)
            transaction?.let {
                TransactionNotificationManager.showNotification(this, it)
            }
        }

        rootNode.recycle()
    }
}
```

**Important:** Call `rootNode.recycle()` to prevent memory leaks.

### Configuration Updates

#### Update accessibility_service_config.xml

Add event types to monitor:
```xml
android:accessibilityEventTypes="typeWindowContentChanged|typeWindowStateChanged"
```

This ensures we receive events when:
- User navigates between activities
- View content updates (e.g., payment confirmation appears)

## Testing Strategy

### Manual Testing Scenarios

1. **Open WeChat**: No notification should appear
2. **Navigate to chat**: No notification
3. **Open payment authentication**: Notification should appear with extracted info
4. **Complete payment**: Success notification should appear
5. **Other WeChat features**: No false positives

### Edge Cases to Handle

1. **Multiple amounts on screen**: Choose the largest/prominent one
2. **Missing merchant name**: Use null, don't fail
3. **Different currency formats**: Handle "￥", "¥", "$" etc.
4. **Old WeChat versions**: Pattern matching should still work
5. **WeChat UI updates**: Feature-based detection minimizes breakage

## Implementation Order

### Step 1: Pattern Detection Foundation
1. Create `PatternMatcher.kt` with amount detection
2. Create `PaymentViewDetector.kt` with basic detection logic
3. Test detection with saved inspection JSON files

### Step 2: Transaction Extraction
1. Create `TransactionInfo.kt` data model
2. Create `AmountParser.kt` for amount handling
3. Create `TransactionExtractor.kt` with extraction logic
4. Test extraction with inspection files

### Step 3: Notification System
1. Create `TransactionNotificationManager.kt`
2. Add notification permission to manifest
3. Create notification channel
4. Test notification display

### Step 4: Integration
1. Modify `SelectToSpeakService.kt` to monitor events
2. Wire up detector -> extractor -> notification pipeline
3. Add duplicate event prevention (debounce)
4. Test end-to-end on device

### Step 5: Refinement
1. Add logging for debugging
2. Tune detection thresholds
3. Handle edge cases
4. Performance optimization

## Code Style Guidelines

1. **Keep it simple**: No unnecessary abstractions
2. **Basic UI**: Plain text notifications, no styling
3. **No decoration**: Functional code only, no visual polish
4. **Defensive coding**: Handle null/missing data gracefully
5. **Performance**: Minimize view tree traversals, recycle nodes

## Duplicate Detection Prevention

To avoid showing the same notification multiple times:

1. **Debounce by time**: Only notify once per 2 seconds per view type
2. **Track last transaction**: Store hash of (amount + merchant + viewType)
3. **Skip if same**: Don't notify if same transaction detected within debounce window

## Expected Output

When testing, user should see:

1. **On Payment Authentication Screen:**
   ```
   Notification: "检测到支付请求"
   Content: "商户: 硅基流动
             金额: ￥50.00
             付款方式: 零钱
             状态: 等待确认"
   ```

2. **On Payment Success Screen:**
   ```
   Notification: "支付成功"
   Content: "商户: 硅基流动
             金额: ￥50.00"
   ```

## File Structure After Implementation

```
app/src/main/java/com/example/accounting/
|-- MainActivity.kt
|-- model/
|   |-- ViewNodeInfo.kt
|   |-- InspectionResult.kt
|   |-- TransactionInfo.kt               [NEW]
|-- service/
|   |-- ViewInspectionAccessibilityService.kt
|-- detector/                            [NEW]
|   |-- PaymentViewDetector.kt
|   |-- PatternMatcher.kt
|-- extractor/                           [NEW]
|   |-- TransactionExtractor.kt
|   |-- AmountParser.kt
|-- notification/                        [NEW]
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
|-- SelectToSpeakService.kt              [MODIFY]
```

## Success Criteria

1. ✅ Payment authentication view is detected reliably
2. ✅ Payment success view is detected reliably
3. ✅ Other WeChat screens don't trigger false positives
4. ✅ Amount is extracted correctly
5. ✅ Merchant name is extracted (when present)
6. ✅ Payment method is extracted (when present)
7. ✅ Notifications display correctly
8. ✅ No crashes or memory leaks
9. ✅ Detection works quickly (< 1 second)
10. ✅ Code is simple and maintainable

## Future Considerations (Not in this phase)

- Support for Alipay detection
- Data persistence to local storage
- Cloud synchronization
- User confirmation UI
- Transaction history view
