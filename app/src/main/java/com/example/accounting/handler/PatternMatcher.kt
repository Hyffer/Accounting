package com.example.accounting.handler

import java.math.BigDecimal

object PatternMatcher {
    private val amountPattern = Regex("""[￥¥$]([\d,]+\.?\d*)|([\d,]+点[\d\s]+)元""")

    fun containsAmount(text: String): Boolean {
        return amountPattern.containsMatchIn(text)
    }

    fun extractAmount(text: String): BigDecimal? {
        val match = amountPattern.find(text) ?: return null
        val amountStr = (match.groupValues[1].ifEmpty { match.groupValues[2] })
            .replace(",", "")
            .replace("点", ".").replace(" ", "")
        return amountStr.toBigDecimalOrNull()
    }

    fun containsKeywords(text: String, keywords: List<String>): Boolean {
        return keywords.any { keyword -> text.contains(keyword) }
    }
}