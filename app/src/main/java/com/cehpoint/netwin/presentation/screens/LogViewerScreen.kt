package com.cehpoint.netwin.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cehpoint.netwin.ui.theme.DarkBackground
import com.cehpoint.netwin.ui.theme.NetWinCyan
import com.cehpoint.netwin.utils.Logger

/**
 * Screen to view persistent logs for debugging
 * Logs are saved to file and can be viewed even after app restart
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf("Loading logs...") }
    var showShareDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        logs = Logger.readLogs(context)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Debug Logs",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row {
                IconButton(onClick = { 
                    logs = Logger.readLogs(context)
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NetWinCyan)
                }
                
                IconButton(onClick = { showShareDialog = true }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = NetWinCyan)
                }
                
                IconButton(onClick = { 
                    Logger.clearLogs(context)
                    logs = "Logs cleared"
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.Red)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Log content
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs.split("\n").filter { it.isNotBlank() }) { logLine ->
                    Text(
                        text = logLine,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Info text
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Logs are saved to: Downloads/netwin_debug_logs.txt",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
    
    // Share dialog
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Share Logs") },
            text = { 
                Text(
                    "Logs can be found in your Downloads folder as 'netwin_debug_logs.txt'. " +
                    "You can share this file for debugging purposes.",
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("OK", color = NetWinCyan)
                }
            }
        )
    }
}
