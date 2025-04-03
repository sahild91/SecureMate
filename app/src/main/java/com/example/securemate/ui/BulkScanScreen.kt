package com.example.securemate.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.securemate.sms_scanner.SmsParser
import com.example.securemate.threat_model.ThreatChecker
import com.example.securemate.flagged_links_logger.LoggerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkScanScreen(navController: NavController) {
    val context = LocalContext.current

    var selectedDate by remember {
        mutableStateOf(Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -7)
        }.time)
    }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    var isScanning by remember { mutableStateOf(false) }
    var totalCount by remember { mutableStateOf(0) }
    var currentIndex by remember { mutableStateOf(0) }
    var suspiciousCount by remember { mutableStateOf(0) }
    var showResult by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk SMS Scan") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            if (!permissionGranted) {
                Text("Permission required to read SMS.", color = MaterialTheme.colorScheme.error)
                Button(onClick = {
                    permissionLauncher.launch(Manifest.permission.READ_SMS)
                }) {
                    Text("Grant Permission")
                }
            } else {
                Text("Selected Date: ${dateFormat.format(selectedDate)}")
                Spacer(Modifier.height(16.dp))

                Button(onClick = {
                    val calendar = Calendar.getInstance().apply { time = selectedDate }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            calendar.set(year, month, day)
                            selectedDate = calendar.time
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }) {
                    Text("Choose Custom Date")
                }

                Spacer(Modifier.height(12.dp))
                var showConfirmDialog by remember { mutableStateOf(false) }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = {
                        selectedDate = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_MONTH, -7)
                        }.time
                    }) {
                        Text("Reset to 7 Days Ago")
                    }

                    Button(onClick = {
                        showConfirmDialog = true
                    }) {
                        Text("Scan All SMS")
                    }
                }

                if (showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        title = { Text("Scan All SMS?") },
                        text = {
                            Text("This will scan your entire SMS inbox from the very beginning. This may take a while depending on your inbox size.")
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                selectedDate = Date(0)
                                showConfirmDialog = false
                            }) {
                                Text("Yes, Scan All")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))
                var startScanTrigger by remember { mutableStateOf(false) }

                Button(
                    enabled = !isScanning,
                    onClick = {
                        isScanning = true
                        showResult = null
                        totalCount = 0
                        currentIndex = 0
                        suspiciousCount = 0
                        startScanTrigger = true
                    }
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scanning...")
                    } else {
                        Text("Scan Now")
                    }
                }

                LaunchedEffect(startScanTrigger) {
                    if (startScanTrigger) {
                        val (total, flagged) = scanSmsWithProgress(
                            context,
                            selectedDate,
                            onUpdate = { current -> currentIndex = current }
                        )
                        isScanning = false
                        totalCount = total
                        suspiciousCount = flagged
                        showResult = "Scan complete. $flagged suspicious links found out of $total SMS."
                        startScanTrigger = false
                    }
                }

                if (isScanning) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { if (totalCount == 0) 0f else currentIndex / totalCount.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Scanning SMS ${currentIndex} of $totalCount...")
                }

                Spacer(Modifier.height(24.dp))
                showResult?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

suspend fun scanSmsWithProgress(
    context: Context,
    fromDate: Date,
    onUpdate: (Int) -> Unit
): Pair<Int, Int> = withContext(Dispatchers.IO) {
    val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
    val cursor: Cursor? = context.contentResolver.query(
        uri,
        arrayOf("address", "body", "date"),
        "date>=?",
        arrayOf(fromDate.time.toString()),
        "date DESC"
    )

    var total = 0
    var flagged = 0
    var index = 0

    cursor?.use {
        total = it.count
        while (it.moveToNext()) {
            val sender = it.getString(0) ?: "Unknown"
            val messageBody = it.getString(1) ?: ""
            val timestamp = it.getLong(2)

            val links = SmsParser.extractLinks(messageBody)
            for (url in links) {
                val result = ThreatChecker.checkThreat(url)
                if (result.isThreat) {
                    LoggerHelper.logSuspiciousLink(
                        context = context,
                        url = url,
                        sender = sender,
                        time = timestamp,
                        reason = result.reason,
                        level = result.level,
                        messageBody = messageBody
                    )
                    flagged++
                }
            }

            index++
            onUpdate(index)
        }
    }

    total to flagged
}