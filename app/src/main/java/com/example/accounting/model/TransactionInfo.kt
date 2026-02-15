package com.example.accounting.model

data class TransactionInfo(
    val amount: String,
    val merchant: String?,
    val paymentMethod: String?,
    val timestamp: Long,
    val viewType: PaymentViewType
)

sealed class PaymentViewType {
    object Authentication : PaymentViewType()
    object Success : PaymentViewType()
    object None : PaymentViewType()
}
