package com.pokeball.companion.data.ble

import java.util.UUID

object BleConstants {
    val INPUT_SERVICE_UUID: UUID = UUID.fromString("6675e16c-f36d-4567-bb55-6b51e27a23e5")
    val INPUT_CHARACTERISTIC_UUID: UUID = UUID.fromString("6675e16c-f36d-4567-bb55-6b51e27a23e6")
    val CCCD_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val BATTERY_LEVEL_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    val DEVICE_NAME_FILTERS = listOf("Pokeball", "Pokemon PBP")
}
