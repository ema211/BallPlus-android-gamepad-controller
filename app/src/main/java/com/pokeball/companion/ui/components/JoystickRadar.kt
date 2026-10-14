package com.pokeball.companion.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pokeball.companion.data.model.DpadDirection
import com.pokeball.companion.data.model.MappingConfig
import com.pokeball.companion.data.model.StickMode
import com.pokeball.companion.ui.theme.PokeAccent
import com.pokeball.companion.ui.theme.PokeGreen
import com.pokeball.companion.ui.theme.PokeRed

@Composable
fun JoystickRadar(
    stickX: Float,
    stickY: Float,
    deadzone: Float,
    stickMode: StickMode = StickMode.FULL_JOYSTICK,
    diagonalThreshold: Float = 22.5f,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val maxRadius = (size.toPx() / 2f) - 8f

            // Radar background circle
            drawCircle(
                color = Color(0xFF1E2124),
                radius = maxRadius,
                center = center
            )

            // Outer border
            drawCircle(
                color = Color(0xFF424242),
                radius = maxRadius,
                center = center,
                style = Stroke(width = 2f)
            )

            // Axis Crosshair Lines (Cardinals: 0°, 90°, 180°, 270°)
            drawLine(
                color = Color(0xFF424242),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 2f
            )
            drawLine(
                color = Color(0xFF424242),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 2f
            )

            // Diagonal guidelines for Mapped D-Pad Mode
            if (stickMode == StickMode.MAPPED_DPAD && diagonalThreshold > 0.5f) {
                val diagOffset = maxRadius * 0.7071f
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                drawLine(
                    color = Color(0xFF384048),
                    start = Offset(center.x - diagOffset, center.y - diagOffset),
                    end = Offset(center.x + diagOffset, center.y + diagOffset),
                    strokeWidth = 1.5f,
                    pathEffect = dashEffect
                )
                drawLine(
                    color = Color(0xFF384048),
                    start = Offset(center.x - diagOffset, center.y + diagOffset),
                    end = Offset(center.x + diagOffset, center.y - diagOffset),
                    strokeWidth = 1.5f,
                    pathEffect = dashEffect
                )
            }

            // Deadzone circle
            val deadzoneRadius = maxRadius * deadzone.coerceIn(0f, 1f)
            drawCircle(
                color = PokeRed.copy(alpha = 0.25f),
                radius = deadzoneRadius,
                center = center
            )
            drawCircle(
                color = PokeRed,
                radius = deadzoneRadius,
                center = center,
                style = Stroke(width = 2f)
            )

            // Current Joystick indicator point
            val currentPos = Offset(
                x = center.x + (stickX.coerceIn(-1f, 1f) * maxRadius),
                y = center.y - (stickY.coerceIn(-1f, 1f) * maxRadius)
            )

            if (stickMode == StickMode.MAPPED_DPAD) {
                // Dim physical stick vector & point
                drawLine(
                    color = Color.White.copy(alpha = 0.25f),
                    start = center,
                    end = currentPos,
                    strokeWidth = 1.5f
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = 6f,
                    center = currentPos
                )

                // Snapped D-Pad direction
                val tempConfig = MappingConfig(
                    stickMode = stickMode,
                    dpadDiagonalThreshold = diagonalThreshold
                )
                val dpadDir = tempConfig.resolveDpadDirection(stickX, stickY, deadzone)

                if (dpadDir != DpadDirection.NONE) {
                    val r = maxRadius * 0.85f
                    val diagR = r * 0.7071f
                    val snappedPos = when (dpadDir) {
                        DpadDirection.NONE -> center
                        DpadDirection.UP -> Offset(center.x, center.y - r)
                        DpadDirection.DOWN -> Offset(center.x, center.y + r)
                        DpadDirection.LEFT -> Offset(center.x - r, center.y)
                        DpadDirection.RIGHT -> Offset(center.x + r, center.y)
                        DpadDirection.UP_RIGHT -> Offset(center.x + diagR, center.y - diagR)
                        DpadDirection.UP_LEFT -> Offset(center.x - diagR, center.y - diagR)
                        DpadDirection.DOWN_RIGHT -> Offset(center.x + diagR, center.y + diagR)
                        DpadDirection.DOWN_LEFT -> Offset(center.x - diagR, center.y + diagR)
                    }

                    // Snapped vector line
                    drawLine(
                        color = PokeGreen,
                        start = center,
                        end = snappedPos,
                        strokeWidth = 4f
                    )

                    // Snapped position marker
                    drawCircle(
                        color = PokeGreen,
                        radius = 11f,
                        center = snappedPos
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 11f,
                        center = snappedPos,
                        style = Stroke(width = 2.5f)
                    )
                }
            } else {
                // Full Joystick Mode: Standard smooth vector and dot
                drawLine(
                    color = PokeAccent.copy(alpha = 0.6f),
                    start = center,
                    end = currentPos,
                    strokeWidth = 3f
                )

                // Joystick position dot
                drawCircle(
                    color = PokeAccent,
                    radius = 10f,
                    center = currentPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 10f,
                    center = currentPos,
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
