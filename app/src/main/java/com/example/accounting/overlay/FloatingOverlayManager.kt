package com.example.accounting.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.FrameLayout
import android.widget.TextView

class FloatingOverlayManager(
    private val context: Context,
    private val onInspectionClick: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var layoutParams: LayoutParams? = null

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (overlayView != null) return

        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        val floatingButton = createFloatingButton()

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var lastClickTime = 0L

        floatingButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams?.let { params ->
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingButton, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = kotlin.math.abs(event.rawX - initialTouchX)
                    val deltaY = kotlin.math.abs(event.rawY - initialTouchY)

                    // If movement is small, treat as click
                    if (deltaX < 10 && deltaY < 10) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime > 300) { // Debounce
                            lastClickTime = currentTime
                            onInspectionClick()
                        }
                    }
                    true
                }
                else -> false
            }
        }

        overlayView = floatingButton
        windowManager.addView(floatingButton, layoutParams)
    }

    fun hide() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    private fun createFloatingButton(): View {
        val size = dpToPx(64)
        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
        }

        val background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setSize(100, 100)
            setColor(Color.parseColor("#6200EE")) // Material primary color
        }

        val textView = TextView(context).apply {
            text = "V"
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
            isClickable = false
            isFocusable = false
        }.apply {
            this.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }

        container.background = background
        container.addView(textView)

        return container
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
