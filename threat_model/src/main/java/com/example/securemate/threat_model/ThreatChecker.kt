package com.example.securemate.threat_model

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

    private const val maxSafeUrlLength = 150

    fun checkThreat(url: String): ThreatResult {
        val lower = url.lowercase()

        return when {
            suspiciousTlds.any { lower.contains(it) } ->
                ThreatResult(true, ThreatLevel.HIGH, "Uses suspicious domain (.tk, etc.)")

            suspiciousKeywords.any { lower.contains(it) } && ipRegex.containsMatchIn(lower) ->
                ThreatResult(true, ThreatLevel.HIGH, "Phishing keywords + raw IP address")

            suspiciousKeywords.any { lower.contains(it) } ->
                ThreatResult(true, ThreatLevel.MEDIUM, "Contains phishing-related keywords")

            ipRegex.containsMatchIn(lower) ->
                ThreatResult(true, ThreatLevel.LOW, "Uses raw IP instead of domain")

            portRegex.containsMatchIn(lower) ->
                ThreatResult(true, ThreatLevel.LOW, "Uses non-standard port")

            encodedCharsRegex.containsMatchIn(lower) ->
                ThreatResult(true, ThreatLevel.LOW, "URL contains obfuscated characters")

            redirectPatterns.any { lower.contains(it) } ->
                ThreatResult(true, ThreatLevel.LOW, "Potential redirect trap")

            unicodeDomainRegex.containsMatchIn(lower) ->
                ThreatResult(true, ThreatLevel.MEDIUM, "Uses Unicode domain (possible spoofing)")

            hexIpRegex.containsMatchIn(lower) ->
                ThreatResult(true, ThreatLevel.MEDIUM, "Hex-encoded IP detected")

            url.length > maxSafeUrlLength ->
                ThreatResult(true, ThreatLevel.LOW, "URL is unusually long")

            else -> ThreatResult(false, ThreatLevel.LOW, "Safe")
        }
    }
}