package com.example.securemate.wifi_scanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.securemate.wifi_scanner.NetworkDevice
import com.example.securemate.wifi_scanner.NetworkDeviceScanner
import com.example.securemate.wifi_scanner.NetworkSpeedTester
import com.example.securemate.wifi_scanner.ScanState
import com.example.securemate.wifi_scanner.SecurityAssessment
import com.example.securemate.wifi_scanner.SpeedTestResult
import com.example.securemate.wifi_scanner.SpeedTestState
import com.example.securemate.wifi_scanner.WifiNetworkInfo
import com.example.securemate.wifi_scanner.WifiScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for WiFi scanner functionality
 */
class WifiScannerViewModel(application: Application) : AndroidViewModel(application) {

    // Core components
    private val wifiScanner = WifiScanner(application)
    private val speedTester = NetworkSpeedTester(application)
    private val deviceScanner = NetworkDeviceScanner(application)

    // State flows for UI
    val currentWifiInfo: LiveData<WifiNetworkInfo?> = wifiScanner.currentWifiInfo
    val securityAssessment: LiveData<SecurityAssessment?> = wifiScanner.securityAssessment
    val speedTestState: LiveData<SpeedTestState> = speedTester.speedTestState
    val deviceScanState: StateFlow<ScanState> = deviceScanner.scanState
    val discoveredDevices: StateFlow<List<NetworkDevice>> = deviceScanner.discoveredDevices

    // Combined state for overall WiFi health
    private val _isVpnActive = MutableStateFlow(false)

    // Convert LiveData to StateFlow for combining
    private val _securityAssessmentFlow = MutableStateFlow<SecurityAssessment?>(null)

    val overallNetworkHealth = combine(
        _securityAssessmentFlow,
        _isVpnActive,
        deviceScanState
    ) { security, vpnActive, scanState ->
        calculateOverallHealth(security, vpnActive, scanState)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NetworkHealthStatus.UNKNOWN
    )

    init {
        // Start monitoring WiFi
        wifiScanner.startMonitoring()

        // Check VPN status periodically
        checkVpnStatus()

        // Observe LiveData and update StateFlow
        viewModelScope.launch {
            securityAssessment.observeForever { assessment ->
                _securityAssessmentFlow.value = assessment
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        wifiScanner.stopMonitoring()
    }

    /**
     * Run a network speed test
     */
    fun runSpeedTest() {
        viewModelScope.launch {
            speedTester.runSpeedTest()
            // No need to update a StateFlow since we're observing the LiveData directly
        }
    }

    /**
     * Get speed test history
     */
    fun getSpeedTestHistory(): List<SpeedTestResult> {
        return speedTester.getSpeedTestHistory()
    }

    /**
     * Get average speeds from history
     */
    fun getAverageSpeedFromHistory(): Pair<Double, Double> {
        return speedTester.getAverageSpeedFromHistory()
    }

    /**
     * Scan for devices on the network
     */
    fun scanForDevices() {
        viewModelScope.launch {
            deviceScanner.scanDevices()
        }
    }

    /**
     * Mark a device as trusted
     */
    fun setDeviceTrust(macAddress: String, trusted: Boolean) {
        deviceScanner.setDeviceTrust(macAddress, trusted)
    }

    /**
     * Set a custom name for a device
     */
    fun setDeviceName(macAddress: String, name: String) {
        deviceScanner.setDeviceName(macAddress, name)
    }

    /**
     * Get WiFi signal quality percentage
     */
    fun getSignalQualityPercentage(): Int {
        return wifiScanner.getSignalQualityPercentage()
    }

    /**
     * Check if VPN is active
     */
    private fun checkVpnStatus() {
        viewModelScope.launch {
            val isVpnActive = wifiScanner.isVpnConnected()
            _isVpnActive.value = isVpnActive
        }
    }

    /**
     * Calculate overall network health status
     */
    private fun calculateOverallHealth(
        securityAssessment: SecurityAssessment?,
        isVpnActive: Boolean,
        scanState: ScanState
    ): NetworkHealthStatus {
        if (securityAssessment == null) {
            return NetworkHealthStatus.UNKNOWN
        }

        // Determine based on security level
        val securityStatus = when (securityAssessment.level) {
            com.example.securemate.wifi_scanner.SecurityLevel.VERY_LOW -> 0
            com.example.securemate.wifi_scanner.SecurityLevel.LOW -> 1
            com.example.securemate.wifi_scanner.SecurityLevel.MEDIUM -> 2
            com.example.securemate.wifi_scanner.SecurityLevel.HIGH -> 3
            com.example.securemate.wifi_scanner.SecurityLevel.VERY_HIGH -> 4
        }

        // Boost score if VPN is active
        val vpnBoost = if (isVpnActive) 1 else 0

        // Penalize if there are untrusted devices (only if we've completed a scan)
        val untrustedPenalty = if (scanState is ScanState.Complete) {
            val untrustedDevices = discoveredDevices.value.count { !it.isTrusted && !it.isLocalDevice }
            if (untrustedDevices > 2) -2 else if (untrustedDevices > 0) -1 else 0
        } else {
            0
        }

        // Calculate final score (0-4)
        val finalScore = (securityStatus + vpnBoost + untrustedPenalty).coerceIn(0, 4)

        return when (finalScore) {
            0 -> NetworkHealthStatus.CRITICAL
            1 -> NetworkHealthStatus.POOR
            2 -> NetworkHealthStatus.FAIR
            3 -> NetworkHealthStatus.GOOD
            4 -> NetworkHealthStatus.EXCELLENT
            else -> NetworkHealthStatus.UNKNOWN
        }
    }
}

/**
 * Network health status enum
 */
enum class NetworkHealthStatus {
    UNKNOWN,
    CRITICAL,
    POOR,
    FAIR,
    GOOD,
    EXCELLENT;

    /**
     * Get color for status
     */
    fun getColorHex(): String {
        return when (this) {
            UNKNOWN -> "#9E9E9E"  // Gray
            CRITICAL -> "#F44336" // Red
            POOR -> "#FF9800"     // Orange
            FAIR -> "#FFC107"     // Amber
            GOOD -> "#4CAF50"     // Green
            EXCELLENT -> "#2196F3" // Blue
        }
    }

    /**
     * Get readable description
     */
    fun getDescription(): String {
        return when (this) {
            UNKNOWN -> "Not available"
            CRITICAL -> "Critical security issues detected"
            POOR -> "Security concerns present"
            FAIR -> "Acceptable security, some improvements needed"
            GOOD -> "Good security profile"
            EXCELLENT -> "Excellent security and performance"
        }
    }
}