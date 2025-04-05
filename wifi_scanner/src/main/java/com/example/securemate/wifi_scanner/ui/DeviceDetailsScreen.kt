package com.example.securemate.wifi_scanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.securemate.wifi_scanner.NetworkDevice
import com.example.securemate.wifi_scanner.viewmodel.WifiScannerViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Device Details Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailsScreen(
    navController: NavController,
    macAddress: String,
    viewModel: WifiScannerViewModel = viewModel()
) {
    val devices by viewModel.discoveredDevices.collectAsState()
    val device = devices.find { it.macAddress == macAddress }

    var showRenameDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (device == null) {
            // Device not found
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Device not found in current scan results")
            }
        } else {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Device header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Device icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    device.isLocalDevice -> MaterialTheme.colorScheme.primary
                                    device.isTrusted -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                device.isLocalDevice -> Icons.Default.Smartphone
                                device.vendorName.contains("Apple", ignoreCase = true) -> Icons.Default.DevicesOther
                                else -> Icons.Default.Devices
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.deviceName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = device.vendorName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (device.isTrusted) Icons.Default.VerifiedUser else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (device.isTrusted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = if (device.isTrusted) "Trusted Device" else "Untrusted Device",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (device.isTrusted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Trust status card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (device.isTrusted)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (device.isTrusted) Icons.Default.VerifiedUser else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (device.isTrusted)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (device.isTrusted) "This is a trusted device" else "This device is not trusted",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (device.isTrusted)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (device.isTrusted)
                                "You've marked this device as trusted on your network."
                            else
                                "Untrusted devices could potentially pose security risks.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (device.isTrusted)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!device.isLocalDevice) {
                            Button(
                                onClick = {
                                    if (device.isTrusted) {
                                        viewModel.setDeviceTrust(device.macAddress, false)
                                    } else {
                                        viewModel.setDeviceTrust(device.macAddress, true)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (device.isTrusted)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Icon(
                                    imageVector = if (device.isTrusted)
                                        Icons.Default.RemoveCircle
                                    else
                                        Icons.Default.Add,
                                    contentDescription = null
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = if (device.isTrusted)
                                        "Remove Trust"
                                    else
                                        "Trust this Device"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Device details card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Device Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // IP Address
                        DetailRow(
                            icon = Icons.Default.Wifi,
                            label = "IP Address",
                            value = device.ipAddress
                        )

                        // MAC Address
                        DetailRow(
                            icon = Icons.Default.Memory,
                            label = "MAC Address",
                            value = device.macAddress
                        )

                        // Vendor
                        DetailRow(
                            icon = Icons.Default.Business,
                            label = "Manufacturer",
                            value = device.vendorName
                        )

                        // First Seen
                        DetailRow(
                            icon = Icons.Default.Visibility,
                            label = "First Seen",
                            value = dateFormat.format(Date(device.firstSeen))
                        )

                        // Last Seen
                        DetailRow(
                            icon = Icons.Default.Update,
                            label = "Last Seen",
                            value = dateFormat.format(Date(device.lastSeen))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                if (!device.isLocalDevice) {
                    Button(
                        onClick = { showRenameDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("Rename Device")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showRemoveDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("Remove from History")
                    }
                }
            }

            // Rename Dialog
            if (showRenameDialog) {
                var newName by remember { mutableStateOf(device.deviceName) }

                AlertDialog(
                    onDismissRequest = { showRenameDialog = false },
                    title = { Text("Rename Device") },
                    text = {
                        Column {
                            Text("Enter a new name for this device:")

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Device Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    viewModel.setDeviceName(device.macAddress, newName)
                                }
                                showRenameDialog = false
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showRenameDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Remove Dialog
            if (showRemoveDialog) {
                AlertDialog(
                    onDismissRequest = { showRemoveDialog = false },
                    title = { Text("Remove Device") },
                    text = {
                        Text("Are you sure you want to remove this device from history? This cannot be undone.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                // Remove device logic would go here
                                // Not implemented in the current API
                                showRemoveDialog = false
                                navController.navigateUp()
                            }
                        ) {
                            Text("Remove")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showRemoveDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Detail row for device information
 */
@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}