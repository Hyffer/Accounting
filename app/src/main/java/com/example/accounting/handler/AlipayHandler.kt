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
        val keyword: String = when (viewType) {
            is PaymentViewType.Request -> "付款金额"
            is PaymentViewType.Success -> "支付成功"
            else -> return null
        }
        for (node in textNodes) {
            val contentDesc = node.contentDescription
            if (contentDesc != null && contentDesc.contains(keyword)) {
                val regex = Regex("""$keyword\s*[¥￥]?\s*([\d.]+)\s*元?""")
                val match = regex.find(contentDesc)
                if (match != null) {
                    val amountValue = match.groupValues[1]
                    return amountValue.toBigDecimalOrNull()
                }
            }
        }
        return null
    }
}
