package com.example.accounting.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "config")

object ConfigDataStore {
    object Keys {
        val URL = stringPreferencesKey("backend_url")
        val TOKEN = stringPreferencesKey("auth_token")
    }

    fun getUrl(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.URL]
        }
    }

    fun getToken(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.TOKEN]
        }
    }

    suspend fun setUrl(context: Context, url: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.URL] = url
        }
    }

    suspend fun setToken(context: Context, token: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.TOKEN] = token
        }
    }
}