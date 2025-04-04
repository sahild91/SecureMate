package com.example.securemate.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun PinSetupScreen(navController: NavController) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf(1) } // 1 = enter pin, 2 = confirm pin
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (stage == 1) "Create a PIN" else "Confirm your PIN",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "This PIN will be used to unlock the app",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // PIN display dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val currentPin = if (stage == 1) pin else confirmPin
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            if (index < currentPin.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                )
            }
        }

        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PIN keypad
        NumericKeypad(
            onKeyPressed = { digit ->
                if (stage == 1) {
                    if (pin.length < 4) {
                        pin += digit
                        error = null

                        // Move to confirmation when 4 digits entered
                        if (pin.length == 4) {
                            stage = 2
                        }
                    }
                } else {
                    if (confirmPin.length < 4) {
                        confirmPin += digit
                        error = null

                        // Check confirmation when 4 digits entered
                        if (confirmPin.length == 4) {
                            if (pin == confirmPin) {
                                // PIN set successfully
                                val prefs = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
                                prefs.edit()
                                    .putString("app_pin", pin)
                                    .putBoolean("pin_auth_enabled", true)
                                    .putBoolean("biometric_auth_enabled", false)
                                    .apply()

                                navController.navigateUp()
                            } else {
                                error = "PINs don't match. Try again."
                                stage = 1
                                pin = ""
                                confirmPin = ""
                            }
                        }
                    }
                }
            },
            onDelete = {
                if (stage == 1) {
                    if (pin.isNotEmpty()) {
                        pin = pin.dropLast(1)
                        error = null
                    }
                } else {
                    if (confirmPin.isNotEmpty()) {
                        confirmPin = confirmPin.dropLast(1)
                        error = null
                    }
                }
            },
            onCancel = {
                navController.navigateUp()
            }
        )
    }
}