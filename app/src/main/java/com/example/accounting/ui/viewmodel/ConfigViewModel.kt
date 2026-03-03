package com.example.accounting.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.accounting.datastore.ConfigDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConfigViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application

    private val _url = MutableStateFlow("")
    private val _token = MutableStateFlow("")
    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)

    val url: StateFlow<String> = _url.asStateFlow()
    val token: StateFlow<String> = _token.asStateFlow()
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    init {
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _url.value = ConfigDataStore.getUrl(context).first() ?: ""
            _token.value = ConfigDataStore.getToken(context).first() ?: ""
        }
    }

    fun updateUrl(newUrl: String) {
        _url.value = newUrl
    }

    fun updateToken(newToken: String) {
        _token.value = newToken
    }

    fun saveConfig() {
        viewModelScope.launch {
            try {
                ConfigDataStore.setUrl(context, _url.value)
                ConfigDataStore.setToken(context, _token.value)
                _saveStatus.value = SaveStatus.Success
            } catch (e: Exception) {
                _saveStatus.value = SaveStatus.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetSaveStatus() {
        _saveStatus.value = SaveStatus.Idle
    }
}

sealed class SaveStatus {
    object Idle : SaveStatus()
    object Success : SaveStatus()
    data class Error(val message: String) : SaveStatus()
}