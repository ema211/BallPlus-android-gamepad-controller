package com.pokeball.companion.ui.screens

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokeball.companion.data.model.MappingConfig
import com.pokeball.companion.data.repository.MappingRepository
import kotlinx.coroutines.launch

enum class MappingTarget {
    TOP_BUTTON,
    STICK_CLICK,
    STICK_CENTER,
    SHAKE
}

class MappingOverlayActivity : ComponentActivity() {

    private lateinit var mappingRepository: MappingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mappingRepository = MappingRepository(applicationContext)

        setContent {
            val config by mappingRepository.mappingConfig.collectAsState(initial = MappingConfig())
            val scope = rememberCoroutineScope()
            var selectedTarget by remember { mutableStateOf<MappingTarget?>(null) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .pointerInput(selectedTarget) {
                        detectTapGestures { offset ->
                            val target = selectedTarget
                            if (target != null) {
                                scope.launch {
                                    when (target) {
                                        MappingTarget.TOP_BUTTON -> {
                                            mappingRepository.updateTopButton(offset.x, offset.y)
                                            Toast.makeText(this@MappingOverlayActivity, "Top Button point set!", Toast.LENGTH_SHORT).show()
                                        }
                                        MappingTarget.STICK_CLICK -> {
                                            mappingRepository.updateStickClick(offset.x, offset.y)
                                            Toast.makeText(this@MappingOverlayActivity, "Stick Click point set!", Toast.LENGTH_SHORT).show()
                                        }
                                        MappingTarget.STICK_CENTER -> {
                                            mappingRepository.updateStickCenter(offset.x, offset.y, config.stickRadius)
                                            Toast.makeText(this@MappingOverlayActivity, "Joystick Center set!", Toast.LENGTH_SHORT).show()
                                        }
                                        MappingTarget.SHAKE -> {
                                            mappingRepository.updateShake(offset.x, offset.y)
                                            Toast.makeText(this@MappingOverlayActivity, "Shake gesture point set!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    selectedTarget = null
                                }
                            }
                        }
                    }
            ) {
                // Draw existing mapped points on canvas
                OverlayPointsCanvas(config = config)

                // Top Floating Control Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (selectedTarget != null) {
                                "Tap on the screen where you want to place [${selectedTarget?.name}]"
                            } else {
                                "Select a control below, then tap the game screen to place it"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FilterChip(
                                selected = selectedTarget == MappingTarget.TOP_BUTTON,
                                onClick = { selectedTarget = MappingTarget.TOP_BUTTON },
                                label = { Text("Top Btn", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = selectedTarget == MappingTarget.STICK_CLICK,
                                onClick = { selectedTarget = MappingTarget.STICK_CLICK },
                                label = { Text("Click", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = selectedTarget == MappingTarget.STICK_CENTER,
                                onClick = { selectedTarget = MappingTarget.STICK_CENTER },
                                label = { Text("D-Pad Center", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = selectedTarget == MappingTarget.SHAKE,
                                onClick = { selectedTarget = MappingTarget.SHAKE },
                                label = { Text("Shake", fontSize = 12.sp) }
                            )
                        }

                        Button(
                            onClick = { finish() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Done / Close Overlay")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OverlayPointsCanvas(config: MappingConfig) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Draw Top Button Point
        if (config.topButtonTapX != null && config.topButtonTapY != null) {
            val center = Offset(config.topButtonTapX, config.topButtonTapY)
            drawCircle(Color.Red, radius = 32f, center = center)
            drawCircle(Color.White, radius = 32f, center = center, style = Stroke(width = 3f))
        }

        // Draw Stick Click Point
        if (config.stickClickTapX != null && config.stickClickTapY != null) {
            val center = Offset(config.stickClickTapX, config.stickClickTapY)
            drawCircle(Color.Cyan, radius = 32f, center = center)
            drawCircle(Color.White, radius = 32f, center = center, style = Stroke(width = 3f))
        }

        // Draw Joystick Center & Radius Boundary
        if (config.stickCenterTapX != null && config.stickCenterTapY != null) {
            val center = Offset(config.stickCenterTapX, config.stickCenterTapY)
            // Outer zone circle
            drawCircle(Color.Blue.copy(alpha = 0.2f), radius = config.stickRadius, center = center)
            drawCircle(Color.Blue, radius = config.stickRadius, center = center, style = Stroke(width = 3f))
            // Center anchor
            drawCircle(Color.Blue, radius = 24f, center = center)
        }

        // Draw Shake Point
        if (config.shakeTapX != null && config.shakeTapY != null) {
            val center = Offset(config.shakeTapX, config.shakeTapY)
            drawCircle(Color.Green, radius = 32f, center = center)
            drawCircle(Color.White, radius = 32f, center = center, style = Stroke(width = 3f))
        }
    }
}
