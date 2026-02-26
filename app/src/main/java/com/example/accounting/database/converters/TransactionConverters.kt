package com.example.accounting.database.converters

import androidx.room.TypeConverter
import com.example.accounting.model.PaymentViewType
import java.math.BigDecimal

class TransactionConverters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal): String {
        return value.toString()
    }

    @TypeConverter
    fun toBigDecimal(value: String): BigDecimal {
        return BigDecimal(value)
    }

    @TypeConverter
    fun fromPaymentViewType(viewType: PaymentViewType): String {
        return when (viewType) {
            is PaymentViewType.Request -> "REQUEST"
            is PaymentViewType.Success -> "SUCCESS"
            is PaymentViewType.None -> "NONE"
        }
    }

    @TypeConverter
    fun toPaymentViewType(value: String): PaymentViewType {
        return when (value) {
            "REQUEST" -> PaymentViewType.Request
            "SUCCESS" -> PaymentViewType.Success
            "NONE" -> PaymentViewType.None
            else -> PaymentViewType.None
        }
    }
}