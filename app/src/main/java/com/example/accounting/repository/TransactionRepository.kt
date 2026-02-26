package com.example.accounting.repository

import com.example.accounting.database.AppDatabase
import com.example.accounting.model.TransactionInfo
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val database: AppDatabase) {
    fun getAllTransactions(): Flow<List<TransactionInfo>> {
        return database.transactionDao().getAllTransactions()
    }

    suspend fun insertTransaction(transaction: TransactionInfo): Long {
        return database.transactionDao().insertTransaction(transaction)
    }

    suspend fun getTransactionById(id: Long): TransactionInfo? {
        return database.transactionDao().getTransactionById(id)
    }

    suspend fun updateTransaction(transaction: TransactionInfo) {
        database.transactionDao().updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionInfo) {
        database.transactionDao().deleteTransaction(transaction)
    }

    suspend fun deleteAllTransactions() {
        database.transactionDao().deleteAllTransactions()
    }
}