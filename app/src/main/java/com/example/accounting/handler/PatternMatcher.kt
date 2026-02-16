package com.example.accounting.handler

object PatternMatcher {
    private val amountPattern = Regex("""[￥¥$][\d,]+\.?\d*""")

    fun containsAmount(text: String): Boolean {
        return amountPattern.containsMatchIn(text)
    }

    fun extractAmount(text: String): String? {
        return amountPattern.find(text)?.value
    }

    fun containsKeywords(text: String, keywords: List<String>): Boolean {
        return keywords.any { keyword -> text.contains(keyword) }
    }
}