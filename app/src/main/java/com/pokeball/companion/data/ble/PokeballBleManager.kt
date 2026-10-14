package com.pokeball.companion.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.pokeball.companion.data.model.JoystickCalibration
import com.pokeball.companion.data.model.PokeballState
import com.pokeball.companion.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class PokeballBleManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val tag = "PokeballBle"

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val parser = PokeballPacketParser()

    private val _state = MutableStateFlow(PokeballState())
    val state: StateFlow<PokeballState> = _state.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private var autoReconnectEnabled = true
    private var cachedMacAddress: String? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0

    fun updateCalibration(calibration: JoystickCalibration) {
        parser.calibration = calibration
    }

    fun setAutoReconnect(enabled: Boolean, mac: String? = null) {
        autoReconnectEnabled = enabled
        if (mac != null) cachedMacAddress = mac
        AppLogger.d(tag, "Auto-reconnect setting updated: enabled=$enabled, cachedMac=$mac")
    }

    fun rescan(timeoutMs: Long = 15000L) {
        startScan(timeoutMs = timeoutMs, restartIfScanning = true)
    }

    /**
     * Start BLE scanning with filters for Poké Ball Plus names.
     */
    fun startScan(timeoutMs: Long = 15000L, restartIfScanning: Boolean = false) {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            AppLogger.e(tag, "Cannot start scan: Bluetooth is disabled or unavailable")
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            AppLogger.e(tag, "BluetoothLeScanner is null")
            return
        }

        if (_isScanning.value) {
            if (restartIfScanning) {
                AppLogger.i(tag, "Rescan requested: stopping previous scan to restart...")
                stopScan()
            } else {
                AppLogger.d(tag, "Scan already in progress")
                return
            }
        }

        _discoveredDevices.value = emptyList()
        _isScanning.value = true
        AppLogger.i(tag, "Starting BLE scan (timeout: ${timeoutMs}ms, filters: ${BleConstants.DEVICE_NAME_FILTERS})")

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val deviceName = result.scanRecord?.deviceName ?: device.name ?: ""

                val isMatch = BleConstants.DEVICE_NAME_FILTERS.any { filter ->
                    deviceName.contains(filter, ignoreCase = true)
                }

                if (isMatch) {
                    val currentList = _discoveredDevices.value.toMutableList()
                    if (currentList.none { it.address == device.address }) {
                        currentList.add(device)
                        _discoveredDevices.value = currentList
                        AppLogger.i(tag, "Discovered target Poké Ball: '$deviceName' [${device.address}], RSSI: ${result.rssi}")
                    }

                    // If we have a cached MAC or want to auto-connect to the first found
                    if (cachedMacAddress != null && device.address.equals(cachedMacAddress, ignoreCase = true)) {
                        AppLogger.i(tag, "Found cached device (${device.address}). Auto-connecting immediately...")
                        stopScan()
                        connect(device)
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                val errorName = when (errorCode) {
                    SCAN_FAILED_ALREADY_STARTED -> "SCAN_FAILED_ALREADY_STARTED (1)"
                    SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "SCAN_FAILED_APP_REGISTRATION_FAILED (2)"
                    SCAN_FAILED_INTERNAL_ERROR -> "SCAN_FAILED_INTERNAL_ERROR (3)"
                    SCAN_FAILED_FEATURE_UNSUPPORTED -> "SCAN_FAILED_FEATURE_UNSUPPORTED (4)"
                    else -> "SCAN_ERROR_$errorCode"
                }
                AppLogger.e(tag, "BLE Scan failed: $errorName")
                _isScanning.value = false
            }
        }

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, scanSettings, scanCallback)
        } catch (e: Exception) {
            AppLogger.e(tag, "Exception starting BLE scan: ${e.message}", e)
            _isScanning.value = false
            return
        }

        mainHandler.postDelayed({
            if (_isScanning.value) {
                AppLogger.d(tag, "Scan timeout reached after ${timeoutMs}ms")
                stopScan()
            }
        }, timeoutMs)
    }

    fun stopScan() {
        if (!_isScanning.value) return
        try {
            scanCallback?.let {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(it)
            }
            AppLogger.d(tag, "BLE scan stopped successfully")
        } catch (e: Exception) {
            AppLogger.w(tag, "Error stopping scan: ${e.message}")
        }
        scanCallback = null
        _isScanning.value = false
    }

    /**
     * Connect to target Poké Ball Plus device.
     */
    fun connect(device: BluetoothDevice) {
        stopScan()
        reconnectJob?.cancel()
        reconnectAttempts = 0

        _state.value = _state.value.copy(
            isConnecting = true,
            deviceName = device.name ?: "Poké Ball Plus",
            deviceAddress = device.address
        )

        closeGatt()

        AppLogger.i(tag, "Initiating GATT connection to [${device.address}] (${device.name ?: "Unknown"})...")
        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
    }

    fun connectByMac(mac: String) {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            AppLogger.e(tag, "Cannot connect to MAC $mac: Bluetooth adapter is off or unavailable")
            return
        }
        try {
            val device = adapter.getRemoteDevice(mac)
            connect(device)
        } catch (e: Exception) {
            AppLogger.e(tag, "Invalid MAC address: $mac", e)
        }
    }

    fun disconnect() {
        AppLogger.i(tag, "Manual disconnect requested by user")
        autoReconnectEnabled = false
        reconnectJob?.cancel()
        closeGatt()
        _state.value = PokeballState()
    }

    private fun closeGatt() {
        bluetoothGatt?.let { gatt ->
            try {
                AppLogger.d(tag, "Disconnecting and closing GATT instance")
                gatt.disconnect()
                gatt.close()
            } catch (e: Exception) {
                AppLogger.w(tag, "Error during GATT close: ${e.message}")
            }
        }
        bluetoothGatt = null
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val stateName = when (newState) {
                BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
                BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
                BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
                BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
                else -> "STATE_$newState"
            }
            val statusStr = gattStatusToString(status)

            if (status != BluetoothGatt.GATT_SUCCESS) {
                AppLogger.e(tag, "Connection state changed with error: newState=$stateName, status=$statusStr")
            } else {
                AppLogger.i(tag, "Connection state changed: newState=$stateName, status=$statusStr")
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                reconnectAttempts = 0
                _state.value = _state.value.copy(
                    isConnected = true,
                    isConnecting = false,
                    deviceAddress = gatt.device.address,
                    deviceName = gatt.device.name ?: "Poké Ball Plus"
                )
                cachedMacAddress = gatt.device.address

                AppLogger.i(tag, "GATT connected. Triggering discoverServices()...")
                val discoveryStarted = gatt.discoverServices()
                if (!discoveryStarted) {
                    AppLogger.e(tag, "Failed to start service discovery!")
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                val wasConnected = _state.value.isConnected
                _state.value = _state.value.copy(
                    isConnected = false,
                    isConnecting = false
                )
                closeGatt()

                if (wasConnected && autoReconnectEnabled && cachedMacAddress != null) {
                    AppLogger.w(tag, "Device disconnected (reason: $statusStr). Auto-reconnect enabled, scheduling retry...")
                    scheduleReconnect()
                } else {
                    AppLogger.i(tag, "GATT disconnected (status: $statusStr). Reconnect not scheduled.")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val statusStr = gattStatusToString(status)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                AppLogger.e(tag, "Service discovery failed with status: $statusStr")
                return
            }

            AppLogger.i(tag, "Services discovered successfully! Inspecting available services:")
            for (service in gatt.services) {
                AppLogger.d(tag, " - Service: ${service.uuid} (${service.characteristics.size} characteristics)")
            }

            // 1. Read Battery Level Characteristic
            val batteryService = gatt.getService(BleConstants.BATTERY_SERVICE_UUID)
            if (batteryService != null) {
                val batteryChar = batteryService.getCharacteristic(BleConstants.BATTERY_LEVEL_CHARACTERISTIC_UUID)
                if (batteryChar != null) {
                    mainHandler.postDelayed({
                        val readSuccess = gatt.readCharacteristic(batteryChar)
                        AppLogger.d(tag, "Initiated Battery Level read: success=$readSuccess")
                    }, 300)
                } else {
                    AppLogger.w(tag, "Battery characteristic not found in battery service")
                }
            } else {
                AppLogger.w(tag, "Battery service not found on device")
            }

            // 2. Subscribe to Input Notifications
            val inputService = gatt.getService(BleConstants.INPUT_SERVICE_UUID)
            if (inputService != null) {
                val inputChar = inputService.getCharacteristic(BleConstants.INPUT_CHARACTERISTIC_UUID)
                if (inputChar != null) {
                    val notifySet = gatt.setCharacteristicNotification(inputChar, true)
                    AppLogger.i(tag, "setCharacteristicNotification(InputChar): $notifySet")

                    val descriptor = inputChar.getDescriptor(BleConstants.CCCD_DESCRIPTOR_UUID)
                    if (descriptor != null) {
                        mainHandler.postDelayed({
                            AppLogger.i(tag, "Writing CCCD descriptor to subscribe to notifications...")
                            writeCccdDescriptor(gatt, descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        }, 600)
                    } else {
                        AppLogger.e(tag, "CCCD descriptor (0x2902) not found on input characteristic!")
                    }
                } else {
                    AppLogger.e(tag, "Input characteristic (0x6675e16c...) not found in input service!")
                }
            } else {
                AppLogger.e(tag, "Input service (0x6675e16c...) not found on device!")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val statusStr = gattStatusToString(status)
            if (status == BluetoothGatt.GATT_SUCCESS &&
                characteristic.uuid == BleConstants.BATTERY_LEVEL_CHARACTERISTIC_UUID
            ) {
                val value = characteristic.value
                if (value != null && value.isNotEmpty()) {
                    val battery = value[0].toInt() and 0xFF
                    _state.value = _state.value.copy(batteryPercent = battery)
                    AppLogger.i(tag, "Battery characteristic read successfully: $battery%")
                } else {
                    AppLogger.w(tag, "Battery read success but payload was empty")
                }
            } else {
                AppLogger.w(tag, "Characteristic read: ${characteristic.uuid}, status: $statusStr")
            }
        }

        @Deprecated("Deprecated in Java for API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == BleConstants.INPUT_CHARACTERISTIC_UUID) {
                characteristic.value?.let { handlePacket(it) }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == BleConstants.INPUT_CHARACTERISTIC_UUID) {
                handlePacket(value)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            val statusStr = gattStatusToString(status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                AppLogger.i(tag, "CCCD descriptor written successfully! Input notifications are ACTIVE.")
            } else {
                AppLogger.e(tag, "Failed to write CCCD descriptor (${descriptor.uuid}): status=$statusStr")
            }
        }
    }

    private fun writeCccdDescriptor(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, value: ByteArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val res = gatt.writeDescriptor(descriptor, value)
            AppLogger.d(tag, "gatt.writeDescriptor (API 33+) result: $res")
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = value
            @Suppress("DEPRECATION")
            val res = gatt.writeDescriptor(descriptor)
            AppLogger.d(tag, "gatt.writeDescriptor (Legacy) result: $res")
        }
    }

    private fun handlePacket(data: ByteArray) {
        val parsed = parser.parse(data) ?: return
        _state.value = parsed.copy(
            deviceName = _state.value.deviceName,
            deviceAddress = _state.value.deviceAddress,
            batteryPercent = _state.value.batteryPercent
        )
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectAttempts++
        val delayMs = if (reconnectAttempts <= 3) 2000L else 5000L

        reconnectJob = scope.launch(Dispatchers.IO) {
            AppLogger.i(tag, "Auto-reconnect attempt #$reconnectAttempts scheduled in ${delayMs}ms for $cachedMacAddress...")
            delay(delayMs)
            cachedMacAddress?.let { mac ->
                connectByMac(mac)
            }
        }
    }

    private fun gattStatusToString(status: Int): String = when (status) {
        BluetoothGatt.GATT_SUCCESS -> "GATT_SUCCESS (0)"
        BluetoothGatt.GATT_READ_NOT_PERMITTED -> "GATT_READ_NOT_PERMITTED (2)"
        BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "GATT_WRITE_NOT_PERMITTED (3)"
        BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> "GATT_INSUF_AUTHENTICATION (5)"
        BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> "GATT_REQUEST_NOT_SUPPORTED (6)"
        BluetoothGatt.GATT_INVALID_OFFSET -> "GATT_INVALID_OFFSET (7)"
        BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "GATT_INSUF_ENCRYPTION (15)"
        0x08 -> "GATT_CONN_TIMEOUT (8)"
        0x13 -> "GATT_CONN_TERMINATE_PEER_USER (19 - Remote disconnected)"
        0x16 -> "GATT_CONN_TERMINATE_LOCAL_HOST (22 - Local host disconnected)"
        0x22 -> "GATT_CONN_LMP_TIMEOUT (34)"
        0x3E -> "GATT_CONN_FAIL_ESTABLISH (62)"
        0x85 -> "GATT_ERROR / 133 (Internal Android BLE stack timeout)"
        257 -> "GATT_FAILURE (257)"
        else -> "GATT_STATUS_$status (0x${Integer.toHexString(status)})"
    }

    companion object {
        @Volatile
        private var instance: PokeballBleManager? = null

        fun getInstance(context: Context): PokeballBleManager {
            return instance ?: synchronized(this) {
                instance ?: PokeballBleManager(
                    context.applicationContext,
                    CoroutineScope(SupervisorJob() + Dispatchers.Main)
                ).also { instance = it }
            }
        }
    }
}
