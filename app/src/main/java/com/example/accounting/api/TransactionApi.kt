package com.example.accounting.api

import android.content.Context
import androidx.core.net.toUri
import com.example.accounting.datastore.ConfigDataStore
import com.example.accounting.model.TransactionInfo
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class TransactionPayload(
    val amount: Double,
    val merchant: String?,
    val payment_method: String?,
    val occurred_at: String
)

class TransactionApi(private val context: Context) {

    private val gson = Gson()

    /**
     * Posts a transaction to the backend server.
     * @throws ApiException.ConfigMissingException if URL or TOKEN is not configured
     * @throws ApiException.ResponseException if server returns non-201 response
     * @throws Exception
     */
    suspend fun postTransaction(transaction: TransactionInfo): Result<Unit> {
        return try {
            val baseUrl = ConfigDataStore.getUrl(context).first()
            val token = ConfigDataStore.getToken(context).first()

            if (baseUrl.isNullOrBlank() || token.isNullOrBlank()) {
                return Result.failure(ApiException.ConfigMissingException())
            }

            val url = baseUrl.toUri().buildUpon()
                .appendPath("transaction_info")
                .build().toString()
            val requestBody = formatTransactionForApi(transaction)
            val jsonBody = gson.toJson(requestBody)

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = ApiClient.getClient().newCall(request).execute()

            if (response.code == 201) {
                Result.success(Unit)
            } else {
                Result.failure(ApiException.ResponseException(response.code))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatTransactionForApi(transaction: TransactionInfo): TransactionPayload {
        val instant = Instant.ofEpochMilli(transaction.timestamp)
        val iso8601Timestamp = DateTimeFormatter.ISO_INSTANT
            .withZone(ZoneOffset.UTC)
            .format(instant)

        return TransactionPayload(
            amount = transaction.amount.toDouble(),
            merchant = transaction.merchant,
            payment_method = transaction.paymentMethod,
            occurred_at = iso8601Timestamp
        )
    }
}