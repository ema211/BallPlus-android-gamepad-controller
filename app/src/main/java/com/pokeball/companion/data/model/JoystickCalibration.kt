package com.pokeball.companion.data.model

data class JoystickCalibration(
    val minX: Int = 7980,
    val maxX: Int = 49620,
    val minY: Int = 13350,
    val maxY: Int = 50280,
    val deadzone: Float = 0.15f
)
