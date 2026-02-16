package com.example.accounting.handler

import com.example.accounting.model.PaymentViewType
import com.example.accounting.model.TransactionInfo
import com.example.accounting.model.ViewNodeInfo

object WeChatPayHandler : PaymentAppHandler {
    override val packageName = "com.tencent.mm"

    override fun detectPaymentView(rootNode: ViewNodeInfo): PaymentViewType {
        val allText = collectAllText(rootNode)

        val hasPaymentMethod = allText.any { it.contains("付款方式") }
        val hasPayButton = allText.any { it.contains("支付") }
        val hasSuccessText = allText.any { it.contains("支付成功") }
        val hasCompleteButton = allText.any { it.contains("完成") }
        val hasAmount = allText.any { PatternMatcher.containsAmount(it) }

        return when {
            hasSuccessText && hasCompleteButton && hasAmount -> PaymentViewType.Success
            hasPaymentMethod && hasPayButton && hasAmount -> PaymentViewType.Authentication
            else -> PaymentViewType.None
        }
    }

    override fun extractTransaction(rootNode: ViewNodeInfo, viewType: PaymentViewType): TransactionInfo? {
        val textNodes = collectTextNodes(rootNode)
        val amount = extractAmount(textNodes) ?: return null

        return TransactionInfo(
            amount = amount,
            merchant = null,
            paymentMethod = null,
            timestamp = System.currentTimeMillis(),
            viewType = viewType
        )
    }

    private fun collectAllText(node: ViewNodeInfo): List<String> {
        val result = mutableListOf<String>()

        fun traverse(currentNode: ViewNodeInfo) {
            currentNode.text?.let { result.add(it) }
            currentNode.children.forEach { traverse(it) }
        }

        traverse(node)
        return result
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
}
