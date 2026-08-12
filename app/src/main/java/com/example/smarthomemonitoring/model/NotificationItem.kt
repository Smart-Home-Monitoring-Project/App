package com.example.smarthomemonitoring.model

data class NotificationItem(
    val id: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val floorId: String = "",
    val houseId: String = "",
    val message: String = "",
    val read: Boolean = false,
    val roomId: String = "",
    val timestamp: Long = 0L,
    val type: String = ""
)