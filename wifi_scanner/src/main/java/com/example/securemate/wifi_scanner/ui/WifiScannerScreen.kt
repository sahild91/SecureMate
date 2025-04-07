package com.example.securemate.wifi_scanner.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.securemate.wifi_scanner.NetworkDevice
import com.example.securemate.wifi_scanner.ScanState
import com.example.securemate.wifi_scanner.SecurityAssessment
import com.example.securemate.wifi_scanner.SecurityIssue
import com.example.securemate.wifi_scanner.SecurityLevel
import com.example.securemate.wifi_scanner.SpeedTestState
import com.example.securemate.wifi_scanner.WifiNetworkInfo
import com.example.securemate.wifi_scanner.viewmodel.NetworkHealthStatus
import com.example.securemate.wifi_scanner.viewmodel.WifiScannerViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.livedata.observeAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScannerScreen(
    navController: NavController,
    viewModel: WifiScannerViewModel = viewModel()
) {
    // State collection
    val deviceScanState by viewModel.deviceScanState.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val networkHealth by viewModel.overallNetworkHealth.collectAsStateWithLifecycle()

    // LiveData observation
    val currentWifiInfo by viewModel.currentWifiInfo.observeAsState()
    val securityAssessment by viewModel.securityAssessment.observeAsState()
    val speedTestState by viewModel.speedTestState.observeAsState()

    // Local UI state
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Overview", "Security", "Devices")
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WiFi Scanner") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "WiFi Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Network info card
            NetworkInfoCard(
                wifiInfo = currentWifiInfo,
                healthStatus = networkHealth,
                signalQuality = viewModel.getSignalQualityPercentage()
            )

            // Tab row
            TabRow(
                selectedTabIndex = selectedTab
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> OverviewTab(
                    wifiInfo = currentWifiInfo,
                    securityAssessment = securityAssessment,
                    speedTestState = speedTestState,
                    onRunSpeedTest = { viewModel.runSpeedTest() }
                )
                1 -> SecurityTab(
                    securityAssessment = securityAssessment,
                    healthStatus = networkHealth
                )
                2 -> DevicesTab(
                    scanState = deviceScanState,
                    devices = discoveredDevices,
                    onScanDevices = { viewModel.scanForDevices() },
                    onSetDeviceTrust = { mac, trusted -> viewModel.setDeviceTrust(mac, trusted) },
                    onSetDeviceName = { mac, name -> viewModel.setDeviceName(mac, name) }
                )
            }
        }
    }
}

