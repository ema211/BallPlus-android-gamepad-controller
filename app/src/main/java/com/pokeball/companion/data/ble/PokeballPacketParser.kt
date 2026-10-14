package com.pokeball.companion.data.ble

import com.pokeball.companion.data.model.JoystickCalibration
import com.pokeball.companion.data.model.PokeballState
import kotlin.math.abs
import kotlin.math.hypot

class PokeballPacketParser(
    var calibration: JoystickCalibration = JoystickCalibration()
) {
    private var lastAccelX: Float = 0f
    private var lastAccelY: Float = 0f
    private var lastAccelZ: Float = 0f
    private var lastShakeTriggerTime: Long = 0L

    // Hold the triggered shake state for 140ms so games and UI reliably register it
    val shakeHoldDurationMs: Long = 140L
    val shakeCooldownMs: Long = 500L

    /**
     * Parses a raw BLE notification byte array (must be at least 12 bytes).
     */
    fun parse(data: ByteArray, currentTimeMs: Long = System.currentTimeMillis()): PokeballState? {
        if (data.size < 12) return null

        // 1. Button Mask (Byte 1)
        val buttonByte = data[1].toInt() and 0xFF
        val topPressed = (buttonByte and 0x01) != 0
        val stickClicked = (buttonByte and 0x02) != 0

        // 2. Analog Joystick Raw Parsing
        // X Axis: Nibble-reordered 16-bit value from bytes 2..3
        // Y Axis: Standard 16-bit Big-Endian value from bytes 4..5
        val rawX = parseXRaw(data[2], data[3])
        val rawY = parseYRaw(data[4], data[5])

        // 3. Normalization to [-1.0f .. 1.0f]
        val normX = normalizeAxis(rawX, calibration.minX, calibration.maxX, invert = false)
        val normY = normalizeAxis(rawY, calibration.minY, calibration.maxY, invert = true)

        // 4. Apply Radial Deadzone
        val (finalX, finalY) = applyRadialDeadzone(normX, normY, calibration.deadzone)

        // 5. Accelerometer (Last 3 bytes: Z, Y, X)
        val accelZ = (data[data.size - 3].toInt() / 127.0f).coerceIn(-2.0f, 2.0f)
        val accelY = (data[data.size - 2].toInt() / 127.0f).coerceIn(-2.0f, 2.0f)
        val accelX = (data[data.size - 1].toInt() / 127.0f).coerceIn(-2.0f, 2.0f)

        // 6. Shake Detection with 140ms pulse duration hold
        val isShake = detectShake(accelX, accelY, accelZ, currentTimeMs)

        return PokeballState(
            isConnected = true,
            isConnecting = false,
            topButtonPressed = topPressed,
            stickClicked = stickClicked,
            stickX = finalX,
            stickY = finalY,
            rawStickX = rawX,
            rawStickY = rawY,
            accelX = accelX,
            accelY = accelY,
            accelZ = accelZ,
            isShakeDetected = isShake,
            lastPacketTimestamp = currentTimeMs
        )
    }

    /**
     * X Axis: Nibble-reordered 16-bit extraction matching the Poké Ball protocol.
     */
    fun parseXRaw(b1: Byte, b2: Byte): Int {
        val b1Int = b1.toInt() and 0xFF
        val b2Int = b2.toInt() and 0xFF
        return ((b2Int and 0x0F) shl 12) or
                ((b1Int and 0xF0) shl 4) or
                (b2Int and 0xF0) or
                (b1Int and 0x0F)
    }

    /**
     * Y Axis: Standard 16-bit Big-Endian value (bytes 4..5).
     * In the hardware, Y bytes are NOT nibble-swapped like X. Swapping Y nibbles
     * shifts low-order ADC noise into high bits, causing violent jitter.
     */
    fun parseYRaw(b1: Byte, b2: Byte): Int {
        val b1Int = b1.toInt() and 0xFF
        val b2Int = b2.toInt() and 0xFF
        return (b1Int shl 8) or b2Int
    }

    // Alias for backwards compatibility
    fun parseAxisRaw(b1: Byte, b2: Byte): Int = parseXRaw(b1, b2)

    private fun normalizeAxis(raw: Int, min: Int, max: Int, invert: Boolean): Float {
        if (max <= min) return 0f
        val range = (max - min).toFloat()
        val ratio = ((raw - min).toFloat() / range).coerceIn(0f, 1f)
        val normalized = (ratio * 2f) - 1f
        return if (invert) -normalized else normalized
    }

    private fun applyRadialDeadzone(x: Float, y: Float, deadzone: Float): Pair<Float, Float> {
        val magnitude = hypot(x, y)
        if (magnitude <= deadzone) {
            return 0f to 0f
        }
        val clampedDeadzone = deadzone.coerceIn(0f, 0.99f)
        val rescale = ((magnitude - clampedDeadzone) / (1f - clampedDeadzone)).coerceIn(0f, 1f)
        val factor = rescale / magnitude
        return (x * factor).coerceIn(-1f, 1f) to (y * factor).coerceIn(-1f, 1f)
    }

    private fun detectShake(
        accelX: Float,
        accelY: Float,
        accelZ: Float,
        currentTimeMs: Long,
        shakeThreshold: Float = 0.25f
    ): Boolean {
        val deltaX = abs(accelX - lastAccelX)
        val deltaY = abs(accelY - lastAccelY)
        val deltaZ = abs(accelZ - lastAccelZ)

        lastAccelX = accelX
        lastAccelY = accelY
        lastAccelZ = accelZ

        var highDeltas = 0
        if (deltaX > shakeThreshold) highDeltas++
        if (deltaY > shakeThreshold) highDeltas++
        if (deltaZ > shakeThreshold) highDeltas++

        // If new shake motion detected outside cooldown, start the pulse
        if (highDeltas >= 2 && (currentTimeMs - lastShakeTriggerTime > shakeCooldownMs)) {
            lastShakeTriggerTime = currentTimeMs
        }

        // Return true for the duration of the pulse (140 ms)
        return (currentTimeMs - lastShakeTriggerTime) < shakeHoldDurationMs
    }
}
