package com.example.smarthomemonitoring

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseRepository {

    private const val DATABASE_URL =
        "https://smart-home-monitoring-84ea7-default-rtdb.asia-southeast1.firebasedatabase.app"

    private val database: FirebaseDatabase =
        FirebaseDatabase.getInstance(DATABASE_URL)

    val housesReference: DatabaseReference =
        database.getReference("houses")

    val houseReference: DatabaseReference =
        database.getReference("houses/house1")

    val floorsReference: DatabaseReference =
        database.getReference("houses/house1/floors")

    val notificationsReference: DatabaseReference =
        database.getReference("houses/house1/notifications")

    val logsReference: DatabaseReference =
        database.getReference("houses/house1/logs")


    /**
     * Changes a device status and creates an activity log.
     *
     * Example:
     * OFF -> ON
     *
     * This updates:
     * houses/house1/floors/{floorId}/rooms/{roomId}/devices/{deviceId}/status
     *
     * and creates:
     * houses/house1/logs/{newLogId}
     */
    fun setDeviceStatusAndLog(
        floorId: String,
        roomId: String,
        deviceId: String,
        deviceName: String,
        newStatus: String
    ) {

        val deviceReference =
            houseReference
                .child("floors")
                .child(floorId)
                .child("rooms")
                .child(roomId)
                .child("devices")
                .child(deviceId)

        deviceReference
            .get()
            .addOnSuccessListener { snapshot ->

                val oldStatus =
                    snapshot
                        .child("status")
                        .getValue(String::class.java)
                        ?: "OFF"

                // Do nothing if the status has not actually changed.
                if (oldStatus == newStatus) {
                    return@addOnSuccessListener
                }

                val currentTime =
                    System.currentTimeMillis()

                val turnedOnAt =
                    snapshot
                        .child("turnedOnAt")
                        .getValue(Long::class.java)

                val updates =
                    mutableMapOf<String, Any?>()

                // Update device status.
                updates[
                    "houses/house1/floors/$floorId/rooms/$roomId/devices/$deviceId/status"
                ] = newStatus

                // Save the time when device was turned on.
                if (newStatus == "ON") {

                    updates[
                        "houses/house1/floors/$floorId/rooms/$roomId/devices/$deviceId/turnedOnAt"
                    ] = currentTime
                }

                // Calculate duration when turning OFF.
                var durationSeconds: Long? = null

                if (
                    newStatus == "OFF" &&
                    turnedOnAt != null &&
                    turnedOnAt > 0L
                ) {

                    durationSeconds =
                        ((currentTime - turnedOnAt) / 1000L)
                            .coerceAtLeast(0L)

                    updates[
                        "houses/house1/floors/$floorId/rooms/$roomId/devices/$deviceId/turnedOnAt"
                    ] = null
                }

                // Create a new Firebase push ID.
                val logKey =
                    logsReference
                        .push()
                        .key

                if (logKey == null) {
                    return@addOnSuccessListener
                }

                val logData =
                    mutableMapOf<String, Any>(
                        "deviceId" to deviceId,
                        "deviceName" to deviceName,
                        "floorId" to floorId,
                        "fromStatus" to oldStatus,
                        "houseId" to "house1",
                        "roomId" to roomId,
                        "timestamp" to currentTime,
                        "toStatus" to newStatus
                    )

                if (durationSeconds != null) {

                    logData[
                        "durationSeconds"
                    ] = durationSeconds
                }

                // Add the new activity log.
                updates[
                    "houses/house1/logs/$logKey"
                ] = logData

                // Perform all changes together.
                database
                    .reference
                    .updateChildren(updates)
                    .addOnFailureListener {
                        // Firebase write failed.
                    }
            }
            .addOnFailureListener {
                // Firebase read failed.
            }
    }
}