package com.example.accounting.handler

import com.example.accounting.model.ViewNodeInfo

/**
 * Utility object for common view node operations shared across payment handlers.
 */
object ViewNodeUtils {

    /**
     * Collects all text content from the view hierarchy.
     */
    fun collectAllText(node: ViewNodeInfo): List<String> {
        val result = mutableListOf<String>()

        fun traverse(currentNode: ViewNodeInfo) {
            currentNode.text?.let { result.add(it) }
            currentNode.children.forEach { traverse(it) }
        }

        traverse(node)
        return result
    }

    /**
     * Collects all text nodes (nodes that have text or contentDescription).
     */
    fun collectTextNodesWithContentDesc(node: ViewNodeInfo): List<ViewNodeInfo> {
        val result = mutableListOf<ViewNodeInfo>()

        fun traverse(currentNode: ViewNodeInfo) {
            if (!currentNode.text.isNullOrEmpty() || !currentNode.contentDescription.isNullOrEmpty()) {
                result.add(currentNode)
            }
            currentNode.children.forEach { traverse(it) }
        }

        traverse(node)
        return result
    }

    /**
     * Collects all button nodes (button class or small clickable element).
     * Returned elements do not overlap with each other.
     */
    fun collectButtonNodes(node: ViewNodeInfo): List<ViewNodeInfo> {
        val result = mutableListOf<ViewNodeInfo>()

        fun traverse(currentNode: ViewNodeInfo) {
            if (currentNode.className?.contains("button", ignoreCase = true) == true ||
                (currentNode.isClickable && currentNode.size < 5)
            ) {
                result.add(currentNode)
            } else {
                currentNode.children.forEach { traverse(it) }
            }
        }

        traverse(node)
        return result
    }

    /**
     * Checks if there's a clickable button containing the specified text.
     */
    fun hasButtonWithText(node: ViewNodeInfo, searchText: String): Boolean {
        val buttons = collectButtonNodes(node)

        return buttons.any { hasText(it, searchText) }
    }

    /**
     * Checks if there's any node in the hierarchy containing the specified text.
     * This is a simple text search without button validation.
     */
    fun hasText(node: ViewNodeInfo, searchText: String): Boolean {
        fun traverse(currentNode: ViewNodeInfo): Boolean {
            if (currentNode.text?.contains(searchText) == true ||
                currentNode.contentDescription?.contains(searchText) == true
            ) {
                return true
            }
            return currentNode.children.any { traverse(it) }
        }

        return traverse(node)
    }
}
