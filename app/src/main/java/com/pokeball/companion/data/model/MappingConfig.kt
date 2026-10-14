package com.pokeball.companion.data.model

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

enum class StickMode {
    FULL_JOYSTICK,
    MAPPED_DPAD
}

enum class DpadDirection {
    NONE,
    UP,
    DOWN,
    LEFT,
    RIGHT,
    UP_RIGHT,
    UP_LEFT,
    DOWN_RIGHT,
    DOWN_LEFT
}

data class MappingConfig(
    val isMappingEnabled: Boolean = true,
    val topButtonTapX: Float? = null,
    val topButtonTapY: Float? = null,
    val stickClickTapX: Float? = null,
    val stickClickTapY: Float? = null,
    val stickCenterTapX: Float? = null,
    val stickCenterTapY: Float? = null,
    val stickRadius: Float = 160f,
    val shakeTapX: Float? = null,
    val shakeTapY: Float? = null,
    val shakeSensitivity: Float = 0.25f,
    val isShakeEnabled: Boolean = true,
    val stickMode: StickMode = StickMode.FULL_JOYSTICK,
    val dpadDiagonalThreshold: Float = 22.5f, // Angle threshold in degrees [0f..40f]
    val isAutoRepeatEnabled: Boolean = false,
    val holdThresholdMs: Long = 400L,
    val repeatIntervalMs: Long = 100L,
    val pulseDurationMs: Long = 80L
) {
    fun resolveDpadDirection(stickX: Float, stickY: Float, deadzone: Float = 0.15f): DpadDirection {
        val magnitude = hypot(stickX, stickY)
        if (magnitude <= deadzone) return DpadDirection.NONE

        val angleRad = atan2(stickY, stickX)
        var angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
        if (angleDeg < 0f) angleDeg += 360f

        // Check 4 diagonals if threshold > 0.5f
        if (dpadDiagonalThreshold > 0.5f) {
            if (angularDistance(angleDeg, 45f) <= dpadDiagonalThreshold) return DpadDirection.UP_RIGHT
            if (angularDistance(angleDeg, 135f) <= dpadDiagonalThreshold) return DpadDirection.UP_LEFT
            if (angularDistance(angleDeg, 225f) <= dpadDiagonalThreshold) return DpadDirection.DOWN_LEFT
            if (angularDistance(angleDeg, 315f) <= dpadDiagonalThreshold) return DpadDirection.DOWN_RIGHT
        }

        // Snap to nearest Cardinal direction
        return when {
            angleDeg in 45f..135f -> DpadDirection.UP
            angleDeg in 135f..225f -> DpadDirection.LEFT
            angleDeg in 225f..315f -> DpadDirection.DOWN
            else -> DpadDirection.RIGHT
        }
    }

    companion object {
        fun angularDistance(a: Float, b: Float): Float {
            val diff = abs(a - b) % 360f
            return if (diff > 180f) 360f - diff else diff
        }
    }
}
