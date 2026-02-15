package com.example.accounting.detector

import com.example.accounting.model.PaymentViewType
import com.example.accounting.model.ViewNodeInfo

object PaymentViewDetector {

    fun detectPaymentView(rootNode: ViewNodeInfo): PaymentViewType {
        val textNodes = collectTextNodes(rootNode)
        val allText = textNodes.mapNotNull { it.text }

        val hasPaymentMethod = allText.any { it.contains("付款方式") }
        val hasAmount = allText.any { PatternMatcher.containsAmount(it) }
        val hasPayButton = allText.any { it.contains("支付") }
        val hasSuccessText = allText.any { it.contains("支付成功") }
        val hasCompleteButton = allText.any { it.contains("完成") }

        return when {
            hasSuccessText && hasAmount && hasCompleteButton -> PaymentViewType.Success
            hasPaymentMethod && hasAmount && hasPayButton -> PaymentViewType.Authentication
            else -> PaymentViewType.None
        }
    }

    private fun collectTextNodes(node: ViewNodeInfo): List<ViewNodeInfo> {
        val result = mutableListOf<ViewNodeInfo>()

        fun traverse(currentNode: ViewNodeInfo) {
            if (!currentNode.text.isNullOrEmpty()) {
                result.add(currentNode)
            }
            currentNode.children.forEach { traverse(it) }
        }

        traverse(node)
        return result
    }
}
