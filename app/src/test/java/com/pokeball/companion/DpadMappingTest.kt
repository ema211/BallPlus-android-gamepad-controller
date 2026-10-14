package com.pokeball.companion

import com.pokeball.companion.data.model.DpadDirection
import com.pokeball.companion.data.model.MappingConfig
import com.pokeball.companion.data.model.StickMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DpadMappingTest {

    @Test
    fun testDpadDeadzoneReturnsNone() {
        val config = MappingConfig(stickMode = StickMode.MAPPED_DPAD)
        assertEquals(DpadDirection.NONE, config.resolveDpadDirection(0f, 0f, deadzone = 0.15f))
        assertEquals(DpadDirection.NONE, config.resolveDpadDirection(0.05f, 0.05f, deadzone = 0.15f))
        assertEquals(DpadDirection.NONE, config.resolveDpadDirection(0.14f, 0f, deadzone = 0.15f))
    }

    @Test
    fun testStrictCardinals() {
        val config = MappingConfig(stickMode = StickMode.MAPPED_DPAD, dpadDiagonalThreshold = 22.5f)
        assertEquals(DpadDirection.UP, config.resolveDpadDirection(0f, 1f))
        assertEquals(DpadDirection.DOWN, config.resolveDpadDirection(0f, -1f))
        assertEquals(DpadDirection.LEFT, config.resolveDpadDirection(-1f, 0f))
        assertEquals(DpadDirection.RIGHT, config.resolveDpadDirection(1f, 0f))
    }

    @Test
    fun testDiagonalsWithDefaultThreshold() {
        val config = MappingConfig(stickMode = StickMode.MAPPED_DPAD, dpadDiagonalThreshold = 22.5f)
        assertEquals(DpadDirection.UP_RIGHT, config.resolveDpadDirection(0.7f, 0.7f))
        assertEquals(DpadDirection.UP_LEFT, config.resolveDpadDirection(-0.7f, 0.7f))
        assertEquals(DpadDirection.DOWN_LEFT, config.resolveDpadDirection(-0.7f, -0.7f))
        assertEquals(DpadDirection.DOWN_RIGHT, config.resolveDpadDirection(0.7f, -0.7f))
    }

    @Test
    fun testStrict4WayDpadWhenThresholdIsZero() {
        val config = MappingConfig(stickMode = StickMode.MAPPED_DPAD, dpadDiagonalThreshold = 0f)

        // With 0 threshold, diagonals must never be returned
        val dir1 = config.resolveDpadDirection(0.8f, 0.6f) // ~36.8° -> RIGHT
        val dir2 = config.resolveDpadDirection(0.6f, 0.8f) // ~53.1° -> UP
        val dir3 = config.resolveDpadDirection(-0.8f, 0.6f) // ~143.1° -> LEFT
        val dir4 = config.resolveDpadDirection(0.6f, -0.8f) // ~306.9° -> DOWN

        assertEquals(DpadDirection.RIGHT, dir1)
        assertEquals(DpadDirection.UP, dir2)
        assertEquals(DpadDirection.LEFT, dir3)
        assertEquals(DpadDirection.DOWN, dir4)

        // Ensure diagonals never trigger
        assertNotEquals(DpadDirection.UP_RIGHT, dir1)
        assertNotEquals(DpadDirection.UP_LEFT, dir3)
        assertNotEquals(DpadDirection.DOWN_RIGHT, dir4)
    }

    @Test
    fun testAngularDistance() {
        assertEquals(0f, MappingConfig.angularDistance(45f, 45f), 0.001f)
        assertEquals(44f, MappingConfig.angularDistance(359f, 315f), 0.001f)
        assertEquals(46f, MappingConfig.angularDistance(1f, 315f), 0.001f)
        assertEquals(90f, MappingConfig.angularDistance(90f, 0f), 0.001f)
        assertEquals(90f, MappingConfig.angularDistance(270f, 0f), 0.001f)
    }
}
