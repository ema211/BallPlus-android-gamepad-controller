package com.pokeball.companion.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import com.pokeball.companion.R
import com.pokeball.companion.data.model.DpadDirection
import com.pokeball.companion.data.model.StickMode
import com.pokeball.companion.ui.MainViewModel
import com.pokeball.companion.ui.components.JoystickRadar
import com.pokeball.companion.ui.theme.PokeAccent
import com.pokeball.companion.ui.theme.PokeGreen
import com.pokeball.companion.ui.theme.PokeRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateDebugLogs: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val calibration by viewModel.calibration.collectAsState()
    val config by viewModel.mappingConfig.collectAsState()
    val autoReconnect by viewModel.autoReconnect.collectAsState()

    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Joystick & Calibration",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Made by Ema-A with Google Antigravity",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
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
                    // Easter egg: tap 5 times within 2 seconds to enter Debug & Logs
                    IconButton(
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime <= 2000L) {
                                tapCount++
                            } else {
                                tapCount = 1
                            }
                            lastTapTime = now

                            if (tapCount >= 5) {
                                tapCount = 0
                                onNavigateDebugLogs()
                            }
                        }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_icon),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PokeRed)
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
            // Live Radar
            JoystickRadar(
                stickX = state.stickX,
                stickY = state.stickY,
                deadzone = calibration.deadzone,
                stickMode = config.stickMode,
                diagonalThreshold = config.dpadDiagonalThreshold,
                size = 180.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Deadzone Tuning Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Radial Deadzone",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${(calibration.deadzone * 100).toInt()}%",
                            color = PokeAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Filter out micro-wobbles and stick drift when centered.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Slider(
                        value = calibration.deadzone,
                        onValueChange = { viewModel.updateDeadzone(it) },
                        valueRange = 0.05f..0.45f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = PokeAccent,
                            activeTrackColor = PokeAccent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stick Input Mode Card (Full Joystick vs Mapped D-Pad)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Stick Input Mode",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (config.stickMode == StickMode.FULL_JOYSTICK)
                            "Continuous analog output for 360° fluid movement."
                        else
                            "Discretized cardinal & diagonal zones for D-Pad games.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode Selection Segmented Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isFull = config.stickMode == StickMode.FULL_JOYSTICK
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setStickMode(StickMode.FULL_JOYSTICK) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFull) PokeAccent.copy(alpha = 0.2f) else Color(0xFF2C2F33)
                            ),
                            border = if (isFull) BorderStroke(1.5.dp, PokeAccent) else null,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🕹️ Full Joystick",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFull) PokeAccent else Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Smooth analog",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        val isDpad = config.stickMode == StickMode.MAPPED_DPAD
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setStickMode(StickMode.MAPPED_DPAD) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDpad) PokeAccent.copy(alpha = 0.2f) else Color(0xFF2C2F33)
                            ),
                            border = if (isDpad) BorderStroke(1.5.dp, PokeAccent) else null,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "➕ Mapped D-Pad",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDpad) PokeAccent else Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Cardinal & Diagonals",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // When in Mapped D-Pad mode, show the Angular Partition / Diagonal Threshold Slider
                    if (config.stickMode == StickMode.MAPPED_DPAD) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Diagonal Angle Partition",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (config.dpadDiagonalThreshold <= 0.5f)
                                        "Strict 4-Way D-Pad (Diagonals disabled)"
                                    else
                                        "±${config.dpadDiagonalThreshold.toInt()}° diagonal zone (8-Way D-Pad)",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "${config.dpadDiagonalThreshold.toInt()}°",
                                color = PokeAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Slider(
                            value = config.dpadDiagonalThreshold,
                            onValueChange = { viewModel.setDpadDiagonalThreshold(it) },
                            valueRange = 0f..40f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = PokeAccent,
                                activeTrackColor = PokeAccent
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Direction Feedback Badge
                        val activeDir = config.resolveDpadDirection(state.stickX, state.stickY, calibration.deadzone)
                        val dirBg = if (activeDir != DpadDirection.NONE) PokeGreen.copy(alpha = 0.2f) else Color(0xFF2C2F33)
                        val dirColor = if (activeDir != DpadDirection.NONE) PokeGreen else Color.Gray
                        val dirText = if (activeDir != DpadDirection.NONE) "Target Direction: $activeDir" else "D-Pad Neutral (Inside Deadzone)"

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(dirBg, RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dirText,
                                color = dirColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shake Gesture Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Shake Gesture",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Simulate action on physical flick / shake",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = config.isShakeEnabled,
                            onCheckedChange = { viewModel.toggleShakeEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PokeAccent)
                        )
                    }

                    if (config.isShakeEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Sensitivity Threshold", color = Color.White, fontSize = 13.sp)
                            Text(
                                text = String.format("%.2f G", config.shakeSensitivity),
                                color = PokeAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Slider(
                            value = config.shakeSensitivity,
                            onValueChange = { viewModel.updateShakeSensitivity(it) },
                            valueRange = 0.10f..0.60f,
                            colors = SliderDefaults.colors(
                                thumbColor = PokeAccent,
                                activeTrackColor = PokeAccent
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Live Shake Trigger Feedback Box
                        val shakeBg = if (state.isShakeDetected) PokeGreen else Color(0xFF2C2F33)
                        val shakeText = if (state.isShakeDetected) "💥 SHAKE TRIGGERED!" else "Shake the ball to test threshold"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(shakeBg, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shakeText,
                                color = if (state.isShakeDetected) Color.Black else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Auto-Reconnect Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto Background Reconnect",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Automatically re-pair when Poké Ball wakes up",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = autoReconnect,
                        onCheckedChange = { viewModel.toggleAutoReconnect(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PokeAccent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Collapsible Button Behavior & Auto-Repeat Section
            var isTimingsExpanded by remember { mutableStateOf(false) }
            val chevronRotation by animateFloatAsState(
                targetValue = if (isTimingsExpanded) 180f else 0f,
                label = "chevronRotation"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with toggle chevron
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTimingsExpanded = !isTimingsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚡ Button Behavior & Auto-Repeat",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Hold threshold, turbo repeat rate & pulse active time",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = { isTimingsExpanded = !isTimingsExpanded }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isTimingsExpanded) "Collapse" else "Expand",
                                tint = PokeAccent,
                                modifier = Modifier.rotate(chevronRotation)
                            )
                        }
                    }

                    // Collapsible Content
                    AnimatedVisibility(visible = isTimingsExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFF2C2F33), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            // 1. Auto-Repeat Toggle Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auto-Repeat on Hold (Turbo)",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Repeatedly sends taps while button is held down",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = config.isAutoRepeatEnabled,
                                    onCheckedChange = { viewModel.toggleAutoRepeatEnabled(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PokeAccent)
                                )
                            }

                            if (config.isAutoRepeatEnabled) {
                                Spacer(modifier = Modifier.height(16.dp))

                                // Hold Threshold Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Hold Threshold (Start Delay)",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Time held before repeating begins",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = "${config.holdThresholdMs} ms",
                                        color = PokeAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Slider(
                                    value = config.holdThresholdMs.toFloat(),
                                    onValueChange = { viewModel.setHoldThresholdMs(it.toLong()) },
                                    valueRange = 150f..1000f,
                                    steps = 16,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PokeAccent,
                                        activeTrackColor = PokeAccent
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Auto-Repeat Interval Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Auto-Repeat Interval (Speed)",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Delay between consecutive repeat taps",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = "${config.repeatIntervalMs} ms (${(1000f / config.repeatIntervalMs).toInt()} taps/s)",
                                        color = PokeAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Slider(
                                    value = config.repeatIntervalMs.toFloat(),
                                    onValueChange = { viewModel.setRepeatIntervalMs(it.toLong()) },
                                    valueRange = 40f..300f,
                                    steps = 12,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PokeAccent,
                                        activeTrackColor = PokeAccent
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Press Pulse Duration (Active Touch Time)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Press Pulse Duration (Active Time)",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Touch down time before release for each simulated tap",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "${config.pulseDurationMs} ms",
                                    color = PokeAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Slider(
                                value = config.pulseDurationMs.toFloat(),
                                onValueChange = { viewModel.setPulseDurationMs(it.toLong()) },
                                valueRange = 30f..200f,
                                steps = 16,
                                colors = SliderDefaults.colors(
                                    thumbColor = PokeAccent,
                                    activeTrackColor = PokeAccent
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Live Button Feedback Tester
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val topColor = if (state.topButtonPressed) PokeGreen else Color(0xFF2C2F33)
                                val stickColor = if (state.stickClicked) PokeGreen else Color(0xFF2C2F33)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(topColor, RoundedCornerShape(8.dp))
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (state.topButtonPressed) "Top Button: PRESSED" else "Top Button: Idle",
                                        color = if (state.topButtonPressed) Color.Black else Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(stickColor, RoundedCornerShape(8.dp))
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (state.stickClicked) "Stick: CLICKED" else "Stick: Idle",
                                        color = if (state.stickClicked) Color.Black else Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
