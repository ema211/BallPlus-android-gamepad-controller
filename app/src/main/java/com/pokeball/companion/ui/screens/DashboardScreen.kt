package com.pokeball.companion.ui.screens

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokeball.companion.data.model.PokeballState
import com.pokeball.companion.ui.MainViewModel
import com.pokeball.companion.ui.components.BatteryIndicator
import com.pokeball.companion.ui.components.PokeballVisualizer
import com.pokeball.companion.ui.theme.PokeAccent
import com.pokeball.companion.ui.theme.PokeGreen
import com.pokeball.companion.ui.theme.PokeRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateCalibration: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()
    var showDeviceSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Poké Ball Plus",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Made by Ema-A with Google Antigravity",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                actions = {
                    BatteryIndicator(batteryPercent = state.batteryPercent)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onNavigateCalibration) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Calibration",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PokeRed
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Accessibility Service Warning Banner
            if (!isAccessibilityEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3E2723)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Accessibility Disabled",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Required to dispatch touches in emulators",
                                color = Color(0xFFBCAAA4),
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) {
                            Text("Enable", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Connection Status Pill
            ConnectionStatusBadge(state = state, isScanning = isScanning)

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Poké Ball Canvas (Mirrors inputs in real time)
            PokeballVisualizer(
                state = state,
                size = 230.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Real-Time Sensor Metrics Card
            RealtimeMetricsCard(state = state)

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!state.isConnected) {
                    Button(
                        onClick = {
                            viewModel.startScan()
                            showDeviceSheet = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PokeRed)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search Poké Ball")
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.disconnect() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PokeRed)
                    ) {
                        Text("Disconnect")
                    }
                }

                Button(
                    onClick = {
                        val intent = Intent(context, MappingOverlayActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PokeAccent)
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Touch Mapping", color = Color.Black)
                }
            }
        }
    }

    // Modal Bottom Sheet for Device Scanning
    if (showDeviceSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showDeviceSheet = false
                viewModel.stopScan()
            },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Discovered Devices",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PokeAccent
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        IconButton(onClick = { viewModel.rescan() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rescan",
                                tint = if (isScanning) PokeAccent else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (discoveredDevices.isEmpty()) {
                    Text(
                        text = if (isScanning) "Searching for 'Pokeball' or 'Pokemon PBP'...\nPress the Top Button or Click the Stick to wake the Poké Ball!"
                        else "No Poké Ball found. Wake the ball and try again.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(discoveredDevices) { device ->
                            DeviceListItem(device = device) {
                                viewModel.connectDevice(device)
                                showDeviceSheet = false
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusBadge(state: PokeballState, isScanning: Boolean) {
    val (color, text) = when {
        state.isConnected -> PokeGreen to "Connected: ${state.deviceName ?: "Poké Ball Plus"}"
        state.isConnecting -> Color(0xFFFFB300) to "Connecting..."
        isScanning -> PokeAccent to "Scanning for Poké Ball..."
        else -> Color(0xFF757575) to "Disconnected"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color(0xFF2C2F33), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

@Composable
fun RealtimeMetricsCard(state: PokeballState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Live Hardware Telemetry",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("Joystick X", String.format("%.2f", state.stickX))
                MetricItem("Joystick Y", String.format("%.2f", state.stickY))
                MetricItem("Raw X / Y", "${state.rawStickX} / ${state.rawStickY}")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("Top Button", if (state.topButtonPressed) "PRESSED" else "IDLE", isAccent = state.topButtonPressed)
                MetricItem("Stick Click", if (state.stickClicked) "CLICKED" else "IDLE", isAccent = state.stickClicked)
                MetricItem("Shake Detected", if (state.isShakeDetected) "ACTIVE" else "IDLE", isAccent = state.isShakeDetected)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("Accel X", String.format("%.2f G", state.accelX))
                MetricItem("Accel Y", String.format("%.2f G", state.accelY))
                MetricItem("Accel Z", String.format("%.2f G", state.accelZ))
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, isAccent: Boolean = false) {
    Column {
        Text(text = label, color = Color.Gray, fontSize = 11.sp)
        Text(
            text = value,
            color = if (isAccent) PokeAccent else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun DeviceListItem(device: BluetoothDevice, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        color = Color(0xFF2C2F33),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = PokeAccent)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                @Suppress("MissingPermission")
                Text(text = device.name ?: "Unknown Device", fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = device.address, color = Color.Gray, fontSize = 12.sp)
            }
            Text(text = "Connect", color = PokeAccent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}
