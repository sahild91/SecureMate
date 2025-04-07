package com.example.securemate.wifi_scanner.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.securemate.wifi_scanner.viewmodel.WifiScannerViewModel

/**
 * Add WiFi scanner routes to the navigation graph
 */
fun NavGraphBuilder.addWifiScannerRoutes(navController: NavHostController) {
    // Main WiFi Scanner Screen
    composable(route = WifiNavConstants.WIFI_SCANNER) {
        val viewModel: WifiScannerViewModel = viewModel()
        WifiScannerScreen(navController = navController, viewModel = viewModel)
    }

    // Device Details Screen
    composable(
        route = "${WifiNavConstants.DEVICE_DETAILS}/{macAddress}",
        arguments = listOf(
            navArgument("macAddress") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
        val viewModel: WifiScannerViewModel = viewModel()
        DeviceDetailsScreen(navController = navController, macAddress = macAddress, viewModel = viewModel)
    }
}