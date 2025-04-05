package com.example.securemate.wifi_scanner

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Core WiFi scanner implementation that provides WiFi network information
 * and security assessment.
 */
class WifiScanner(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val mainHandler = Handler(Looper.getMainLooper())

    // Using LiveData instead of StateFlow
    private val _currentWifiInfo = MutableLiveData<WifiNetworkInfo?>(null)
    val currentWifiInfo: LiveData<WifiNetworkInfo?> = _currentWifiInfo

    private val _securityAssessment = MutableLiveData<SecurityAssessment?>(null)
    val securityAssessment: LiveData<SecurityAssessment?> = _securityAssessment

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * Start monitoring WiFi network changes
     */
    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateWifiInfo()
            }

            override fun onLost(network: Network) {
                _currentWifiInfo.postValue(null)
                _securityAssessment.postValue(null)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                updateWifiInfo()
            }
        }

        networkCallback?.let {
            connectivityManager.registerNetworkCallback(request, it)
        }

        // Perform initial update
        updateWifiInfo()
    }

    /**
     * Stop monitoring WiFi network changes
     */
    fun stopMonitoring() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }

    /**
     * Update current WiFi information
     */
    private fun updateWifiInfo() {
        val wifiInfo = getCurrentWifiInfo()
        _currentWifiInfo.postValue(wifiInfo)

        wifiInfo?.let {
            _securityAssessment.postValue(assessSecurity(it))
        }
    }

    /**
     * Get current WiFi connection information
     */
    private fun getCurrentWifiInfo(): WifiNetworkInfo? {
        val network = connectivityManager.activeNetwork ?: return null
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return null

        if (!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return null
        }

        val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            networkCapabilities.transportInfo as? WifiInfo
        } else {
            @Suppress("DEPRECATION")
            wifiManager.connectionInfo
        }

        wifiInfo ?: return null

        val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wifiInfo.ssid?.removeSurrounding("\"") ?: "Unknown"
        } else {
            @Suppress("DEPRECATION")
            wifiInfo.ssid?.removeSurrounding("\"") ?: "Unknown"
        }

        val bssid = wifiInfo.bssid ?: "Unknown"
        val rssi = wifiInfo.rssi
        val linkSpeed = wifiInfo.linkSpeed
        val frequency = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            wifiInfo.frequency
        } else {
            0
        }
        val ipAddress = getIpAddress(wifiInfo)
        val macAddress = getMacAddress(wifiInfo)
        val isHidden = ssid == "<unknown ssid>" || ssid == "Unknown"
        val band = if (frequency > 5000) "5GHz" else "2.4GHz"

        return WifiNetworkInfo(
            ssid = ssid,
            bssid = bssid,
            rssi = rssi,
            linkSpeed = linkSpeed,
            frequency = frequency,
            ipAddress = ipAddress,
            macAddress = macAddress,
            isHidden = isHidden,
            band = band
        )
    }

    /**
     * Get IP address from WifiInfo
     */
    private fun getIpAddress(wifiInfo: WifiInfo): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+, we need to get the IP address from the network interface
            try {
                val networkInterfaces = java.net.NetworkInterface.getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val networkInterface = networkInterfaces.nextElement()
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address.hostAddress?.contains(':') == false) {
                            return address.hostAddress ?: "Unknown"
                        }
                    }
                }
                "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
        } else {
            @Suppress("DEPRECATION")
            val ipInt = wifiInfo.ipAddress
            String.format(
                "%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )
        }
    }

    /**
     * Get MAC address from WifiInfo
     */
    private fun getMacAddress(wifiInfo: WifiInfo): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+, we need to get the MAC address from the network interface
            try {
                val networkInterfaces = java.net.NetworkInterface.getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val networkInterface = networkInterfaces.nextElement()
                    if (networkInterface.name.startsWith("wlan")) {
                        val macBytes = networkInterface.hardwareAddress ?: return "Unknown"
                        val macBuilder = StringBuilder()
                        for (b in macBytes) {
                            macBuilder.append(String.format("%02X:", b))
                        }
                        if (macBuilder.isNotEmpty()) {
                            macBuilder.deleteCharAt(macBuilder.length - 1)
                        }
                        return macBuilder.toString()
                    }
                }
                "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
        } else {
            @Suppress("DEPRECATION")
            wifiInfo.macAddress ?: "Unknown"
        }
    }

    /**
     * Assess WiFi security based on network information
     */
    private fun assessSecurity(wifiInfo: WifiNetworkInfo): SecurityAssessment {
        // Default to MEDIUM until we have more details
        var level = SecurityLevel.MEDIUM
        val issues = mutableListOf<SecurityIssue>()
        val recommendations = mutableListOf<String>()

        // Check for hidden network (not a strong security measure)
        if (wifiInfo.isHidden) {
            issues.add(
                SecurityIssue(
                    "Hidden Network",
                    "Your network SSID is hidden, which provides minimal security benefit but may cause connection issues."
                )
            )
            recommendations.add("Consider showing your SSID as hiding it doesn't significantly improve security.")
        }

        // Check for weak signal strength
        if (wifiInfo.rssi < -70) {
            issues.add(
                SecurityIssue(
                    "Weak Signal",
                    "Your WiFi signal is weak, which can lead to interrupted connections and slower speeds."
                )
            )
            recommendations.add("Move closer to your WiFi router or consider using a WiFi extender.")
        }

        // Check for 2.4GHz vs 5GHz (5GHz is generally faster and less congested)
        if (wifiInfo.band == "2.4GHz") {
            issues.add(
                SecurityIssue(
                    "Using 2.4GHz Band",
                    "The 2.4GHz band is more congested and generally slower than 5GHz."
                )
            )
            recommendations.add("If your router supports it, connect to the 5GHz network for better speed and less interference.")
        }

        // Default security rating (future: detect actual security type like WPA2/WPA3)
        // This would require additional permissions and capabilities

        // For now, let's simulate security assessment with a placeholder
        // In a real implementation, we'd need to check the actual security type
        val simulatedSecurity = if (wifiInfo.ssid.contains("Guest", ignoreCase = true)) {
            "Open" // Assume guest networks are less secure
        } else {
            "WPA2" // Assume most networks use WPA2
        }

        when (simulatedSecurity) {
            "Open" -> {
                level = SecurityLevel.LOW
                issues.add(
                    SecurityIssue(
                        "Open Network",
                        "Your network appears to have no encryption, making it vulnerable to eavesdropping."
                    )
                )
                recommendations.add("Switch to a network with WPA2 or WPA3 encryption immediately.")
            }
            "WEP" -> {
                level = SecurityLevel.LOW
                issues.add(
                    SecurityIssue(
                        "WEP Encryption",
                        "WEP encryption is deprecated and can be cracked in minutes."
                    )
                )
                recommendations.add("Update your router to use WPA2 or WPA3 encryption.")
            }
            "WPA" -> {
                level = SecurityLevel.MEDIUM
                issues.add(
                    SecurityIssue(
                        "WPA Encryption",
                        "WPA is outdated and has known vulnerabilities."
                    )
                )
                recommendations.add("Update your router to use WPA2 or WPA3 encryption.")
            }
            "WPA2" -> {
                level = SecurityLevel.HIGH
                // No issues for WPA2, but can recommend WPA3 if available
                recommendations.add("Consider upgrading to WPA3 if your router supports it for enhanced security.")
            }
            "WPA3" -> {
                level = SecurityLevel.VERY_HIGH
                // No issues for WPA3
            }
        }

        return SecurityAssessment(
            level = level,
            issues = issues,
            recommendations = recommendations
        )
    }

    /**
     * Detect if the user is connected through a VPN
     */
    fun isVpnConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    /**
     * Get the signal quality as a percentage
     */
    fun getSignalQualityPercentage(): Int {
        val rssi = _currentWifiInfo.value?.rssi ?: return 0
        // RSSI typically ranges from -100 dBm (weak) to -50 dBm (strong)
        val MIN_RSSI = -100
        val MAX_RSSI = -50

        return when {
            rssi <= MIN_RSSI -> 0
            rssi >= MAX_RSSI -> 100
            else -> ((rssi - MIN_RSSI) * 100) / (MAX_RSSI - MIN_RSSI)
        }
    }
}

/**
 * WiFi network information data class
 */
data class WifiNetworkInfo(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val linkSpeed: Int,
    val frequency: Int,
    val ipAddress: String,
    val macAddress: String,
    val isHidden: Boolean,
    val band: String
)

/**
 * Security assessment data class
 */
data class SecurityAssessment(
    val level: SecurityLevel,
    val issues: List<SecurityIssue>,
    val recommendations: List<String>
)

/**
 * Security issue data class
 */
data class SecurityIssue(
    val title: String,
    val description: String
)

/**
 * Security levels enum
 */
enum class SecurityLevel {
    VERY_LOW,
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
}