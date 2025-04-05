package com.example.securemate

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.securemate.app_lock.BiometricAuthHelper
import com.example.securemate.ui.BulkScanScreen
import com.example.securemate.ui.HomeScreen
import com.example.securemate.ui.PinLockScreen
import com.example.securemate.ui.PinSetupScreen
import com.example.securemate.ui.PrivacySettingsScreen
import com.example.securemate.ui.SettingsScreen
import com.example.securemate.ui.SplashScreen
import com.example.securemate.ui.SuspiciousLinksScreen
import com.example.securemate.ui.navigation.NavRoutes
import com.example.securemate.ui.theme.SecureMateTheme
import com.example.securemate.viewmodel.SuspiciousLinksViewModel
import com.example.securemate.wifi_scanner.ui.addWifiScannerRoutes
import com.example.securemate.wifi_scanner.ui.WifiNavConstants

class MainActivity : FragmentActivity() {
    private val viewModel: SuspiciousLinksViewModel by viewModels()
    private var isAuthenticated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if biometric auth is enabled in preferences
        val securityPrefs = getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        val biometricEnabled = securityPrefs.getBoolean("biometric_auth_enabled", false)
        val pinEnabled = securityPrefs.getBoolean("pin_auth_enabled", false)

        when {
            biometricEnabled -> {
                BiometricAuthHelper(this).showBiometricPrompt {
                    isAuthenticated = true
                    initializeApp()
                }
            }
            pinEnabled -> {
                // Will use the PIN screen in setContent
                isAuthenticated = false
                initializeApp()
            }
            else -> {
                isAuthenticated = true
                initializeApp()
            }
        }
    }

    private fun initializeApp() {
        setContent {
            SecureMateTheme {
                if (!isAuthenticated && getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
                        .getBoolean("pin_auth_enabled", false)) {
                    PinLockScreen(
                        onPinVerified = {
                            isAuthenticated = true
                            // Redraw the UI after authentication succeeds
                            setContent {
                                SecureMateTheme {
                                    RequestNotificationPermission()
                                    SecureMateNavGraph(viewModel)
                                }
                            }
                        },
                        onFallback = {
                            // If PIN validation fails or doesn't exist
                            isAuthenticated = true
                            setContent {
                                SecureMateTheme {
                                    RequestNotificationPermission()
                                    SecureMateNavGraph(viewModel)
                                }
                            }
                        }
                    )
                } else {
                    RequestNotificationPermission()
                    SecureMateNavGraph(viewModel)
                }
            }
        }
    }
}

@Composable
fun SecureMateNavGraph(viewModel: SuspiciousLinksViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable(NavRoutes.SPLASH) {
            SplashScreen(navController)
        }
        composable(NavRoutes.HOME) {
            HomeScreen(navController)
        }
        composable(NavRoutes.LINKS_LOG) {
            SuspiciousLinksScreen(viewModel, navController)
        }
        composable(NavRoutes.BULK_SCAN) {
            BulkScanScreen(navController)
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(navController)
        }
        composable(NavRoutes.PRIVACY_SETTINGS) {
            PrivacySettingsScreen(navController)
        }
        composable(NavRoutes.PIN_SETUP) {
            PinSetupScreen(navController)
        }

        addWifiScannerRoutes(navController)
    }
}

@Composable
fun RequestNotificationPermission() {
    val context = LocalContext.current

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(
                context,
                "Notification permission granted. You'll be alerted when suspicious links are found.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "Notification permission denied. SecureMate won't be able to alert you.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}