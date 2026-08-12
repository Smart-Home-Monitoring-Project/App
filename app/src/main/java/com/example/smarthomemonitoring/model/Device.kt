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
    val turnedOffAt: Long? = null
)
