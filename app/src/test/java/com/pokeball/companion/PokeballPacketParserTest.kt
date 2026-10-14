package com.pokeball.companion

import com.pokeball.companion.data.ble.PokeballPacketParser
import com.pokeball.companion.data.model.JoystickCalibration
import org.junit.Assert.*
import org.junit.Test

class PokeballPacketParserTest {

    private val parser = PokeballPacketParser(
        calibration = JoystickCalibration(
            minX = 7980,
            maxX = 49620,
            minY = 13350,
            maxY = 50280,
            deadzone = 0.10f
        )
    )

    @Test
    fun testParseAxisRawMatchesReferenceNibbleReordering() {
        val b1 = 0xAB.toByte()
        val b2 = 0xCD.toByte()
        val expected = 0xDACB

        val raw = parser.parseXRaw(b1, b2)
        assertEquals(expected, raw)
    }

    @Test
    fun testParseYRawDoesNotScrambleNibbles() {
        // Y bytes in the hardware are standard Big-Endian:
        // b1 = 0x7D, b2 = 0x30 -> 0x7D30 = 32048 (roughly neutral Y center)
        val b1 = 0x7D.toByte()
        val b2 = 0x30.toByte()

        val rawY = parser.parseYRaw(b1, b2)
        assertEquals(0x7D30, rawY)

        // When b2 has 1 bit of ADC noise (+1), raw value should only change by 1, NOT 4096!
        val rawYWithNoise = parser.parseYRaw(b1, (0x31).toByte())
        assertEquals(0x7D31, rawYWithNoise)
        assertEquals(1, rawYWithNoise - rawY)
    }

    @Test
    fun testButtonsMask() {
        val dummyPacket = ByteArray(14)
        // Byte 1: 0x01 (Top Button)
        dummyPacket[1] = 0x01.toByte()

        val state1 = parser.parse(dummyPacket)
        assertNotNull(state1)
        assertTrue(state1!!.topButtonPressed)
        assertFalse(state1.stickClicked)

        // Byte 1: 0x02 (Stick Click)
        dummyPacket[1] = 0x02.toByte()
        val state2 = parser.parse(dummyPacket)
        assertNotNull(state2)
        assertFalse(state2!!.topButtonPressed)
        assertTrue(state2.stickClicked)

        // Both buttons: 0x03
        dummyPacket[1] = 0x03.toByte()
        val state3 = parser.parse(dummyPacket)
        assertNotNull(state3)
        assertTrue(state3!!.topButtonPressed)
        assertTrue(state3.stickClicked)
    }

    @Test
    fun testShakeHoldPulseDuration() {
        val t0 = 1000000L
        val packet1 = ByteArray(14)
        // High accel delta
        packet1[packet1.size - 1] = 100.toByte()
        packet1[packet1.size - 2] = 100.toByte()
        packet1[packet1.size - 3] = 0.toByte()

        // First parse sets baseline
        parser.parse(packet1, t0)

        // Second parse has big change to trigger shake
        val packet2 = ByteArray(14)
        packet2[packet2.size - 1] = (-100).toByte()
        packet2[packet2.size - 2] = (-100).toByte()

        val stateTrigger = parser.parse(packet2, t0 + 20)
        assertTrue("Shake should be active at trigger", stateTrigger!!.isShakeDetected)

        // Within 140ms pulse duration, shake remains held
        val stateHeld = parser.parse(packet2, t0 + 100)
        assertTrue("Shake should remain held during pulse duration", stateHeld!!.isShakeDetected)

        // After pulse duration expires (>140ms), shake resets
        val stateExpired = parser.parse(packet2, t0 + 170)
        assertFalse("Shake should reset after pulse duration", stateExpired!!.isShakeDetected)
    }
}
