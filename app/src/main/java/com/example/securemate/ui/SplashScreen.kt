package com.example.securemate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SplashScreen(navController: NavController) {
    Scaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔐 SecureMate", style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { navController.navigate("home") }) {
                    Text("Enter")
                }
            }
        }
    }
}