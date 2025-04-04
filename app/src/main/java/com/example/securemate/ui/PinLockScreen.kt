package com.example.securemate.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun PinLockScreen(
    onPinVerified: () -> Unit,
    onFallback: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE) }
    val storedPin = remember { prefs.getString("app_pin", null) }

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var attempts by remember { mutableStateOf(0) }

    // If no PIN is set, go immediately to fallback
    LaunchedEffect(Unit) {
        if (storedPin == null) {
            onFallback()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter your PIN", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // PIN display dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            if (index < pin.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                )
            }
        }

        if (error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Incorrect PIN (Attempt ${attempts}/5)",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Show option to reset after multiple attempts
        if (attempts >= 3) {
            TextButton(onClick = {
                // Reset and bypass authentication
                prefs.edit()
                    .putBoolean("pin_auth_enabled", false)
                    .putBoolean("biometric_auth_enabled", false)
                    .apply()
                onFallback()
            }) {
                Text("Forgot PIN? Reset Security Settings")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // PIN keypad
        NumericKeypad(
            onKeyPressed = { digit ->
                if (pin.length < 4) {
                    pin += digit
                    error = false

                    // Check if PIN is complete
                    if (pin.length == 4) {
                        if (pin == storedPin) {
                            onPinVerified()
                        } else {
                            error = true
                            attempts++
                            pin = ""

                            // After 5 failed attempts, reset security
                            if (attempts >= 5) {
                                prefs.edit()
                                    .putBoolean("pin_auth_enabled", false)
                                    .putBoolean("biometric_auth_enabled", false)
                                    .apply()
                                onFallback()
                            }
                        }
                    }
                }
            },
            onDelete = {
                if (pin.isNotEmpty()) {
                    pin = pin.dropLast(1)
                    error = false
                }
            }
        )
    }
}

@Composable
fun NumericKeypad(
    onKeyPressed: (String) -> Unit,
    onDelete: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    val numbers = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(if (onCancel != null) "✕" else "", "0", "⌫")
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        numbers.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .then(
                                if (digit.isNotEmpty())
                                    Modifier.clickable {
                                        when (digit) {
                                            "⌫" -> onDelete()
                                            "✕" -> onCancel?.invoke()
                                            else -> onKeyPressed(digit)
                                        }
                                    }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (digit.isNotEmpty()) {
                            Text(
                                digit,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}