package com.example.accounting.handler

object PaymentAppHandlerFactory {
    private val handlers = listOf(
        WeChatPayHandler,
        AlipayHandler
    )

    fun getHandler(packageName: String): PaymentAppHandler? {
        return handlers.find { it.packageName == packageName }
    }
}
