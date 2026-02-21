# 自动记账

## 实现原理

通过 Android 无障碍服务读取屏幕显示内容，识别支付信息

识别的内容包括以下两类：
1. 付款确认页面：在主动支付（如扫码支付、网购支付、转账）时，显示金额等信息，请求用户确认、鉴权。被动支付（使用二维码结账）时没有这个过程。
2. 支付成功页面：通常也会显示金额等信息。

## 支付软件/支付方式

### 微信支付

微信支付在微信内转账、扫码支付、网购支付（从其它应用跳转到微信）等场景下行为表现一致。
除了发送微信红包、支付群收款时不会显示支付成功页面。

### 支付宝

支付宝扫码支付的行为表现与微信类似。

淘宝在使用支付宝付款时并不会跳转到支付宝，而是会显示与支付宝类似的付款确认页面，但支付成功页面与支付宝不同，且不显示金额。

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
