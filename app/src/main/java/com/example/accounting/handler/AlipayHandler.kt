package com.example.accounting.handler

import com.example.accounting.model.PaymentViewType
import com.example.accounting.model.TransactionInfo
import com.example.accounting.model.ViewNodeInfo
import java.math.BigDecimal

object AlipayHandler : PaymentAppHandler {
    override val packageName = "com.eg.android.AlipayGphone"

    override fun detectPaymentView(rootNode: ViewNodeInfo): PaymentViewType {
        val hasPayAmount = ViewNodeUtils.hasText(rootNode, "支付金额")
        val hasConfirmButton = ViewNodeUtils.hasButtonWithText(rootNode, "确认付款")
        val hasSuccessText = ViewNodeUtils.hasText(rootNode, "支付成功")
        val hasCompleteButton = ViewNodeUtils.hasButtonWithText(rootNode, "完成")

        return when {
            hasSuccessText && hasCompleteButton -> PaymentViewType.Success
            hasPayAmount && hasConfirmButton -> PaymentViewType.Request
            else -> PaymentViewType.None
        }
    }

    override fun extractTransaction(
        rootNode: ViewNodeInfo,
        viewType: PaymentViewType
    ): TransactionInfo? {
        val textNodes = ViewNodeUtils.collectTextNodesWithContentDesc(rootNode)
        val amount = extractAmount(textNodes, viewType) ?: return null

        return TransactionInfo(
            amount = amount,
            merchant = null,
            paymentMethod = null,
            timestamp = System.currentTimeMillis(),
            viewType = viewType
        )
    }

    private fun extractAmount(
        textNodes: List<ViewNodeInfo>,
        viewType: PaymentViewType
    ): BigDecimal? {
        when (viewType) {
            is PaymentViewType.Request -> {
                for (node in textNodes) {
                    val contentDesc = node.contentDescription
                    if (contentDesc != null && contentDesc.contains("付款金额")) {
                        val regex = Regex("""付款金额([\d.]+)元""")
                        val match = regex.find(contentDesc)
                        if (match != null) {
                            val amountValue = match.groupValues[1]
                            return amountValue.toBigDecimalOrNull()
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
