package com.pokeball.companion

import com.pokeball.companion.util.AppLogger
import com.pokeball.companion.util.LogLevel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AppLoggerTest {

    @Before
    fun setUp() {
        AppLogger.isLoggingEnabled = true
        AppLogger.clearLogs()
    }

    @Test
    fun testRingBufferCapacityCapsAt500() {
        for (i in 1..600) {
            AppLogger.log(LogLevel.DEBUG, "TestTag", "Message #$i")
        }

        val logs = AppLogger.logsFlow.value
        assertEquals("Buffer should cap at exactly 500 entries", 500, logs.size)
        // First entry should be #101
        assertTrue(logs.first().message.contains("#101"))
        // Last entry should be #600
        assertTrue(logs.last().message.contains("#600"))
    }

    @Test
    fun testLoggingDisabledBypassesRecording() {
        AppLogger.isLoggingEnabled = false
        AppLogger.d("TestTag") { "This should not be logged" }
        AppLogger.i("TestTag", "This also should not be logged")

        val logs = AppLogger.logsFlow.value
        assertEquals(0, logs.size)
    }

    @Test
    fun testClearLogsAndDumpLogs() {
        AppLogger.i("TagA", "Message A")
        AppLogger.w("TagB", "Message B")

        val dump = AppLogger.dumpLogs()
        assertTrue(dump.contains("[INFO] TagA: Message A"))
        assertTrue(dump.contains("[WARN] TagB: Message B"))

        AppLogger.clearLogs()
        assertEquals(0, AppLogger.logsFlow.value.size)
        assertEquals("--- No diagnostic logs captured ---", AppLogger.dumpLogs())
    }
}
