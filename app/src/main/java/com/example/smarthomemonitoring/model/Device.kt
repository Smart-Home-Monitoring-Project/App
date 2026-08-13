package com.example.smarthomemonitoring.model

data class Device(
    val id: String = "",
    val name: String = "",
    val powerDrawWatts: Int = 0,
    val status: String = "OFF",
    val type: String = "",
    val maxOnDuration: Long? = null,
    val safetyCutoff: Boolean = false,
    val turnedOnAt: Long? = null,
    val turnedOffAt: Long? = null,

    // Security camera fields
    val snapshotUri: String? = null,
    val streamUri: String? = null,

    // Automatic schedule fields
    val schedule: DeviceSchedule? = null,

    // Multi-switch fields
    val switches: List<DeviceSwitch> = emptyList()
)

data class DeviceSwitch(
    val id: String = "",
    val name: String = "",
    val status: String = "OFF",
    val controlsDeviceId: String? = null
)

data class DeviceSchedule(
    val enabled: Boolean = false,
    val onTime: String? = null,
    val offTime: String? = null
)