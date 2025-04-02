package com.example.securemate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.example.securemate.ui.HomeScreen
import com.example.securemate.ui.SuspiciousLinksScreen
import com.example.securemate.ui.theme.SecureMateTheme
import com.example.securemate.viewmodel.SuspiciousLinksViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SuspiciousLinksViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecureMateTheme {
                SecureMateNavGraph(viewModel)
            }
        }
    }
}

@Composable
fun SecureMateNavGraph(viewModel: SuspiciousLinksViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController)
        }
        composable("suspicious_links") {
            SuspiciousLinksScreen(viewModel)
        }
    }
}