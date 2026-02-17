package com.example.accounting.handler

import com.example.accounting.model.PaymentViewType
import com.example.accounting.model.TransactionInfo
import com.example.accounting.model.ViewNodeInfo

object AlipayHandler : PaymentAppHandler {
    override val packageName = "com.eg.android.AlipayGphone"

    override fun detectPaymentView(rootNode: ViewNodeInfo): PaymentViewType {
        val allText = collectAllText(rootNode)

        val hasPayTo = allText.any { it.contains("付款给") }
        val hasConfirmButton = allText.any { it.contains("确认付款") }
        val hasSuccessText = allText.any { it.contains("支付成功") }
        val hasCompleteButton = allText.any { it.contains("完成") }

        return when {
            hasSuccessText && hasCompleteButton -> PaymentViewType.Success
            hasPayTo && hasConfirmButton -> PaymentViewType.Request
            else -> PaymentViewType.None
        }
    }

    override fun extractTransaction(rootNode: ViewNodeInfo, viewType: PaymentViewType): TransactionInfo? {
        val textNodes = collectTextNodesWithContentDesc(rootNode)
        val amount = extractAmount(textNodes, viewType) ?: return null

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

    private fun collectTextNodesWithContentDesc(node: ViewNodeInfo): List<ViewNodeInfo> {
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

    private fun extractAmount(textNodes: List<ViewNodeInfo>, viewType: PaymentViewType): String? {
        when (viewType) {
            is PaymentViewType.Request -> {
                for (node in textNodes) {
                    val contentDesc = node.contentDescription
                    if (contentDesc != null && contentDesc.contains("付款金额")) {
                        val regex = Regex("""付款金额([\d.]+)元""")
                        val match = regex.find(contentDesc)
                        if (match != null) {
                            val amountValue = match.groupValues[1]
                            return "￥$amountValue"
                        }
                    }
                }
            }
            is PaymentViewType.Success -> {
                val nodesWithAmount = textNodes.filter { node ->
                    val text = node.text ?: ""
                    PatternMatcher.containsAmount(text)
                }

                if (nodesWithAmount.isNotEmpty()) {
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
            else -> {}
        }
        return null
    }
}
