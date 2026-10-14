package com.pokeball.companion.service

import com.pokeball.companion.data.model.DpadDirection
import com.pokeball.companion.data.model.MappingConfig
import com.pokeball.companion.data.model.PokeballState
import com.pokeball.companion.data.model.StickMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class InputDispatcher(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    private var wasTopPressed = false
    private var wasStickClicked = false
    private var wasShakeActive = false
    private var lastStickDispatchTime = 0L

    private var topButtonRepeatJob: Job? = null
    private var stickClickRepeatJob: Job? = null

    fun processState(state: PokeballState, config: MappingConfig) {
        if (!config.isMappingEnabled) {
            reset()
            return
        }

        if (!config.isAutoRepeatEnabled) {
            topButtonRepeatJob?.cancel()
            topButtonRepeatJob = null
            stickClickRepeatJob?.cancel()
            stickClickRepeatJob = null
        }

        val service = PokeballAccessibilityService.instance ?: return

        // 1. Top Button (Initial Press + Hold Auto-Repeat)
        val tx = config.topButtonTapX
        val ty = config.topButtonTapY
        if (state.topButtonPressed) {
            if (!wasTopPressed) {
                // Initial Tap on press edge
                if (tx != null && ty != null) {
                    service.simulateTap(tx, ty, durationMs = config.pulseDurationMs)
                    if (config.isAutoRepeatEnabled) {
                        topButtonRepeatJob?.cancel()
                        topButtonRepeatJob = scope.launch {
                            delay(config.holdThresholdMs)
                            val repeatPulse = min(config.pulseDurationMs, max(20L, config.repeatIntervalMs - 15L))
                            while (isActive) {
                                service.simulateTap(tx, ty, durationMs = repeatPulse)
                                delay(config.repeatIntervalMs)
                            }
                        }
                    }
                }
            }
        } else {
            if (wasTopPressed) {
                topButtonRepeatJob?.cancel()
                topButtonRepeatJob = null
            }
        }
        wasTopPressed = state.topButtonPressed

        // 2. Stick Click (Initial Press + Hold Auto-Repeat)
        val sx = config.stickClickTapX
        val sy = config.stickClickTapY
        if (state.stickClicked) {
            if (!wasStickClicked) {
                // Initial Tap on press edge
                if (sx != null && sy != null) {
                    service.simulateTap(sx, sy, durationMs = config.pulseDurationMs)
                    if (config.isAutoRepeatEnabled) {
                        stickClickRepeatJob?.cancel()
                        stickClickRepeatJob = scope.launch {
                            delay(config.holdThresholdMs)
                            val repeatPulse = min(config.pulseDurationMs, max(20L, config.repeatIntervalMs - 15L))
                            while (isActive) {
                                service.simulateTap(sx, sy, durationMs = repeatPulse)
                                delay(config.repeatIntervalMs)
                            }
                        }
                    }
                }
            }
        } else {
            if (wasStickClicked) {
                stickClickRepeatJob?.cancel()
                stickClickRepeatJob = null
            }
        }
        wasStickClicked = state.stickClicked

        // 3. Shake Gesture
        if (config.isShakeEnabled && state.isShakeDetected && !wasShakeActive) {
            val shx = config.shakeTapX
            val shy = config.shakeTapY
            if (shx != null && shy != null) {
                service.simulateTap(shx, shy, durationMs = config.pulseDurationMs)
            }
        }
        wasShakeActive = state.isShakeDetected

        // 4. Joystick Movement (Analog vs Mapped D-Pad)
        val centerX = config.stickCenterTapX
        val centerY = config.stickCenterTapY
        if (centerX != null && centerY != null) {
            val magnitude = hypot(state.stickX, state.stickY)
            val now = System.currentTimeMillis()

            if (magnitude > 0.15f && (now - lastStickDispatchTime > 75L)) {
                lastStickDispatchTime = now
                val targetCoordinates = if (config.stickMode == StickMode.MAPPED_DPAD) {
                    val direction = config.resolveDpadDirection(state.stickX, state.stickY, deadzone = 0.15f)
                    val r = config.stickRadius
                    val diag = r * 0.70710677f // cos(45°)
                    when (direction) {
                        DpadDirection.NONE -> null
                        DpadDirection.UP -> Pair(centerX, centerY - r)
                        DpadDirection.DOWN -> Pair(centerX, centerY + r)
                        DpadDirection.LEFT -> Pair(centerX - r, centerY)
                        DpadDirection.RIGHT -> Pair(centerX + r, centerY)
                        DpadDirection.UP_RIGHT -> Pair(centerX + diag, centerY - diag)
                        DpadDirection.UP_LEFT -> Pair(centerX - diag, centerY - diag)
                        DpadDirection.DOWN_RIGHT -> Pair(centerX + diag, centerY + diag)
                        DpadDirection.DOWN_LEFT -> Pair(centerX - diag, centerY + diag)
                    }
                } else {
                    val tx = centerX + (state.stickX * config.stickRadius)
                    val ty = centerY - (state.stickY * config.stickRadius) // invert Y for screen space
                    Pair(tx, ty)
                }

                if (targetCoordinates != null) {
                    service.simulateDrag(centerX, centerY, targetCoordinates.first, targetCoordinates.second, durationMs = 60L)
                }
            }
        }
    }

    fun reset() {
        topButtonRepeatJob?.cancel()
        topButtonRepeatJob = null
        stickClickRepeatJob?.cancel()
        stickClickRepeatJob = null
        wasTopPressed = false
        wasStickClicked = false
        wasShakeActive = false
    }
}
