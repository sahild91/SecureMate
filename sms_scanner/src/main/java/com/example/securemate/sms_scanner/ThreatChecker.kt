package com.example.securemate.sms_scanner

object ThreatChecker {

    private val suspiciousKeywords = listOf(
        "login", "verify", "reset", "unlock", "confirm", "update", "password",
        "secure", "account", "urgent", "alert", "important", "gift", "win", "free"
    )

    private val suspiciousTlds = listOf(
        ".tk", ".ml", ".ga", ".cf", ".gq", ".xyz", ".top", ".loan", ".support", ".click"
    )

    private val ipRegex = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b""") // raw IP
    private val portRegex = Regex(""":[0-9]{2,5}""") // e.g., :8080
    private val encodedCharsRegex = Regex("""%[0-9A-Fa-f]{2}""") // URL encoded
    private val redirectPatterns = listOf("redirect=", "redir=", "url=", "goto=")
    private val unicodeDomainRegex = Regex("""xn--[a-z0-9\-]+""") // IDN homograph attacks
    private val hexIpRegex = Regex("""0x[0-9A-Fa-f]+""") // e.g., 0xC0A80001 (hex IP)

    fun checkThreat(url: String): Pair<Boolean, String?> {
        val lower = url.lowercase()

        return when {
            suspiciousKeywords.any { lower.contains(it) } -> true to "Contains phishing keywords"
            suspiciousTlds.any { lower.contains(it) } -> true to "Uses suspicious domain (.tk, etc.)"
            ipRegex.containsMatchIn(url) -> true to "Uses raw IP instead of domain"
            portRegex.containsMatchIn(url) -> true to "Uses non-standard port"
            encodedCharsRegex.containsMatchIn(url) -> true to "URL contains obfuscated characters"
            redirectPatterns.any { lower.contains(it) } -> true to "Potential redirect trap"
            unicodeDomainRegex.containsMatchIn(url) -> true to "Unicode domain detected"
            hexIpRegex.containsMatchIn(url) -> true to "Hex-encoded IP address"
            url.length > 150 -> true to "URL is too long"
            else -> false to null
        }
    }
}