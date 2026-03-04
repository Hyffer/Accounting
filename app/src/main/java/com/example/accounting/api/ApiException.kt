package com.example.accounting.api

sealed class ApiException(message: String) : Exception(message) {
    class ConfigMissingException :
        ApiException("Missing backend params, please configure them in settings")

    class ResponseException(code: Int) : ApiException("Unexpected response code: $code")
}