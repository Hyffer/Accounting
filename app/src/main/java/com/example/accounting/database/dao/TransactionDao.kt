package com.example.accounting.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.accounting.model.TransactionInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionInfo>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionInfo?

    @Insert
    suspend fun insertTransaction(transaction: TransactionInfo): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionInfo)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionInfo)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT * FROM transactions WHERE timestamp >= :minTimestamp ORDER BY timestamp DESC")
    suspend fun getTransactionsAfter(minTimestamp: Long): List<TransactionInfo>
}