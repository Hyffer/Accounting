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

/**
 * Transaction processing state.
 * State transition:
 * - Upload: Idle -> Uploading
 * - Upload success: Uploading -> Deleting
 * - Upload fail: Uploading -> Failed
 * - Clear: Failed -> Idle
 * - Delete: Idle -> Deleting
 */
enum class TransactionState {
    Idle,
    Uploading,
    Failed,
    Deleting
}

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    private val transactionApi: TransactionApi
    val transactions: StateFlow<List<TransactionInfo>>

    private val _transactionStates = MutableStateFlow<Map<Long, TransactionState>>(emptyMap())
    val transactionStates: StateFlow<Map<Long, TransactionState>> = _transactionStates.asStateFlow()

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

    fun insertTransaction(transaction: TransactionInfo, onInserted: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.insertTransaction(transaction)
            onInserted(id)
        }
    }

    fun deleteTransaction(transaction: TransactionInfo) {
        var allowDirectDelete = false
        _transactionStates.update { current ->
            val state = current[transaction.id] ?: TransactionState.Idle
            if (state == TransactionState.Idle) {
                allowDirectDelete = true
                current + (transaction.id to TransactionState.Deleting)
            } else {
                current
            }
        }
        if (!allowDirectDelete) return

        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            // TransactionState.Deleting not removed,
            // to get rid of figuring out which of `transactions` and `transactionStates` updates first.
        }
    }

    fun uploadThenDeleteTransaction(transaction: TransactionInfo) {
        // Atomically check and update
        var allowUpload = false
        _transactionStates.update { current ->
            val state = current[transaction.id] ?: TransactionState.Idle
            if (state == TransactionState.Idle) {
                allowUpload = true
                current + (transaction.id to TransactionState.Uploading)
            } else {
                current
            }
        }
        if (!allowUpload) return

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                transactionApi.postTransaction(transaction)
            }

            result.fold(
                onSuccess = {
                    _transactionStates.update { it + (transaction.id to TransactionState.Deleting) }
                    repository.deleteTransaction(transaction)   // deletion implement only checks primary key
                },
                onFailure = { error ->
                    _transactionStates.update { it + (transaction.id to TransactionState.Failed) }
                    Toast.makeText(getApplication(), error.toString(), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    fun clearTransactionState(transactionId: Long) {
        _transactionStates.update { it - transactionId }
    }
}