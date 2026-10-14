package com.pokeball.companion.data.model

data class PokeballState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val deviceName: String? = null,
    val deviceAddress: String? = null,
    val batteryPercent: Int = -1,
    val topButtonPressed: Boolean = false,
    val stickClicked: Boolean = false,
    val stickX: Float = 0f,
    val stickY: Float = 0f,
    val rawStickX: Int = 0,
    val rawStickY: Int = 0,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val isShakeDetected: Boolean = false,
    val lastPacketTimestamp: Long = 0L
)
