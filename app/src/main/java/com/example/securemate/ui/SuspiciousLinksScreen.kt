package com.example.securemate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.securemate.viewmodel.SuspiciousLinksViewModel
import com.example.securemate.flagged_links_logger.FlaggedLink
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuspiciousLinksScreen(viewModel: SuspiciousLinksViewModel) {
    val links by viewModel.links.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadLinks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suspicious Links Log") },
                actions = {
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs")
                    }
                }
            )
        }
    ) { padding ->
        if (links.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No suspicious links detected yet.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(links) { link ->
                    SuspiciousLinkCard(link)
                }
            }
        }
    }
}

@Composable
fun SuspiciousLinkCard(link: FlaggedLink) {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val date = formatter.format(Date(link.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "🔗 ${link.url}", style = MaterialTheme.typography.titleMedium)
            Text(text = "🧠 Reason: ${link.reason}", style = MaterialTheme.typography.bodySmall)
            Text(text = "🕒 $date", style = MaterialTheme.typography.labelSmall)
        }
    }
}
