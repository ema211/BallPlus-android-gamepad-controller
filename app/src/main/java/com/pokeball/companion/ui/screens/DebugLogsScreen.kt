package com.pokeball.companion.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokeball.companion.service.PokeballAccessibilityService
import com.pokeball.companion.ui.MainViewModel
import com.pokeball.companion.ui.theme.PokeAccent
import com.pokeball.companion.ui.theme.PokeGreen
import com.pokeball.companion.ui.theme.PokeRed
import com.pokeball.companion.util.AppLogger
import com.pokeball.companion.util.LogEntry
import com.pokeball.companion.util.LogLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val logs by AppLogger.logsFlow.collectAsState()
    val state by viewModel.state.collectAsState()
    var isLoggingActive by remember { mutableStateOf(AppLogger.isLoggingEnabled) }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    val isAccEnabled = PokeballAccessibilityService.isEnabled(context)
    val isAccBound = PokeballAccessibilityService.instance != null
    val shizukuStatus = remember(context) { getShizukuStatus(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Diagnostics & Live Logs",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "${logs.size} / 500 buffered events",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val dump = AppLogger.dumpLogs()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Pokeball Diagnostics", dump))
                            Toast.makeText(context, "Diagnostic logs copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Logs", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            AppLogger.clearLogs()
                            Toast.makeText(context, "Log buffer cleared", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF212121))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF121416))
                .padding(12.dp)
        ) {
            // Status Diagnostics Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "System Diagnostics Summary",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // BLE Status
                    val bleText = when {
                        state.isConnected -> "CONNECTED (${state.deviceName ?: "Unknown"} [${state.deviceAddress ?: "N/A"}])"
                        state.isConnecting -> "CONNECTING (${state.deviceName ?: "Target"})..."
                        else -> "DISCONNECTED"
                    }
                    val bleColor = if (state.isConnected) PokeGreen else if (state.isConnecting) Color(0xFFFFB300) else Color.Gray
                    DiagnosticRow(label = "BLE State", value = bleText, valueColor = bleColor)

                    // Accessibility Status
                    val accText = when {
                        isAccEnabled && isAccBound -> "ACTIVE & BOUND"
                        isAccEnabled && !isAccBound -> "ENABLED in OS, WAITING FOR BIND"
                        else -> "DISABLED"
                    }
                    val accColor = when {
                        isAccEnabled && isAccBound -> PokeGreen
                        isAccEnabled && !isAccBound -> Color(0xFFFFB300)
                        else -> PokeRed
                    }
                    DiagnosticRow(label = "Accessibility Service", value = accText, valueColor = accColor)

                    // Shizuku Status
                    DiagnosticRow(label = "Shizuku Service", value = shizukuStatus, valueColor = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Master Switch & Quick Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2124), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isLoggingActive,
                        onCheckedChange = {
                            isLoggingActive = it
                            AppLogger.isLoggingEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = PokeAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLoggingActive) "Live Logging: ON" else "Live Logging: PAUSED",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            val dump = AppLogger.dumpLogs()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Pokeball Diagnostics", dump))
                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PokeAccent)
                    ) {
                        Text("Copy", color = Color.Black, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { AppLogger.clearLogs() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PokeRed)
                    ) {
                        Text("Clear", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Monospace Scrollable Terminal Log Viewer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0A0C0E), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No diagnostic events captured yet.\nConnect or use the Poké Ball to stream events.",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(logs) { entry ->
                            LogItemView(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Text(text = value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun LogItemView(entry: LogEntry) {
    val levelColor = when (entry.level) {
        LogLevel.ERROR -> PokeRed
        LogLevel.WARN -> Color(0xFFFFB300)
        LogLevel.INFO -> PokeAccent
        LogLevel.DEBUG -> Color(0xFF90A4AE)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            text = entry.formattedTime,
            color = Color(0xFF78909C),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "[${entry.level.name.take(1)}]",
            color = levelColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${entry.tag}:",
            color = Color(0xFFCFD8DC),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = entry.message,
            color = levelColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun getShizukuStatus(context: Context): String {
    return try {
        val pm = context.packageManager
        pm.getPackageInfo("moe.shizuku.privileged.api", 0)
        "Installed"
    } catch (e: Exception) {
        "Not installed"
    }
}
