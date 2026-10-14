package com.pokeball.companion.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.deviceDataStore by preferencesDataStore(name = "device_prefs")

class DeviceRepository(private val context: Context) {

    private object Keys {
        val LAST_DEVICE_MAC = stringPreferencesKey("last_device_mac")
        val LAST_DEVICE_NAME = stringPreferencesKey("last_device_name")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
    }

    val lastDeviceMac: Flow<String?> = context.deviceDataStore.data.map { prefs ->
        prefs[Keys.LAST_DEVICE_MAC]
    }

    val lastDeviceName: Flow<String?> = context.deviceDataStore.data.map { prefs ->
        prefs[Keys.LAST_DEVICE_NAME]
    }

    val autoReconnect: Flow<Boolean> = context.deviceDataStore.data.map { prefs ->
        prefs[Keys.AUTO_RECONNECT] ?: true
    }

    suspend fun saveDevice(mac: String, name: String?) {
        context.deviceDataStore.edit { prefs ->
            prefs[Keys.LAST_DEVICE_MAC] = mac
            if (name != null) {
                prefs[Keys.LAST_DEVICE_NAME] = name
            }
        }
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        context.deviceDataStore.edit { prefs ->
            prefs[Keys.AUTO_RECONNECT] = enabled
        }
    }

    suspend fun clearDevice() {
        context.deviceDataStore.edit { prefs ->
            prefs.remove(Keys.LAST_DEVICE_MAC)
            prefs.remove(Keys.LAST_DEVICE_NAME)
        }
    }
}
