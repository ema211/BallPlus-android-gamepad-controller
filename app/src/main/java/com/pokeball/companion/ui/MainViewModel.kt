package com.pokeball.companion.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pokeball.companion.data.ble.PokeballBleManager
import com.pokeball.companion.data.model.JoystickCalibration
import com.pokeball.companion.data.model.MappingConfig
import com.pokeball.companion.data.model.PokeballState
import com.pokeball.companion.data.model.StickMode
import com.pokeball.companion.data.repository.DeviceRepository
import com.pokeball.companion.data.repository.MappingRepository
import com.pokeball.companion.service.PokeballAccessibilityService
import com.pokeball.companion.service.PokeballForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val deviceRepository = DeviceRepository(application)
    private val mappingRepository = MappingRepository(application)
    val bleManager: PokeballBleManager = PokeballBleManager.getInstance(application)

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    init {
        // Ensure foreground service is running
        PokeballForegroundService.start(application)
        refreshAccessibilityStatus()
    }

    val state: StateFlow<PokeballState> = bleManager.state

    val isScanning: StateFlow<Boolean> = bleManager.isScanning

    val discoveredDevices: StateFlow<List<BluetoothDevice>> = bleManager.discoveredDevices

    val mappingConfig: StateFlow<MappingConfig> = mappingRepository.mappingConfig.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MappingConfig()
    )

    val calibration: StateFlow<JoystickCalibration> = mappingRepository.joystickCalibration.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JoystickCalibration()
    )

    val autoReconnect: StateFlow<Boolean> = deviceRepository.autoReconnect.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun refreshAccessibilityStatus() {
        _isAccessibilityEnabled.value = PokeballAccessibilityService.isEnabled(getApplication())
    }

    fun startScan() {
        bleManager.startScan()
    }

    fun rescan() {
        bleManager.rescan()
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun connectDevice(device: BluetoothDevice) {
        bleManager.connect(device)
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    fun updateDeadzone(deadzone: Float) {
        viewModelScope.launch {
            mappingRepository.setDeadzone(deadzone)
        }
    }

    fun updateShakeSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            mappingRepository.setShakeSensitivity(sensitivity)
        }
    }

    fun toggleShakeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            mappingRepository.setShakeEnabled(enabled)
        }
    }

    fun toggleMappingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            mappingRepository.setMappingEnabled(enabled)
        }
    }

    fun toggleAutoReconnect(enabled: Boolean) {
        viewModelScope.launch {
            deviceRepository.setAutoReconnect(enabled)
        }
    }

    fun setStickMode(mode: StickMode) {
        viewModelScope.launch {
            mappingRepository.setStickMode(mode)
        }
    }

    fun setDpadDiagonalThreshold(threshold: Float) {
        viewModelScope.launch {
            mappingRepository.setDpadDiagonalThreshold(threshold)
        }
    }

    fun toggleAutoRepeatEnabled(enabled: Boolean) {
        viewModelScope.launch {
            mappingRepository.setAutoRepeatEnabled(enabled)
        }
    }

    fun setHoldThresholdMs(threshold: Long) {
        viewModelScope.launch {
            mappingRepository.setHoldThresholdMs(threshold)
        }
    }

    fun setRepeatIntervalMs(interval: Long) {
        viewModelScope.launch {
            mappingRepository.setRepeatIntervalMs(interval)
        }
    }

    fun setPulseDurationMs(duration: Long) {
        viewModelScope.launch {
            mappingRepository.setPulseDurationMs(duration)
        }
    }
}
