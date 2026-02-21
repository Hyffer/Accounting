package com.example.accounting.handler

import com.example.accounting.model.PaymentViewType
import com.example.accounting.model.TransactionInfo
import com.example.accounting.model.ViewNodeInfo
import java.math.BigDecimal

object WeChatPayHandler : PaymentAppHandler {
    override val packageName = "com.tencent.mm"

    override fun detectPaymentView(rootNode: ViewNodeInfo): PaymentViewType {
        val hasPaymentMethod = ViewNodeUtils.hasText(rootNode, "付款方式")
        val hasPayButton = ViewNodeUtils.hasButtonWithText(rootNode, "支付")
        val hasSuccessText = ViewNodeUtils.hasText(rootNode, "支付成功")
        val hasCompleteButton = ViewNodeUtils.hasButtonWithText(rootNode, "完成")
        val hasAmount = ViewNodeUtils.collectAllText(rootNode).any { PatternMatcher.containsAmount(it) }

        return when {
            hasSuccessText && hasCompleteButton && hasAmount -> PaymentViewType.Success
            hasPaymentMethod && hasPayButton && hasAmount -> PaymentViewType.Request
            else -> PaymentViewType.None
        }
    }

    override fun extractTransaction(rootNode: ViewNodeInfo, viewType: PaymentViewType): TransactionInfo? {
        val textNodes = ViewNodeUtils.collectTextNodesWithContentDesc(rootNode)
        val amount = extractAmount(textNodes) ?: return null

        return TransactionInfo(
            amount = amount,
            merchant = null,
            paymentMethod = null,
            timestamp = System.currentTimeMillis(),
            viewType = viewType
        )
    }

    private fun extractAmount(textNodes: List<ViewNodeInfo>): BigDecimal? {
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
