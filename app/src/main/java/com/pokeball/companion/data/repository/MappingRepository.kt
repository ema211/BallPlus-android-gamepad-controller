package com.pokeball.companion.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pokeball.companion.data.model.JoystickCalibration
import com.pokeball.companion.data.model.MappingConfig
import com.pokeball.companion.data.model.StickMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.mappingDataStore by preferencesDataStore(name = "mapping_prefs")

class MappingRepository(private val context: Context) {

    private object Keys {
        val MAPPING_ENABLED = booleanPreferencesKey("mapping_enabled")
        val TOP_X = floatPreferencesKey("top_x")
        val TOP_Y = floatPreferencesKey("top_y")
        val STICK_CLICK_X = floatPreferencesKey("stick_click_x")
        val STICK_CLICK_Y = floatPreferencesKey("stick_click_y")
        val STICK_CENTER_X = floatPreferencesKey("stick_center_x")
        val STICK_CENTER_Y = floatPreferencesKey("stick_center_y")
        val STICK_RADIUS = floatPreferencesKey("stick_radius")
        val SHAKE_X = floatPreferencesKey("shake_x")
        val SHAKE_Y = floatPreferencesKey("shake_y")
        val SHAKE_SENSITIVITY = floatPreferencesKey("shake_sensitivity")
        val SHAKE_ENABLED = booleanPreferencesKey("shake_enabled")
        val STICK_MODE = stringPreferencesKey("stick_mode")
        val DPAD_DIAGONAL_THRESHOLD = floatPreferencesKey("dpad_diagonal_threshold")
        val AUTO_REPEAT_ENABLED = booleanPreferencesKey("auto_repeat_enabled")
        val HOLD_THRESHOLD_MS = longPreferencesKey("hold_threshold_ms")
        val REPEAT_INTERVAL_MS = longPreferencesKey("repeat_interval_ms")
        val PULSE_DURATION_MS = longPreferencesKey("pulse_duration_ms")

        val CALIB_DEADZONE = floatPreferencesKey("calib_deadzone")
    }

    val mappingConfig: Flow<MappingConfig> = context.mappingDataStore.data.map { prefs ->
        MappingConfig(
            isMappingEnabled = prefs[Keys.MAPPING_ENABLED] ?: true,
            topButtonTapX = prefs[Keys.TOP_X],
            topButtonTapY = prefs[Keys.TOP_Y],
            stickClickTapX = prefs[Keys.STICK_CLICK_X],
            stickClickTapY = prefs[Keys.STICK_CLICK_Y],
            stickCenterTapX = prefs[Keys.STICK_CENTER_X],
            stickCenterTapY = prefs[Keys.STICK_CENTER_Y],
            stickRadius = prefs[Keys.STICK_RADIUS] ?: 160f,
            shakeTapX = prefs[Keys.SHAKE_X],
            shakeTapY = prefs[Keys.SHAKE_Y],
            shakeSensitivity = prefs[Keys.SHAKE_SENSITIVITY] ?: 0.25f,
            isShakeEnabled = prefs[Keys.SHAKE_ENABLED] ?: true,
            stickMode = prefs[Keys.STICK_MODE]?.let {
                try {
                    StickMode.valueOf(it)
                } catch (e: Exception) {
                    StickMode.FULL_JOYSTICK
                }
            } ?: StickMode.FULL_JOYSTICK,
            dpadDiagonalThreshold = prefs[Keys.DPAD_DIAGONAL_THRESHOLD] ?: 22.5f,
            isAutoRepeatEnabled = prefs[Keys.AUTO_REPEAT_ENABLED] ?: false,
            holdThresholdMs = prefs[Keys.HOLD_THRESHOLD_MS] ?: 400L,
            repeatIntervalMs = prefs[Keys.REPEAT_INTERVAL_MS] ?: 100L,
            pulseDurationMs = prefs[Keys.PULSE_DURATION_MS] ?: 80L
        )
    }

    val joystickCalibration: Flow<JoystickCalibration> = context.mappingDataStore.data.map { prefs ->
        JoystickCalibration(
            deadzone = prefs[Keys.CALIB_DEADZONE] ?: 0.15f
        )
    }

    suspend fun updateTopButton(x: Float, y: Float) {
        context.mappingDataStore.edit {
            it[Keys.TOP_X] = x
            it[Keys.TOP_Y] = y
        }
    }

    suspend fun updateStickClick(x: Float, y: Float) {
        context.mappingDataStore.edit {
            it[Keys.STICK_CLICK_X] = x
            it[Keys.STICK_CLICK_Y] = y
        }
    }

    suspend fun updateStickCenter(x: Float, y: Float, radius: Float = 160f) {
        context.mappingDataStore.edit {
            it[Keys.STICK_CENTER_X] = x
            it[Keys.STICK_CENTER_Y] = y
            it[Keys.STICK_RADIUS] = radius
        }
    }

    suspend fun updateShake(x: Float, y: Float) {
        context.mappingDataStore.edit {
            it[Keys.SHAKE_X] = x
            it[Keys.SHAKE_Y] = y
        }
    }

    suspend fun setMappingEnabled(enabled: Boolean) {
        context.mappingDataStore.edit {
            it[Keys.MAPPING_ENABLED] = enabled
        }
    }

    suspend fun setDeadzone(deadzone: Float) {
        context.mappingDataStore.edit {
            it[Keys.CALIB_DEADZONE] = deadzone
        }
    }

    suspend fun setShakeSensitivity(sensitivity: Float) {
        context.mappingDataStore.edit {
            it[Keys.SHAKE_SENSITIVITY] = sensitivity
        }
    }

    suspend fun setShakeEnabled(enabled: Boolean) {
        context.mappingDataStore.edit {
            it[Keys.SHAKE_ENABLED] = enabled
        }
    }

    suspend fun setStickMode(mode: StickMode) {
        context.mappingDataStore.edit {
            it[Keys.STICK_MODE] = mode.name
        }
    }

    suspend fun setDpadDiagonalThreshold(threshold: Float) {
        context.mappingDataStore.edit {
            it[Keys.DPAD_DIAGONAL_THRESHOLD] = threshold
        }
    }

    suspend fun setAutoRepeatEnabled(enabled: Boolean) {
        context.mappingDataStore.edit {
            it[Keys.AUTO_REPEAT_ENABLED] = enabled
        }
    }

    suspend fun setHoldThresholdMs(threshold: Long) {
        context.mappingDataStore.edit {
            it[Keys.HOLD_THRESHOLD_MS] = threshold
        }
    }

    suspend fun setRepeatIntervalMs(interval: Long) {
        context.mappingDataStore.edit {
            it[Keys.REPEAT_INTERVAL_MS] = interval
        }
    }

    suspend fun setPulseDurationMs(duration: Long) {
        context.mappingDataStore.edit {
            it[Keys.PULSE_DURATION_MS] = duration
        }
    }
}
