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
     * Change a normal device status.
     *
     * IMPORTANT:
     * This Android function ONLY changes the device state.
     *
     * Activity logs are NOT created here.
     *
     * The backend is responsible for creating activity logs
     * when it detects the state change in Firebase.
     *
     * The existing function name is kept so HomeScreen.kt
     * does not need to be changed.
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

                // Nothing to do if the status has not changed.
                if (oldStatus == newStatus) {
                    return@addOnSuccessListener
                }

                val currentTime =
                    System.currentTimeMillis()

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
                 *
                 * Keep the timing information because it can
                 * be used by the backend for logging/reporting.
                 */
                if (newStatus == "OFF") {

                    val turnedOnAt =
                        snapshot
                            .child("turnedOnAt")
                            .getValue(Long::class.java)

                    if (
                        turnedOnAt != null &&
                        turnedOnAt > 0L
                    ) {

                        updates["$base/turnedOnAt"] =
                            null

                        updates["$base/turnedOffAt"] =
                            currentTime
                    }
                }


                /*
                 * Write only the device state.
                 *
                 * NO Activity log is created here.
                 */
                database
                    .reference
                    .updateChildren(updates)
            }
    }


    /**
     * Control one individual switch inside a multi-switch unit.
     *
     * Android updates:
     *
     * 1. Individual switch status
     * 2. Controlled device status
     * 3. Parent multi-switch status
     *
     * Activity logging is NOT performed here.
     *
     * The backend is responsible for creating the activity log.
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
                 * Read the controlled device.
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
                         * Save ON time for the controlled device.
                         */
                        if (newStatus == "ON") {

                            updates[
                                "$targetBase/turnedOnAt"
                            ] = currentTime
                        }


                        /*
                         * Save OFF time for the controlled device.
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
                         * Update parent multi-switch status.
                         *
                         * If at least one individual switch is ON,
                         * the parent multi-switch is considered ON.
                         */
                        var anySwitchOn =
                            false

                        for (
                        child in multiSnapshot
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
                         * IMPORTANT:
                         *
                         * No Activity log is created here.
                         *
                         * The backend should detect the Firebase
                         * state change and create the single log.
                         */


                        /*
                         * Write all state changes together.
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