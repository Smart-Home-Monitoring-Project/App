package com.example.smarthomemonitoring.model

data class ActivityLog(
    val id: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val floorId: String = "",
    val fromStatus: String = "",
    val houseId: String = "",
    val roomId: String = "",
    val timestamp: Long = 0L,
    val toStatus: String = "",
    val durationSeconds: Long? = null
)