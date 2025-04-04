package com.example.securemate.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.securemate.viewmodel.SuspiciousLinksViewModel
import com.example.securemate.flagged_links_logger.FlaggedLink
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuspiciousLinksScreen(viewModel: SuspiciousLinksViewModel, navController: NavController) {
    val links by viewModel.links.collectAsState()
    var selectedLevel by remember { mutableStateOf("ALL") }

    val filteredLinks = when (selectedLevel) {
        "HIGH" -> links.filter { it.threatLevel == "HIGH" }
        "MEDIUM" -> links.filter { it.threatLevel == "MEDIUM" }
        "LOW" -> links.filter { it.threatLevel == "LOW" }
        else -> links
    }

    LaunchedEffect(Unit) {
        viewModel.loadLinks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suspicious Links Log") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    DropdownMenuWithFilter(selectedLevel) {
                        selectedLevel = it
                    }
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(filteredLinks) { link ->
                SuspiciousLinkCard(link)
            }
        }
    }
}

@Composable
fun DropdownMenuWithFilter(selected: String, onFilterChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text("Filter: $selected")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("ALL", "HIGH", "MEDIUM", "LOW").forEach { level ->
                DropdownMenuItem(text = { Text(level) }, onClick = {
                    onFilterChange(level)
                    expanded = false
                })
            }
        }
    }
}

@Composable
fun SuspiciousLinkCard(link: FlaggedLink) {
    var expanded by remember { mutableStateOf(false) }
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val date = formatter.format(Date(link.timestamp))

    val color = when (link.threatLevel) {
        "HIGH" -> MaterialTheme.colorScheme.error
        "MEDIUM" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sender: ${link.sender}", style = MaterialTheme.typography.titleMedium)
            Text("Time: $date", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text("🔗 ${link.url}", style = MaterialTheme.typography.bodyLarge)
            Text("🧠 ${link.reason}", style = MaterialTheme.typography.bodyMedium)
            Text("🔥 Threat Level: ${link.threatLevel}", color = color, style = MaterialTheme.typography.labelLarge)

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color= Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "📝 Message Content (Full):\n${link.message}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
