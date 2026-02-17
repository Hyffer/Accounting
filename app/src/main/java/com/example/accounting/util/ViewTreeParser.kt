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
            children = children
        )
    }

    fun countViews(node: ViewNodeInfo?): Int {
        if (node == null) return 0
        return 1 + node.children.sumOf { countViews(it) }
    }
}
