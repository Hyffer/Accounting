package com.example.accounting.model

import java.math.BigDecimal

data class TransactionInfo(
    val amount: BigDecimal,
    val merchant: String?,
    val paymentMethod: String?,
    val timestamp: Long,
    val viewType: PaymentViewType
)

sealed class PaymentViewType {
    object Request : PaymentViewType()
    object Success : PaymentViewType()
    object None : PaymentViewType()
}
