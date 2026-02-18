package com.example.accounting.util

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.example.accounting.model.ViewNodeInfo

object ViewTreeParser {
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
        }

        // Calculate height and size
        val height = if (children.isEmpty()) {
            0
        } else {
            children.maxOf { it.height } + 1
        }

        val size = if (children.isEmpty()) {
            1
        } else {
            children.sumOf { it.size } + 1
        }

        return ViewNodeInfo(
            className = node.className?.toString(),
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            viewIdResourceName = node.viewIdResourceName,
            boundsInScreen = bounds,
            isClickable = node.isClickable,
            isEnabled = node.isEnabled,
            isFocusable = node.isFocusable,
            isScrollable = node.isScrollable,
            childCount = node.childCount,
            depth = depth,
            height = height,
            size = size,
            children = children
        )
    }

}
