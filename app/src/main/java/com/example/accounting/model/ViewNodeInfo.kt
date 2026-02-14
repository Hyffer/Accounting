package com.example.accounting.model

import android.graphics.Rect

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
