package com.example.accounting.model

import android.graphics.Rect

data class ViewNodeInfo(
    // properties from AccessibilityNodeInfo
    val className: String?,
    val text: String?,
    val contentDescription: String?,
    val viewIdResourceName: String?,
    val boundsInScreen: Rect?,
    val isClickable: Boolean,
    val isEnabled: Boolean,
    val isFocusable: Boolean,
    val isScrollable: Boolean,
    val childCount: Int,    // direct child count

    // other helpful properties
    val depth: Int,         // depth in view hierarchy (root node is 0)
    val height: Int,        // height of subtree (leaf node is 0)
    val size: Int,          // total node count of subtree (leaf node is 1)

    val children: List<ViewNodeInfo>
)
