package com.example.accounting.extractor

import com.example.accounting.detector.PatternMatcher
import com.example.accounting.model.PaymentViewType
import com.example.accounting.model.TransactionInfo
import com.example.accounting.model.ViewNodeInfo

object TransactionExtractor {

    fun extractTransaction(rootNode: ViewNodeInfo, viewType: PaymentViewType): TransactionInfo? {
        val textNodes = collectTextNodes(rootNode)

        val amount = extractAmount(textNodes) ?: return null
        val merchant = extractMerchant(textNodes)
        val paymentMethod = extractPaymentMethod(textNodes)

        return TransactionInfo(
            amount = amount,
            merchant = merchant,
            paymentMethod = paymentMethod,
            timestamp = System.currentTimeMillis(),
            viewType = viewType
        )
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

    private fun extractAmount(textNodes: List<ViewNodeInfo>): String? {
        val nodesWithAmount = textNodes.filter { node ->
            PatternMatcher.containsAmount(node.text ?: "")
        }

        if (nodesWithAmount.isEmpty()) return null

        val largestNode = nodesWithAmount.maxByOrNull { node ->
            val bounds = node.boundsInScreen
            if (bounds != null) {
                val width = bounds.right - bounds.left
                val height = bounds.bottom - bounds.top
                width * height
            } else {
                0
            }
        }

        return largestNode?.text?.let { PatternMatcher.extractAmount(it) }
    }

    private fun extractMerchant(textNodes: List<ViewNodeInfo>): String? {
        // future implementation
        return null
    }

    private fun extractPaymentMethod(textNodes: List<ViewNodeInfo>): String? {
        // future implementation
        return null
    }
}
