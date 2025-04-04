package com.example.securemate.sms_scanner

object SmsParser {
    private val urlPattern = Regex("""https?://\S+""")

    fun extractLinks(text: String): List<String> {
        return urlPattern.findAll(text).map { it.value }.toList()
    }
}