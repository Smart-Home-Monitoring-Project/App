package com.example.smarthomemonitoring.model

data class DashboardStats(
    val totalDevices: Int = 0,
    val activeDevices: Int = 0,
    val totalRooms: Int = 0,
    val currentPowerWatts: Int = 0
)