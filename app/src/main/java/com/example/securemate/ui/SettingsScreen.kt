package com.example.securemate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.securemate.settings.ScanPreferencesHelper
import com.example.securemate.worker.scheduleOrCancelWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(ScanPreferencesHelper.isScanEnabled(context)) }
    var frequency by remember { mutableStateOf(ScanPreferencesHelper.getScanFrequencyDays(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Auto Scan", modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = {
                    enabled = it
                    ScanPreferencesHelper.setScanEnabled(context, it)
                    scheduleOrCancelWorker(context, it, frequency)
                })
            }

            Spacer(Modifier.height(12.dp))

            Text("Scan Frequency")
            DropdownMenuWithFrequency(frequency) { selected ->
                frequency = selected
                ScanPreferencesHelper.setScanFrequencyDays(context, selected)
                if (enabled) {
                    scheduleOrCancelWorker(context, true, selected)
                }
            }
        }
    }
}

@Composable
fun DropdownMenuWithFrequency(current: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = mapOf(1 to "Daily", 3 to "Every 3 Days", 7 to "Weekly")

    Box {
        TextButton(onClick = { expanded = true }) {
            Text("Selected: ${options[current]}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    onSelect(value)
                    expanded = false
                })
            }
        }
    }
}