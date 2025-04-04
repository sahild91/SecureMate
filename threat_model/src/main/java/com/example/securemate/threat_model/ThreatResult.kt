package com.example.securemate.threat_model

data class ThreatResult(
    val isThreat: Boolean,
    val level: ThreatLevel,
    val reason: String
)
