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
     * Change a normal device status and create an activity log.
     *
     * Example:
     *
     * OFF -> ON
     *
     * Firebase path:
     *
     * houses/house1/floors/{floorId}/rooms/{roomId}/devices/{deviceId}/status
     */
    fun setDeviceStatusAndLog(
        floorId: String,
        roomId: String,
        deviceId: String,
        deviceName: String,
        newStatus: String
    ) {

        val deviceReference =
            deviceReference(
                floorId = floorId,
                roomId = roomId,
                deviceId = deviceId
            )

        deviceReference
            .get()
            .addOnSuccessListener { snapshot ->

                val oldStatus =
                    snapshot
                        .child("status")
                        .getValue(String::class.java)
                        ?: "OFF"

                // Nothing to do if status did not change.
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

                val base =
                    "houses/house1/floors/$floorId/rooms/$roomId/devices/$deviceId"

                /*
                 * Update device status.
                 */
                updates["$base/status"] = newStatus

                /*
                 * Device turned ON.
                 */
                if (newStatus == "ON") {

                    updates["$base/turnedOnAt"] =
                        currentTime
                }

                /*
                 * Device turned OFF.
                 */
                var durationSeconds: Long? = null

                if (
                    newStatus == "OFF" &&
                    turnedOnAt != null &&
                    turnedOnAt > 0L
                ) {

                    durationSeconds =
                        ((currentTime - turnedOnAt) / 1000L)
                            .coerceAtLeast(0L)

                    updates["$base/turnedOnAt"] =
                        null

                    updates["$base/turnedOffAt"] =
                        currentTime
                }

                /*
                 * Create activity log.
                 */
                val logKey =
                    logsReference
                        .push()
                        .key
                        ?: return@addOnSuccessListener

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

                    logData["durationSeconds"] =
                        durationSeconds
                }

                updates[
                    "houses/house1/logs/$logKey"
                ] = logData

                /*
                 * Perform everything together.
                 */
                database
                    .reference
                    .updateChildren(updates)
            }
    }


    /**
     * Control one individual switch inside a multi-switch unit.
     *
     * Example:
     *
     * switch-1 -> r5-ceiling
     * switch-2 -> r5-stove
     * switch-3 -> r5-outlet
     *
     * Both the switch status and the controlled device status
     * are updated together.
     */
    fun setMultiSwitchState(
        floorId: String,
        roomId: String,
        multiSwitchId: String,
        switchId: String,
        controlledDeviceId: String,
        switchName: String,
        newStatus: String
    ) {

        val multiSwitchReference =
            deviceReference(
                floorId = floorId,
                roomId = roomId,
                deviceId = multiSwitchId
            )

        val targetDeviceReference =
            deviceReference(
                floorId = floorId,
                roomId = roomId,
                deviceId = controlledDeviceId
            )

        /*
         * Read the multi-switch.
         */
        multiSwitchReference
            .get()
            .addOnSuccessListener { multiSnapshot ->

                /*
                 * Read the device controlled by the switch.
                 */
                targetDeviceReference
                    .get()
                    .addOnSuccessListener { targetSnapshot ->

                        val oldTargetStatus =
                            targetSnapshot
                                .child("status")
                                .getValue(String::class.java)
                                ?: "OFF"

                        val oldSwitchStatus =
                            multiSnapshot
                                .child("switches")
                                .child(switchId)
                                .child("status")
                                .getValue(String::class.java)
                                ?: "OFF"

                        /*
                         * Nothing changed.
                         */
                        if (
                            oldSwitchStatus == newStatus &&
                            oldTargetStatus == newStatus
                        ) {
                            return@addOnSuccessListener
                        }

                        val currentTime =
                            System.currentTimeMillis()

                        val updates =
                            mutableMapOf<String, Any?>()

                        val multiBase =
                            "houses/house1/floors/$floorId/rooms/$roomId/devices/$multiSwitchId"

                        val targetBase =
                            "houses/house1/floors/$floorId/rooms/$roomId/devices/$controlledDeviceId"


                        /*
                         * Update the individual switch.
                         */
                        updates[
                            "$multiBase/switches/$switchId/status"
                        ] = newStatus


                        /*
                         * Update the actual controlled device.
                         */
                        updates[
                            "$targetBase/status"
                        ] = newStatus


                        /*
                         * Save ON time.
                         */
                        if (newStatus == "ON") {

                            updates[
                                "$targetBase/turnedOnAt"
                            ] = currentTime
                        }


                        /*
                         * Save OFF time.
                         */
                        if (newStatus == "OFF") {

                            val targetTurnedOnAt =
                                targetSnapshot
                                    .child("turnedOnAt")
                                    .getValue(Long::class.java)

                            if (
                                targetTurnedOnAt != null &&
                                targetTurnedOnAt > 0L
                            ) {

                                updates[
                                    "$targetBase/turnedOnAt"
                                ] = null

                                updates[
                                    "$targetBase/turnedOffAt"
                                ] = currentTime
                            }
                        }


                        /*
                         * Update the parent multi-switch status.
                         *
                         * If at least one switch is ON,
                         * the multi-switch is considered ON.
                         */
                        var anySwitchOn =
                            false

                        for (
                        child
                        in multiSnapshot
                            .child("switches")
                            .children
                        ) {

                            val childId =
                                child.key
                                    ?: continue

                            val childStatus =
                                if (childId == switchId) {

                                    newStatus

                                } else {

                                    child
                                        .child("status")
                                        .getValue(String::class.java)
                                        ?: "OFF"
                                }

                            if (childStatus == "ON") {

                                anySwitchOn =
                                    true

                                break
                            }
                        }

                        updates[
                            "$multiBase/status"
                        ] =
                            if (anySwitchOn) {
                                "ON"
                            } else {
                                "OFF"
                            }


                        /*
                         * Create activity log for the
                         * actual controlled device.
                         */
                        val logKey =
                            logsReference
                                .push()
                                .key

                        if (
                            logKey != null &&
                            oldTargetStatus != newStatus
                        ) {

                            val targetName =
                                targetSnapshot
                                    .child("name")
                                    .getValue(String::class.java)
                                    ?: switchName

                            val logData =
                                mutableMapOf<String, Any>(
                                    "deviceId" to controlledDeviceId,
                                    "deviceName" to targetName,
                                    "floorId" to floorId,
                                    "fromStatus" to oldTargetStatus,
                                    "houseId" to "house1",
                                    "roomId" to roomId,
                                    "timestamp" to currentTime,
                                    "toStatus" to newStatus,
                                    "controlSource" to
                                            "$multiSwitchId/$switchId"
                                )

                            /*
                             * Calculate duration when turned OFF.
                             */
                            if (newStatus == "OFF") {

                                val targetTurnedOnAt =
                                    targetSnapshot
                                        .child("turnedOnAt")
                                        .getValue(Long::class.java)

                                if (
                                    targetTurnedOnAt != null &&
                                    targetTurnedOnAt > 0L
                                ) {

                                    logData[
                                        "durationSeconds"
                                    ] =
                                        (
                                                (currentTime - targetTurnedOnAt) /
                                                        1000L
                                                )
                                            .coerceAtLeast(0L)
                                }
                            }

                            updates[
                                "houses/house1/logs/$logKey"
                            ] = logData
                        }


                        /*
                         * Write all changes together.
                         */
                        database
                            .reference
                            .updateChildren(updates)
                    }
            }
    }


    /**
     * Returns the Firebase reference for a device.
     */
    private fun deviceReference(
        floorId: String,
        roomId: String,
        deviceId: String
    ): DatabaseReference {

        return floorsReference
            .child(floorId)
            .child("rooms")
            .child(roomId)
            .child("devices")
            .child(deviceId)
    }
}