@Composable
fun NetworkInfoCard(
    wifiInfo: WifiNetworkInfo?,
    healthStatus: NetworkHealthStatus,
    signalQuality: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (wifiInfo != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )

                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = wifiInfo.ssid,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${wifiInfo.band} · ",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            // Signal quality indicator
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width((signalQuality * 36 / 100).dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                signalQuality > 70 -> Color.Green
                                                signalQuality > 40 -> Color(0xFFFFA000) // Amber
                                                else -> Color.Red
                                            }
                                        )
                                )
                            }

                            Text(
                                text = " $signalQuality%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Health status indicator
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(healthStatus.getColorHex())))
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (healthStatus) {
                                NetworkHealthStatus.EXCELLENT -> "A"
                                NetworkHealthStatus.GOOD -> "B"
                                NetworkHealthStatus.FAIR -> "C"
                                NetworkHealthStatus.POOR -> "D"
                                NetworkHealthStatus.CRITICAL -> "F"
                                NetworkHealthStatus.UNKNOWN -> "?"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(36.dp)
                    )

                    Text(
                        text = "Not connected to WiFi",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OverviewTab(
    wifiInfo: WifiNetworkInfo?,
    securityAssessment: SecurityAssessment?,
    speedTestState: SpeedTestState?,
    onRunSpeedTest: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "WiFi Details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (wifiInfo != null) {
                DetailItem("SSID", wifiInfo.ssid)
                DetailItem("MAC Address", wifiInfo.macAddress)
                DetailItem("IP Address", wifiInfo.ipAddress)
                DetailItem("Frequency", "${wifiInfo.frequency} MHz (${wifiInfo.band})")
                DetailItem("Link Speed", "${wifiInfo.linkSpeed} Mbps")
                DetailItem("Signal Strength", "${wifiInfo.rssi} dBm")
                if (wifiInfo.isHidden) {
                    DetailItem("Hidden Network", "Yes")
                }
            } else {
                Text(
                    text = "Not connected to WiFi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Speed Test",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onRunSpeedTest,
                    enabled = wifiInfo != null && speedTestState !is SpeedTestState.TESTING
                ) {
                    Text(if (speedTestState is SpeedTestState.TESTING) "Testing..." else "Run Test")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (speedTestState) {
                SpeedTestState.IDLE -> {
                    Text("Idle", style = MaterialTheme.typography.bodyMedium)
                }

                is SpeedTestState.TESTING -> {
                    Text("Testing...", style = MaterialTheme.typography.bodyMedium)
                }

                is SpeedTestState.COMPLETE -> {
                    Text("Speed Test Complete", style = MaterialTheme.typography.bodyMedium)
                }

                is SpeedTestState.ERROR -> {
                    Text("Error occurred during speed test", style = MaterialTheme.typography.bodyMedium)
                }

                null -> {
                    Text("Unknown speed test state", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Security Summary",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (securityAssessment != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = when (securityAssessment.level) {
                            SecurityLevel.VERY_HIGH, SecurityLevel.HIGH -> Icons.Default.Security
                            SecurityLevel.MEDIUM -> Icons.Default.Shield
                            SecurityLevel.LOW, SecurityLevel.VERY_LOW -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint = when (securityAssessment.level) {
                            SecurityLevel.VERY_HIGH, SecurityLevel.HIGH -> Color.Green
                            SecurityLevel.MEDIUM -> Color(0xFFFFA000) // Amber
                            SecurityLevel.LOW, SecurityLevel.VERY_LOW -> Color.Red
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp)
                    )

                    Text(
                        text = when (securityAssessment.level) {
                            SecurityLevel.VERY_HIGH -> "Very High Security"
                            SecurityLevel.HIGH -> "High Security"
                            SecurityLevel.MEDIUM -> "Medium Security"
                            SecurityLevel.LOW -> "Low Security"
                            SecurityLevel.VERY_LOW -> "Very Low Security"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (securityAssessment.issues.isNotEmpty()) {
                    Text(
                        text = "${securityAssessment.issues.size} issue(s) detected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "No issues detected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Green,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                Text(
                    text = "Not connected to WiFi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun SecurityTab(
    securityAssessment: SecurityAssessment?,
    healthStatus: NetworkHealthStatus
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Security Assessment",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(healthStatus.getColorHex())))
                        .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (healthStatus) {
                            NetworkHealthStatus.EXCELLENT -> "A"
                            NetworkHealthStatus.GOOD -> "B"
                            NetworkHealthStatus.FAIR -> "C"
                            NetworkHealthStatus.POOR -> "D"
                            NetworkHealthStatus.CRITICAL -> "F"
                            NetworkHealthStatus.UNKNOWN -> "?"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                }
            }

            Text(
                text = healthStatus.getDescription(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                color = Color(android.graphics.Color.parseColor(healthStatus.getColorHex()))
            )

            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (securityAssessment != null) {
            if (securityAssessment.issues.isNotEmpty()) {
                item {
                    Text(
                        text = "Security Issues",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                    )
                }

                items(securityAssessment.issues) { issue ->
                    SecurityIssueItem(issue)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (securityAssessment.recommendations.isNotEmpty()) {
                item {
                    Text(
                        text = "Recommendations",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(securityAssessment.recommendations) { recommendation ->
                    RecommendationItem(recommendation)
                }
            }
        } else {
            item {
                Text(
                    text = "Not connected to WiFi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun DevicesTab(
    scanState: ScanState,
    devices: List<NetworkDevice>,
    onScanDevices: () -> Unit,
    onSetDeviceTrust: (String, Boolean) -> Unit,
    onSetDeviceName: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scan button and status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Network Devices",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onScanDevices,
                enabled = scanState !is ScanState.Scanning
            ) {
                Text(if (scanState is ScanState.Scanning) "Scanning..." else "Scan")
            }
        }

        // Scan status
        when (scanState) {
            is ScanState.Idle -> {
                Text(
                    text = "Start a scan to detect devices on your network",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            is ScanState.Scanning -> {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Scanning network devices...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = { scanState.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
            is ScanState.Complete -> {
                Text(
                    text = "Found ${scanState.devicesFound} device(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            is ScanState.Error -> {
                Text(
                    text = "Error: ${scanState.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Device list
        if (devices.isNotEmpty()) {
            LazyColumn {
                items(devices) { device ->
                    DeviceItem(
                        device = device,
                        onSetTrust = { trusted -> onSetDeviceTrust(device.macAddress, trusted) },
                        onRename = { name -> onSetDeviceName(device.macAddress, name) }
                    )
                }
            }
        } else if (scanState !is ScanState.Scanning && scanState !is ScanState.Idle) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No devices found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun DetailItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SecurityIssueItem(issue: SecurityIssue) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = issue.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun RecommendationItem(recommendation: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
        )
        Text(
            text = recommendation,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun DeviceItem(
    device: NetworkDevice,
    onSetTrust: (Boolean) -> Unit,
    onRename: (String) -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var deviceName by remember { mutableStateOf(device.deviceName) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isLocalDevice)
                MaterialTheme.colorScheme.primaryContainer
            else if (device.isTrusted)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (device.isLocalDevice)
                        Icons.Default.Smartphone
                    else
                        Icons.Default.Devices,
                    contentDescription = null,
                    tint = if (device.isLocalDevice)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = device.vendorName,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (!device.isLocalDevice) {
                    Switch(
                        checked = device.isTrusted,
                        onCheckedChange = onSetTrust,
                        thumbContent = {
                            if (device.isTrusted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            DetailItem("IP Address", device.ipAddress)
            DetailItem("MAC Address", device.macAddress)

            val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            DetailItem("First Seen", dateFormatter.format(Date(device.firstSeen)))
            DetailItem("Last Seen", dateFormatter.format(Date(device.lastSeen)))

            if (!device.isLocalDevice) {
                TextButton(
                    onClick = { showRenameDialog = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Rename")
                }
            }
        }
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(device.deviceName) }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Device") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Device Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deviceName = newName
                        onRename(newName)
                        showRenameDialog = false
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}