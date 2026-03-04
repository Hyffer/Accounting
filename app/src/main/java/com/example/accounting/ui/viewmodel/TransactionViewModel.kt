package com.example.accounting.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.accounting.api.TransactionApi
import com.example.accounting.database.AppDatabase
import com.example.accounting.model.TransactionInfo
import com.example.accounting.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    private val transactionApi: TransactionApi
    val transactions: StateFlow<List<TransactionInfo>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database)
        transactionApi = TransactionApi(application)
        transactions = repository.getAllTransactions()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    fun deleteTransaction(transaction: TransactionInfo) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    suspend fun uploadTransaction(transaction: TransactionInfo): Result<Unit> {
        return withContext(Dispatchers.IO) {
            transactionApi.postTransaction(transaction)
        }
    }
}