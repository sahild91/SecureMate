package com.example.securemate.wifi_scanner

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Network device scanner to detect devices on the same network
 */
class NetworkDeviceScanner(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState

    private val _discoveredDevices = MutableStateFlow<List<NetworkDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<NetworkDevice>> = _discoveredDevices

    private val trustedDevicesPrefs = context.getSharedPreferences("trusted_devices", Context.MODE_PRIVATE)

    /**
     * Scan for devices on the network
     */
    suspend fun scanDevices() = withContext(Dispatchers.IO) {
        try {
            _scanState.value = ScanState.Scanning(0)
            val devices = mutableListOf<NetworkDevice>()

            // Get local device IP address
            val myIp = getLocalIpAddress()
            if (myIp.isNullOrEmpty()) {
                _scanState.value = ScanState.Error("Could not determine local IP address")
                return@withContext
            }

            // Get subnet mask and calculate network range
            val subnetMask = getSubnetMask()

            // Extract base address for scanning
            val baseIp = getBaseIpAddress(myIp, subnetMask)
            if (baseIp.isNullOrEmpty()) {
                _scanState.value = ScanState.Error("Could not determine network base address")
                return@withContext
            }

            // Create our device record
            val localDevice = NetworkDevice(
                ipAddress = myIp,
                macAddress = getLocalMacAddress() ?: "Unknown",
                deviceName = "This Device",
                isLocalDevice = true,
                isTrusted = true,
                vendorName = "Unknown",
                firstSeen = System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis()
            )
            devices.add(localDevice)

            // Read ARP table first for efficiency
            val arpResults = readArpTable()
            for (entry in arpResults) {
                if (!entry.ipAddress.equals(myIp, ignoreCase = true)) {
                    val isTrusted = isTrustedDevice(entry.macAddress)
                    val device = NetworkDevice(
                        ipAddress = entry.ipAddress,
                        macAddress = entry.macAddress,
                        deviceName = getDeviceNameFromMac(entry.macAddress) ?: "Unknown Device",
                        isLocalDevice = false,
                        isTrusted = isTrusted,
                        vendorName = guessVendorFromMac(entry.macAddress),
                        firstSeen = getTrustedDeviceFirstSeen(entry.macAddress) ?: System.currentTimeMillis(),
                        lastSeen = System.currentTimeMillis()
                    )
                    devices.add(device)
                }
            }

            // Now scan IP addresses to find devices not in ARP table
            // For this demo, we'll scan the first 20 addresses to keep it quick
            // In a real app, you'd scan the entire subnet
            val ipPrefix = baseIp.substring(0, baseIp.lastIndexOf('.') + 1)
            for (i in 1..20) {
                val progress = (i * 100) / 20
                _scanState.value = ScanState.Scanning(progress)

                val testIp = "$ipPrefix$i"
                if (testIp == myIp) continue // Skip our own IP

                // Skip IPs we already found in ARP table
                if (devices.any { it.ipAddress == testIp }) continue

                val reachable = isReachable(testIp)
                if (reachable) {
                    // Look up MAC address for this IP
                    val macAddress = getMacFromArpTable(testIp) ?: "Unknown"
                    val isTrusted = isTrustedDevice(macAddress)

                    val device = NetworkDevice(
                        ipAddress = testIp,
                        macAddress = macAddress,
                        deviceName = getDeviceNameFromMac(macAddress) ?: "Unknown Device",
                        isLocalDevice = false,
                        isTrusted = isTrusted,
                        vendorName = guessVendorFromMac(macAddress),
                        firstSeen = getTrustedDeviceFirstSeen(macAddress) ?: System.currentTimeMillis(),
                        lastSeen = System.currentTimeMillis()
                    )
                    devices.add(device)
                }
            }

            // Update stored devices
            for (device in devices) {
                if (!device.isLocalDevice) {
                    updateDeviceLastSeen(device.macAddress)
                }
            }

            _discoveredDevices.value = devices
            _scanState.value = ScanState.Complete(devices.size)
        } catch (e: Exception) {
            Log.e("NetworkDeviceScanner", "Error scanning devices", e)
            _scanState.value = ScanState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Mark a device as trusted
     */
    fun setDeviceTrust(macAddress: String, trusted: Boolean) {
        trustedDevicesPrefs.edit()
            .putBoolean("trusted_$macAddress", trusted)
            .apply()

        // Update our current list
        val currentDevices = _discoveredDevices.value.toMutableList()
        val index = currentDevices.indexOfFirst { it.macAddress == macAddress }
        if (index != -1) {
            currentDevices[index] = currentDevices[index].copy(isTrusted = trusted)
            _discoveredDevices.value = currentDevices
        }
    }

    /**
     * Check if a device is trusted
     */
    private fun isTrustedDevice(macAddress: String): Boolean {
        return trustedDevicesPrefs.getBoolean("trusted_$macAddress", false)
    }

    /**
     * Get the first time a device was seen
     */
    private fun getTrustedDeviceFirstSeen(macAddress: String): Long? {
        val time = trustedDevicesPrefs.getLong("first_seen_$macAddress", 0)
        return if (time > 0) time else null
    }

    /**
     * Update the last seen time for a device
     */
    private fun updateDeviceLastSeen(macAddress: String) {
        // Create a "first seen" entry if it doesn't exist
        if (!trustedDevicesPrefs.contains("first_seen_$macAddress")) {
            trustedDevicesPrefs.edit()
                .putLong("first_seen_$macAddress", System.currentTimeMillis())
                .apply()
        }

        // Update last seen time
        trustedDevicesPrefs.edit()
            .putLong("last_seen_$macAddress", System.currentTimeMillis())
            .apply()
    }

    /**
     * Get the saved name for a device, if any
     */
    private fun getDeviceNameFromMac(macAddress: String): String? {
        return trustedDevicesPrefs.getString("name_$macAddress", null)
    }

    /**
     * Set a custom name for a device
     */
    fun setDeviceName(macAddress: String, name: String) {
        trustedDevicesPrefs.edit()
            .putString("name_$macAddress", name)
            .apply()

        // Update our current list
        val currentDevices = _discoveredDevices.value.toMutableList()
        val index = currentDevices.indexOfFirst { it.macAddress == macAddress }
        if (index != -1) {
            currentDevices[index] = currentDevices[index].copy(deviceName = name)
            _discoveredDevices.value = currentDevices
        }
    }

    /**
     * Get local IP address
     */
    private fun getLocalIpAddress(): String? {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.hostAddress?.contains(':') == false) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkDeviceScanner", "Error getting local IP", e)
        }
        return null
    }

    /**
     * Get subnet mask (non-deprecated approach)
     */
    private fun getSubnetMask(): String {
        // Default to Class C
        var subnetMask = "255.255.255.0"

        try {
            // For newer Android versions, we can't easily get the subnet mask directly
            // Instead, we'll assume a standard class C subnet mask for most home/office networks
            // In a production app, you'd want to implement a more robust solution

            // For a more accurate approach, you could use DHCP client information or
            // implement a platform-dependent solution
        } catch (e: Exception) {
            Log.e("NetworkDeviceScanner", "Error getting subnet mask", e)
        }

        return subnetMask
    }

    /**
     * Get local MAC address (non-deprecated approach)
     */
    private fun getLocalMacAddress(): String? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // On Android 12 and higher, we can't get the real MAC address
                // for privacy reasons, so we'll have to use the network interfaces

                val networkInterfaces = NetworkInterface.getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val networkInterface = networkInterfaces.nextElement()
                    if (networkInterface.name.equals("wlan0", ignoreCase = true)) {
                        val hardwareAddress = networkInterface.hardwareAddress
                        if (hardwareAddress != null) {
                            val stringBuilder = StringBuilder()
                            for (b in hardwareAddress) {
                                stringBuilder.append(String.format("%02X:", b))
                            }
                            if (stringBuilder.isNotEmpty()) {
                                stringBuilder.deleteCharAt(stringBuilder.length - 1)
                            }
                            return stringBuilder.toString()
                        }
                    }
                }
            } else {
                // On Android 11 and below, we can try to use the NetworkCallback API
                val currentNetwork = connectivityManager.activeNetwork
                if (currentNetwork != null) {
                    val wifiInfo = getCurrentWifiInfo()
                    if (wifiInfo != null) {
                        val macAddress = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            // Android 11+ cannot access MAC address directly
                            // We'd need to use NetworkInterface approach
                            getMacFromNetworkInterface()
                        } else {
                            @Suppress("DEPRECATION")
                            wifiInfo.macAddress
                        }

                        if (macAddress != null && macAddress != "02:00:00:00:00:00") {
                            return macAddress
                        }
                    }
                }

                // Fallback to network interface method
                return getMacFromNetworkInterface()
            }
        } catch (e: Exception) {
            Log.e("NetworkDeviceScanner", "Error getting MAC address", e)
        }
        return null
    }

    /**
     * Get MAC address from network interface
     */
    private fun getMacFromNetworkInterface(): String? {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                if (networkInterface.name.equals("wlan0", ignoreCase = true)) {
                    val hardwareAddress = networkInterface.hardwareAddress
                    if (hardwareAddress != null) {
                        val stringBuilder = StringBuilder()
                        for (b in hardwareAddress) {
                            stringBuilder.append(String.format("%02X:", b))
                        }
                        if (stringBuilder.isNotEmpty()) {
                            stringBuilder.deleteCharAt(stringBuilder.length - 1)
                        }
                        return stringBuilder.toString()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkDeviceScanner", "Error getting MAC from network interface", e)
        }
        return null
    }

    /**
     * Get current WifiInfo in a non-deprecated way
     */
    private fun getCurrentWifiInfo(): WifiInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+
            val currentNetwork = connectivityManager.activeNetwork ?: return null
            val capabilities = connectivityManager.getNetworkCapabilities(currentNetwork) ?: return null
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+
                    // getTransportInfo() doesn't take parameters - it returns the TransportInfo for the current network
                    val transportInfo = capabilities.transportInfo
                    transportInfo as? WifiInfo
                } else {
                    // Android 10-11
                    @Suppress("DEPRECATION")
                    wifiManager.connectionInfo
                }
            } else null
        } else {
            // Below Android 10
            @Suppress("DEPRECATION")
            wifiManager.connectionInfo
        }
    }

    /**
     * Convert integer IP address to string format
     */
    private fun intToIp(i: Int): String {
        return ((i and 0xFF).toString() + "." +
                (i shr 8 and 0xFF) + "." +
                (i shr 16 and 0xFF) + "." +
                (i shr 24 and 0xFF))
    }

    /**
     * Get base IP address for network
     */
    private fun getBaseIpAddress(ipAddress: String, subnetMask: String): String? {
        try {
            val ipParts = ipAddress.split(".").map { it.toInt() }
            val maskParts = subnetMask.split(".").map { it.toInt() }

            if (ipParts.size != 4 || maskParts.size != 4) return null

            val baseIpParts = ipParts.zip(maskParts) { ip, mask -> ip and mask }
            return baseIpParts.joinToString(".")
        } catch (e: Exception) {
            Log.e("NetworkDeviceScanner", "Error calculating base IP", e)
        }
        return null
    }

    /**
     * Check if an IP address is reachable
     */
    private fun isReachable(ipAddress: String): Boolean {
        return try {
            val address = InetAddress.getByName(ipAddress)
            address.isReachable(1000) // 1 second timeout
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Read ARP table for existing devices
     */
    private fun readArpTable(): List<ArpEntry> {
        val entries = mutableListOf<ArpEntry>()

        try {
            val reader = BufferedReader(FileReader("/proc/net/arp"))
            // Skip header line
            reader.readLine()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    val parts = it.split("\\s+".toRegex()).filter { part -> part.isNotEmpty() }
                    if (parts.size >= 4) {
                        val ip = parts[0]
                        val mac = parts[3]

                        // Filter out invalid entries
                        if (mac != "00:00:00:00:00:00" && !ip.startsWith("0.0.0.0")) {
                            entries.add(ArpEntry(ip, mac))
                        }
                    }
                }
            }
            reader.close()
        } catch (e: IOException) {
            Log.e("NetworkDeviceScanner", "Error reading ARP table", e)
        }

        return entries
    }

    /**
     * Get MAC address from ARP table for a specific IP
     */
    private fun getMacFromArpTable(ipAddress: String): String? {
        val arpTable = readArpTable()
        return arpTable.find { it.ipAddress == ipAddress }?.macAddress
    }

    /**
     * Guess vendor name from MAC address OUI (first 6 characters)
     * In a real app, this would use a comprehensive OUI database
     */
    private fun guessVendorFromMac(macAddress: String): String {
        if (macAddress.isEmpty() || macAddress == "Unknown") return "Unknown"

        val oui = macAddress.replace(":", "").take(6).uppercase()

        // Very small sample of common vendors
        return when (oui) {
            "FCFBFB", "FCFC48", "B07FB9" -> "Samsung"
            "000C29", "000569", "001C14" -> "VMware"
            "F0B4D2", "B8782E", "D023DB" -> "Apple"
            "001A11", "286ABA", "E4F004" -> "Google"
            "306023", "609AC1", "8CEA48" -> "Xiaomi"
            "00DBDF", "9CD643", "E0B9E5" -> "Amazon"
            "1CAFF7", "58A0CB", "C8DDC9" -> "Lenovo"
            "3C9872", "485D60", "701F53" -> "Huawei"
            "5C2E59", "90F052", "BCF685" -> "OnePlus"
            "00217C", "001438", "000777" -> "D-Link"
            "0030BD", "000FE2", "002272" -> "Belkin"
            "00234E", "00096E", "001018" -> "Netgear"
            "F85F2A", "F81A67", "F8AB05" -> "Cisco"
            "A0BB3E", "B8D94D", "FCAD0F" -> "Sony"
            "001D7E", "001963", "001599" -> "Hewlett-Packard"
            "000AFA", "001562", "000F5F" -> "Microsoft"
            "001C25", "C81451", "68DBCA" -> "Intel"
            else -> "Unknown"
        }
    }
}

/**
 * Network device data class
 */
data class NetworkDevice(
    val ipAddress: String,
    val macAddress: String,
    val deviceName: String,
    val isLocalDevice: Boolean,
    val isTrusted: Boolean,
    val vendorName: String,
    val firstSeen: Long,
    val lastSeen: Long
)

/**
 * ARP table entry
 */
data class ArpEntry(
    val ipAddress: String,
    val macAddress: String
)

/**
 * Scan state sealed class
 */
sealed class ScanState {
    object Idle : ScanState()
    data class Scanning(val progressPercent: Int) : ScanState()
    data class Complete(val devicesFound: Int) : ScanState()
    data class Error(val message: String) : ScanState()
}