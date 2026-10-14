package com.pokeball.companion.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pokeball.companion.data.model.PokeballState
import com.pokeball.companion.ui.theme.PokeAccent
import com.pokeball.companion.ui.theme.PokeGreen
import com.pokeball.companion.ui.theme.PokeRed
import com.pokeball.companion.ui.theme.PokeRedDark

@Composable
fun PokeballVisualizer(
    state: PokeballState,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp
) {
    // Smooth animated joystick displacement
    val animatedStickX by animateFloatAsState(
        targetValue = state.stickX,
        animationSpec = spring(stiffness = 800f),
        label = "stickX"
    )
    val animatedStickY by animateFloatAsState(
        targetValue = state.stickY,
        animationSpec = spring(stiffness = 800f),
        label = "stickY"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val radius = (size.toPx() / 2f) - 10f

            // 1. Top Button (Physical button at 12 o'clock)
            val topButtonColor = if (state.topButtonPressed) PokeAccent else Color(0xFF616161)
            val topButtonRadius = radius * 0.16f
            val topButtonCenter = Offset(center.x, center.y - radius + 8f)

            drawCircle(
                color = topButtonColor,
                radius = topButtonRadius,
                center = topButtonCenter
            )
            drawCircle(
                color = Color.Black,
                radius = topButtonRadius,
                center = topButtonCenter,
                style = Stroke(width = 4f)
            )

            // 2. Main Ball Outer Circle
            val ballRect = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)

            // Top Hemisphere (Red)
            val topPath = Path().apply {
                arcTo(ballRect, 180f, 180f, false)
                close()
            }
            drawPath(
                path = topPath,
                brush = Brush.verticalGradient(
                    colors = listOf(PokeRed, PokeRedDark),
                    startY = center.y - radius,
                    endY = center.y
                )
            )

            // Bottom Hemisphere (White)
            val bottomPath = Path().apply {
                arcTo(ballRect, 0f, 180f, false)
                close()
            }
            drawPath(
                path = bottomPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFE0E0E0)),
                    startY = center.y,
                    endY = center.y + radius
                )
            )

            // 3. Middle Black Equator Belt
            val beltThickness = radius * 0.14f
            drawRect(
                color = Color(0xFF212121),
                topLeft = Offset(center.x - radius, center.y - (beltThickness / 2f)),
                size = androidx.compose.ui.geometry.Size(radius * 2f, beltThickness)
            )

            // Outer Black Border
            drawCircle(
                color = Color(0xFF212121),
                radius = radius,
                center = center,
                style = Stroke(width = 8f)
            )

            // 4. Center Outer Ring
            val centerRingRadius = radius * 0.36f
            drawCircle(
                color = Color(0xFF212121),
                radius = centerRingRadius,
                center = center
            )

            // Center Middle Ring (White / LED Glow)
            val ledColor = when {
                !state.isConnected -> Color(0xFF757575)
                state.isShakeDetected -> PokeGreen
                else -> Color.White
            }
            drawCircle(
                color = ledColor,
                radius = centerRingRadius * 0.82f,
                center = center
            )

            // 5. Analog Joystick Nub (Mirrors real physical stick)
            val maxTravel = centerRingRadius * 0.45f
            val stickOffset = Offset(
                x = center.x + (animatedStickX * maxTravel),
                y = center.y - (animatedStickY * maxTravel) // Invert Y so up is up
            )
            val stickRadius = centerRingRadius * 0.52f

            val stickColor = if (state.stickClicked) PokeAccent else Color(0xFF37474F)

            // Joystick base shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = stickRadius,
                center = stickOffset.copy(y = stickOffset.y + 4f)
            )

            // Joystick Cap
            drawCircle(
                color = stickColor,
                radius = stickRadius,
                center = stickOffset
            )

            // Joystick Grip Ridge
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = stickRadius * 0.65f,
                center = stickOffset,
                style = Stroke(width = 3f)
            )
        }
    }
}
