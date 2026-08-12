package com.example.smarthomemonitoring.model

data class Room(
    val id: String = "",
    val name: String = "",
    val floor: String = "",
    val floorName: String = "",
    val devices: List<Device> = emptyList()
)