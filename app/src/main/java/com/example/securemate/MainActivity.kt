package com.example.securemate

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.securemate.ui.BulkScanScreen
import com.example.securemate.ui.HomeScreen
import com.example.securemate.ui.SettingsScreen
import com.example.securemate.ui.SplashScreen
import com.example.securemate.ui.SuspiciousLinksScreen
import com.example.securemate.ui.theme.SecureMateTheme
import com.example.securemate.viewmodel.SuspiciousLinksViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SuspiciousLinksViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecureMateTheme {
                RequestNotificationPermission()
                SecureMateNavGraph(viewModel)
            }
        }
    }
}

@Composable
fun SecureMateNavGraph(viewModel: SuspiciousLinksViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController)
        }
        composable("home") {
            HomeScreen(navController)
        }
        composable("suspicious_links") {
            SuspiciousLinksScreen(viewModel, navController)
        }
        composable("bulk_scan") {
            BulkScanScreen(navController)
        }
        composable("settings") {
            SettingsScreen(navController)
        }
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