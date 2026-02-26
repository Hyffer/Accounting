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
