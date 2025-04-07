package com.example.securemate.ui

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE) }

    var biometricEnabled by remember { mutableStateOf(prefs.getBoolean("biometric_auth_enabled", false)) }
    var pinEnabled by remember { mutableStateOf(prefs.getBoolean("pin_auth_enabled", false)) }
    var lockOnExit by remember { mutableStateOf(prefs.getBoolean("lock_on_exit", true)) }
    var showPinSetupDialog by remember { mutableStateOf(false) }

    val biometricManager = BiometricManager.from(context)
    val canUseBiometric by remember {
        mutableStateOf(
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    == BiometricManager.BIOMETRIC_SUCCESS
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Security") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                "App Lock",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Biometric lock option
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Use Biometric Authentication")
                    Text(
                        "Unlock with fingerprint or face",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = { checked ->
                        biometricEnabled = checked
                        prefs.edit().putBoolean("biometric_auth_enabled", checked).apply()

                        // If biometric is enabled, automatically disable PIN
                        if (checked && pinEnabled) {
                            pinEnabled = false
                            prefs.edit().putBoolean("pin_auth_enabled", false).apply()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // PIN lock option
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Use PIN Lock")
                    Text(
                        "Protect with a 4-digit PIN",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = pinEnabled,
                    onCheckedChange = { checked ->
                        pinEnabled = checked
                        prefs.edit().putBoolean("pin_auth_enabled", checked).apply()

                        // If PIN is enabled, automatically disable biometric
                        if (checked && biometricEnabled) {
                            biometricEnabled = false
                            prefs.edit().putBoolean("biometric_auth_enabled", false).apply()
                        }

                        // Show PIN setup dialog if enabling
                        if (checked) {
                            // If enabling PIN, show setup dialog
                            showPinSetupDialog = true
                        } else {
                            // Disable PIN
                            pinEnabled = false
                            prefs.edit().putBoolean("pin_auth_enabled", false).apply()
                        }
                    }
                )
            }
            if (pinEnabled) {
                TextButton(
                    onClick = { showPinSetupDialog = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Change PIN")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Lock on exit option
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lock on Exit")
                    Text(
                        "Require authentication when app is reopened",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = lockOnExit,
                    onCheckedChange = { checked ->
                        lockOnExit = checked
                        prefs.edit().putBoolean("lock_on_exit", checked).apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Data Protection",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // Show confirmation dialog first
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear All Scan Data")
            }
        }
    }

    if (showPinSetupDialog) {
        navController.navigate("pin_setup")
        showPinSetupDialog = false
    }
}