package com.pokeball.companion.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

data class LogEntry(
    val timestamp: Long,
    val tag: String,
    val level: LogLevel,
    val message: String
) {
    val formattedTime: String by lazy {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
    }

    override fun toString(): String {
        return "$formattedTime [${level.name}] $tag: $message"
    }
}

object AppLogger {

    @Volatile
    var isLoggingEnabled: Boolean = false

    private const val MAX_LOGS = 500
    private val buffer = ArrayDeque<LogEntry>(MAX_LOGS)
    private val lock = Any()

    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    fun log(level: LogLevel, tag: String, message: String) {
        if (!isLoggingEnabled) return

        // Print to logcat (safely catch RuntimeException for JVM test runner)
        try {
            when (level) {
                LogLevel.DEBUG -> Log.d(tag, message)
                LogLevel.INFO -> Log.i(tag, message)
                LogLevel.WARN -> Log.w(tag, message)
                LogLevel.ERROR -> Log.e(tag, message)
            }
        } catch (_: RuntimeException) {
            // Ignored in JVM unit test environment
        }

        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            level = level,
            message = message
        )

        val snapshot: List<LogEntry>
        synchronized(lock) {
            if (buffer.size >= MAX_LOGS) {
                buffer.removeFirst()
            }
            buffer.addLast(entry)
            snapshot = buffer.toList()
        }

        _logsFlow.value = snapshot
    }

    inline fun d(tag: String, message: () -> String) {
        if (isLoggingEnabled) log(LogLevel.DEBUG, tag, message())
    }

    fun d(tag: String, message: String) {
        if (isLoggingEnabled) log(LogLevel.DEBUG, tag, message)
    }

    inline fun i(tag: String, message: () -> String) {
        if (isLoggingEnabled) log(LogLevel.INFO, tag, message())
    }

    fun i(tag: String, message: String) {
        if (isLoggingEnabled) log(LogLevel.INFO, tag, message)
    }

    inline fun w(tag: String, message: () -> String) {
        if (isLoggingEnabled) log(LogLevel.WARN, tag, message())
    }

    fun w(tag: String, message: String) {
        if (isLoggingEnabled) log(LogLevel.WARN, tag, message)
    }

    inline fun e(tag: String, throwable: Throwable? = null, message: () -> String) {
        if (isLoggingEnabled) {
            val msg = if (throwable != null) {
                "${message()}\n${Log.getStackTraceString(throwable)}"
            } else {
                message()
            }
            log(LogLevel.ERROR, tag, msg)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (isLoggingEnabled) {
            val msg = if (throwable != null) {
                "$message\n${Log.getStackTraceString(throwable)}"
            } else {
                message
            }
            log(LogLevel.ERROR, tag, msg)
        }
    }

    fun dumpLogs(): String {
        synchronized(lock) {
            if (buffer.isEmpty()) return "--- No diagnostic logs captured ---"
            return buffer.joinToString(separator = "\n") { it.toString() }
        }
    }

    fun clearLogs() {
        synchronized(lock) {
            buffer.clear()
        }
        _logsFlow.value = emptyList()
    }
}
