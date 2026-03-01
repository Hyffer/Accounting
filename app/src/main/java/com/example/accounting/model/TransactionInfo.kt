package com.example.accounting.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.accounting.database.converters.TransactionConverters
import java.math.BigDecimal

@Entity(tableName = "transactions")
@TypeConverters(TransactionConverters::class)
data class TransactionInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

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

/**
 * Checks if the transaction is a duplicate of another one.
 * Two transactions are considered duplicates if they have the same amount, merchant,
 * paymentMethod, and viewType (ignoring timestamp differences).
 */
fun TransactionInfo.isDuplicate(another: TransactionInfo): Boolean {
    return this.amount == another.amount &&
            this.merchant == another.merchant &&
            this.paymentMethod == another.paymentMethod &&
            this.viewType == another.viewType
}

/**
 * Checks if the Success transaction matches a Request transaction.
 * They match if they have the same amount, merchant, and paymentMethod.
 */
fun TransactionInfo.isMatchingRequest(requestTransaction: TransactionInfo): Boolean {
    return this.viewType == PaymentViewType.Success &&
            requestTransaction.viewType == PaymentViewType.Request &&
            this.amount == requestTransaction.amount &&
            this.merchant == requestTransaction.merchant &&
            this.paymentMethod == requestTransaction.paymentMethod
}
