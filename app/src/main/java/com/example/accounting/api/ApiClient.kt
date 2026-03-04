package com.example.accounting.api

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ApiClient {
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build()
    }

    fun getClient(): OkHttpClient = okHttpClient
}