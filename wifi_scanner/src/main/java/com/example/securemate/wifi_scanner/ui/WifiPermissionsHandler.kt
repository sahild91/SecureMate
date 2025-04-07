package com.example.securemate.wifi_scanner.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Permissions required for WiFi scanning
 */
enum class WifiPermission(val permission: String) {
    LOCATION_FINE(Manifest.permission.ACCESS_FINE_LOCATION),
    LOCATION_COARSE(Manifest.permission.ACCESS_COARSE_LOCATION),
    NEARBY_WIFI_DEVICES(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.NEARBY_WIFI_DEVICES else "")
}

/**
 * WiFi Permission Handler composable
 * Handles requesting necessary permissions for WiFi scanning
 */
@Composable
fun WifiPermissionsHandler(
    onPermissionsGranted: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var permissionsRequested by remember { mutableStateOf(false) }

    // List of required permissions based on API level
    val requiredPermissions = remember {
        mutableListOf(
            WifiPermission.LOCATION_FINE.permission,
            WifiPermission.LOCATION_COARSE.permission
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(WifiPermission.NEARBY_WIFI_DEVICES.permission)
            }
        }.toTypedArray()
    }

    // Request multiple permissions
    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsRequested = true

        // Check if all required permissions are granted
        val allGranted = requiredPermissions.all { permission ->
            permissions[permission] == true
        }

        if (allGranted) {
            // Check if location services are enabled
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

            if (isLocationEnabled) {
                onPermissionsGranted()
            }
        }
    }

    // Check if all permissions are already granted
    val allPermissionsGranted = requiredPermissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // Check location services
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    LaunchedEffect(Unit) {
        if (!permissionsRequested && !allPermissionsGranted) {
            multiplePermissionLauncher.launch(requiredPermissions)
        } else if (allPermissionsGranted && isLocationEnabled) {
            onPermissionsGranted()
        }
    }

    if (allPermissionsGranted && isLocationEnabled) {
        // All permissions granted and location enabled, show content
        content()
    } else {
        // Show permission request screen
        PermissionRequestScreen(
            allPermissionsGranted = allPermissionsGranted,
            isLocationEnabled = isLocationEnabled,
            onRequestPermissions = {
                multiplePermissionLauncher.launch(requiredPermissions)
            }
        )
    }
}

/**
 * Permission request screen
 */
@Composable
fun PermissionRequestScreen(
    allPermissionsGranted: Boolean,
    isLocationEnabled: Boolean,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!allPermissionsGranted) {
            // Show permission request
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Location Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "WiFi scanning requires location permissions to work properly. " +
                        "This allows SecureMate to scan and analyze your WiFi network.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Permissions")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            ) {
                Text("Open App Settings")
            }
        } else if (!isLocationEnabled) {
            // Show location services request
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Location Services Required",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "WiFi scanning requires location services to be enabled. " +
                        "Please enable location services in your device settings.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable Location Services")
            }
        }
    }
}