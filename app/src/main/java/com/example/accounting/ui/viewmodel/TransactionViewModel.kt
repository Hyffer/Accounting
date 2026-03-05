package com.example.accounting.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.accounting.api.TransactionApi
import com.example.accounting.database.AppDatabase
import com.example.accounting.model.TransactionInfo
import com.example.accounting.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    private val transactionApi: TransactionApi
    val transactions: StateFlow<List<TransactionInfo>>

    // Edit screen state
    private val _focusedTransaction = MutableStateFlow<TransactionInfo?>(null)
    val focusedTransaction: StateFlow<TransactionInfo?> = _focusedTransaction.asStateFlow()

    // Callback to trigger uploading from edit screen
    private var focusedUploadCallback: ((TransactionInfo) -> Unit)? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database)
        transactionApi = TransactionApi(application)
        transactions = repository.getAllTransactions()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    fun focusTransaction(transaction: TransactionInfo, onUpload: (TransactionInfo) -> Unit) {
        _focusedTransaction.value = transaction
        focusedUploadCallback = onUpload
    }

    fun defocusTransaction() {
        _focusedTransaction.value = null
        focusedUploadCallback = null
    }

    fun triggerFocusedUpload(transaction: TransactionInfo) {
        focusedUploadCallback?.invoke(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionInfo) {
        repository.updateTransaction(transaction)
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