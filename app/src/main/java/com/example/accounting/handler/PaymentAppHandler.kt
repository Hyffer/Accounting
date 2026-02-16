package com.example.accounting.handler

import com.example.accounting.model.PaymentViewType
import com.example.accounting.model.TransactionInfo
import com.example.accounting.model.ViewNodeInfo

interface PaymentAppHandler {
    val packageName: String
    fun detectPaymentView(rootNode: ViewNodeInfo): PaymentViewType
    fun extractTransaction(rootNode: ViewNodeInfo, viewType: PaymentViewType): TransactionInfo?
}
