package com.pokeball.companion

import com.pokeball.companion.data.model.MappingConfig
import com.pokeball.companion.service.InputDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ButtonTimingTest {

    @Test
    fun testDefaultButtonTimingConfig() {
        val config = MappingConfig()
        assertFalse("Auto-repeat should be disabled by default", config.isAutoRepeatEnabled)
        assertEquals(400L, config.holdThresholdMs)
        assertEquals(100L, config.repeatIntervalMs)
        assertEquals(80L, config.pulseDurationMs)
    }

    @Test
    fun testCustomButtonTimingConfig() {
        val config = MappingConfig(
            isAutoRepeatEnabled = true,
            holdThresholdMs = 250L,
            repeatIntervalMs = 60L,
            pulseDurationMs = 45L
        )
        assertTrue(config.isAutoRepeatEnabled)
        assertEquals(250L, config.holdThresholdMs)
        assertEquals(60L, config.repeatIntervalMs)
        assertEquals(45L, config.pulseDurationMs)
    }

    @Test
    fun testEffectiveRepeatPulseCalculation() {
        // When repeat interval is 50ms and pulse is 80ms, pulse must be capped to ensure release gap
        val pulse = 80L
        val intervalFast = 50L
        val effectivePulseFast = min(pulse, max(20L, intervalFast - 15L))
        assertEquals(35L, effectivePulseFast)

        // When repeat interval is 200ms and pulse is 80ms, pulse stays 80ms
        val intervalSlow = 200L
        val effectivePulseSlow = min(pulse, max(20L, intervalSlow - 15L))
        assertEquals(80L, effectivePulseSlow)
    }

    @Test
    fun testDispatcherReset() {
        val dispatcher = InputDispatcher()
        // Ensure reset executes without exceptions
        dispatcher.reset()
    }
}
