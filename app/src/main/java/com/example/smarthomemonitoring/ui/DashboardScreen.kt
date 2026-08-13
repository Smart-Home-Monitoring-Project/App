package com.example.smarthomemonitoring.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomemonitoring.FirebaseRepository
import com.example.smarthomemonitoring.model.DashboardStats
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onOpenFloorPlan: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {

    var stats by remember {
        mutableStateOf(DashboardStats())
    }

    var houseName by remember {
        mutableStateOf("Smart Home")
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {

        /*
         * Read house name.
         */
        FirebaseRepository.houseReference
            .child("info")
            .child("name")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {
                        houseName =
                            snapshot.getValue(String::class.java)
                                ?: "Smart Home"
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {
                    }
                }
            )

        /*
         * Listen to all floors and devices.
         */
        FirebaseRepository.floorsReference
            .addValueEventListener(
                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        var totalDevices = 0
                        var activeDevices = 0
                        var totalRooms = 0
                        var currentPower = 0

                        /*
                         * Main circuit breaker state.
                         *
                         * r4-breaker is the master breaker
                         * for the whole house.
                         */
                        var mainBreakerOn = true

                        /*
                         * First find the main breaker.
                         */
                        for (floorSnapshot in snapshot.children) {

                            val roomsSnapshot =
                                floorSnapshot.child("rooms")

                            for (roomSnapshot in roomsSnapshot.children) {

                                val devicesSnapshot =
                                    roomSnapshot.child("devices")

                                for (deviceSnapshot in devicesSnapshot.children) {

                                    val deviceId =
                                        deviceSnapshot.key ?: ""

                                    val deviceType =
                                        deviceSnapshot
                                            .child("type")
                                            .getValue(String::class.java)
                                            ?: ""

                                    if (
                                        deviceId == "r4-breaker" ||
                                        deviceType == "main_breaker"
                                    ) {

                                        val breakerStatus =
                                            deviceSnapshot
                                                .child("status")
                                                .getValue(String::class.java)
                                                ?: "OFF"

                                        mainBreakerOn =
                                            breakerStatus.uppercase() == "ON"
                                    }
                                }
                            }
                        }

                        /*
                         * Now calculate the dashboard values.
                         */
                        for (floorSnapshot in snapshot.children) {

                            val roomsSnapshot =
                                floorSnapshot.child("rooms")

                            for (roomSnapshot in roomsSnapshot.children) {

                                totalRooms++

                                val devicesSnapshot =
                                    roomSnapshot.child("devices")

                                for (deviceSnapshot in devicesSnapshot.children) {

                                    val deviceId =
                                        deviceSnapshot.key ?: ""

                                    val type =
                                        deviceSnapshot
                                            .child("type")
                                            .getValue(String::class.java)
                                            ?: ""

                                    /*
                                     * Main breaker itself is not counted
                                     * as a normal household device.
                                     */
                                    if (
                                        deviceId == "r4-breaker" ||
                                        type == "main_breaker"
                                    ) {
                                        continue
                                    }

                                    totalDevices++

                                    val status =
                                        deviceSnapshot
                                            .child("status")
                                            .getValue(String::class.java)
                                            ?: "OFF"

                                    val power =
                                        deviceSnapshot
                                            .child("powerDrawWatts")
                                            .getValue(Int::class.java)
                                            ?: 0

                                    /*
                                     * If the main breaker is OFF,
                                     * the whole house has no active
                                     * powered devices and consumes 0 W.
                                     */
                                    if (mainBreakerOn) {

                                        if (status.uppercase() == "ON") {

                                            activeDevices++

                                            /*
                                             * Multi-switch is a controller.
                                             * Its own 8 W controller value is
                                             * not counted as appliance usage.
                                             */
                                            if (type != "multi_switch") {
                                                currentPower += power
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        /*
                         * Explicitly force zero when the main
                         * circuit breaker is OFF.
                         */
                        if (!mainBreakerOn) {

                            activeDevices = 0
                            currentPower = 0
                        }

                        stats =
                            DashboardStats(
                                totalDevices = totalDevices,
                                activeDevices = activeDevices,
                                totalRooms = totalRooms,
                                currentPowerWatts = currentPower
                            )

                        isLoading = false
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        isLoading = false
                    }
                }
            )
    }

    if (isLoading) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            CircularProgressIndicator()

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text("Loading home data...")
        }

        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        item {

            Text(
                text = houseName,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Smart Home Dashboard",
                fontSize = 16.sp
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }

        item {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "Current Power Usage",
                        fontSize = 16.sp
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "${stats.currentPowerWatts} W",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Power currently being consumed"
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }

        item {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                StatisticCard(
                    title = "Devices",
                    value = stats.totalDevices.toString(),
                    modifier = Modifier.weight(1f)
                )

                StatisticCard(
                    title = "Active",
                    value = stats.activeDevices.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        item {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                StatisticCard(
                    title = "Locations",
                    value = stats.totalRooms.toString(),
                    modifier = Modifier.weight(1f)
                )

                StatisticCard(
                    title = "Inactive",
                    value =
                        (
                                stats.totalDevices -
                                        stats.activeDevices
                                ).toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }

        item {

            Text(
                text = "Home Overview",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "Firebase Connection",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "● Connected"
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text =
                            "Live data is being received from the shared smart-home database."
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }

        /*
         * Floor plan button.
         */
        item {

            androidx.compose.material3.Button(
                onClick = onOpenFloorPlan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("VIEW FLOOR PLANS")
            }
        }
    }
}


@Composable
private fun StatisticCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = title,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}