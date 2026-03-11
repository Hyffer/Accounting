package com.example.accounting.ui.viewmodel

import android.app.Application
import android.widget.Toast
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    private val transactionApi: TransactionApi
    val transactions: StateFlow<List<TransactionInfo>>

    // Uploading on the fly
    private val _uploadingIds = MutableStateFlow<Set<Long>>(emptySet())
    val uploadingIds: StateFlow<Set<Long>> = _uploadingIds.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database)
        transactionApi = TransactionApi(application)
        transactions = repository.getAllTransactions()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    fun updateTransaction(transaction: TransactionInfo) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionInfo) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun uploadThenDeleteTransaction(transaction: TransactionInfo) {
        // Atomically check and add
        var alreadyOnTheFly = false
        _uploadingIds.update { current ->
            if (transaction.id in current) {
                alreadyOnTheFly = true
                current
            } else {
                current + transaction.id
            }
        }
        if (alreadyOnTheFly) return

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                transactionApi.postTransaction(transaction)
            }

            _uploadingIds.update { it - transaction.id }

            result.fold(
                onSuccess = {
                    repository.deleteTransaction(transaction)   // deletion implement only checks primary key
                },
                onFailure = { error ->
                    Toast.makeText(getApplication(), error.toString(), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}