package com.example.smarthomemonitoring.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomemonitoring.FirebaseRepository
import com.example.smarthomemonitoring.model.Device
import com.example.smarthomemonitoring.model.Room
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

@Composable
fun FloorPlanScreen(
    modifier: Modifier = Modifier
) {

    val rooms =
        remember {
            mutableStateListOf<Room>()
        }

    var selectedFloor by remember {
        mutableStateOf("floor1")
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    DisposableEffect(Unit) {

        val listener =
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    rooms.clear()

                    for (floorSnapshot in snapshot.children) {

                        val floorId =
                            floorSnapshot.key
                                ?: continue

                        val floorName =
                            when (floorId) {

                                "floor1" -> "Floor 1"

                                "floor2" -> "Floor 2"

                                else ->
                                    floorId.replaceFirstChar {
                                        it.uppercase()
                                    }
                            }

                        val roomsSnapshot =
                            floorSnapshot.child("rooms")

                        for (roomSnapshot in roomsSnapshot.children) {

                            val roomId =
                                roomSnapshot.key
                                    ?: continue

                            val firebaseName =
                                roomSnapshot
                                    .child("name")
                                    .getValue(String::class.java)

                            val roomName =
                                if (!firebaseName.isNullOrBlank()) {
                                    firebaseName
                                } else if (
                                    roomId == "room-garden"
                                ) {
                                    "Garden / Exterior"
                                } else {
                                    "Room"
                                }

                            val devices =
                                roomSnapshot
                                    .child("devices")
                                    .children
                                    .map { deviceSnapshot ->

                                        Device(
                                            id =
                                                deviceSnapshot.key
                                                    ?: "",

                                            name =
                                                deviceSnapshot
                                                    .child("name")
                                                    .getValue(
                                                        String::class.java
                                                    )
                                                    ?: "Unknown Device",

                                            powerDrawWatts =
                                                deviceSnapshot
                                                    .child("powerDrawWatts")
                                                    .getValue(
                                                        Int::class.java
                                                    )
                                                    ?: 0,

                                            status =
                                                deviceSnapshot
                                                    .child("status")
                                                    .getValue(
                                                        String::class.java
                                                    )
                                                    ?: "OFF",

                                            type =
                                                deviceSnapshot
                                                    .child("type")
                                                    .getValue(
                                                        String::class.java
                                                    )
                                                    ?: ""
                                        )
                                    }

                            rooms.add(
                                Room(
                                    id = roomId,
                                    name = roomName,
                                    floor = floorId,
                                    floorName = floorName,
                                    devices = devices
                                )
                            )
                        }
                    }

                    if (
                        rooms.none {
                            it.floor == selectedFloor
                        }
                    ) {

                        selectedFloor =
                            rooms
                                .firstOrNull()
                                ?.floor
                                ?: "floor1"
                    }

                    isLoading = false
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    isLoading = false
                }
            }

        FirebaseRepository
            .floorsReference
            .addValueEventListener(listener)

        onDispose {

            FirebaseRepository
                .floorsReference
                .removeEventListener(listener)
        }
    }

    if (isLoading) {

        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            CircularProgressIndicator()
        }

        return
    }

    val floorRooms =
        rooms
            .filter {
                it.floor == selectedFloor
            }
            .sortedBy {
                it.name
            }

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp)
    ) {

        item {

            Text(
                text = "Floor Plans",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text =
                    "Abstract floor layout and room status",
                color = Color.Gray
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }

        item {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                FloorButton(
                    text = "Floor 1",
                    selected = selectedFloor == "floor1",
                    onClick = {
                        selectedFloor = "floor1"
                    },
                    modifier = Modifier.weight(1f)
                )

                FloorButton(
                    text = "Floor 2",
                    selected = selectedFloor == "floor2",
                    onClick = {
                        selectedFloor = "floor2"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }

        if (floorRooms.isEmpty()) {

            item {

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            "No rooms are configured for this floor.",
                        modifier =
                            Modifier.padding(20.dp)
                    )
                }
            }

        } else {

            /*
             * Abstract grid.
             *
             * Each room becomes one grid cell.
             */
            items(
                items = floorRooms,
                key = {
                    "${selectedFloor}_${it.id}"
                }
            ) { room ->

                FloorRoomCell(
                    room = room
                )
            }
        }

        item {

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text =
                    "The floor plan is an abstract grid representation. " +
                            "Room and device data are read live from Firebase.",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun FloorButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        modifier =
            modifier.clickable(
                onClick = onClick
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
            )
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text = text,
                fontWeight =
                    if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    }
            )
        }
    }
}

@Composable
private fun FloorRoomCell(
    room: Room
) {

    val activeCount =
        room.devices.count {
            it.status.uppercase() == "ON"
        }

    val errorCount =
        room.devices.count {
            val status =
                it.status.uppercase()

            status == "ERROR" ||
                    status == "DISCONNECTED"
        }

    val roomStatusColor =
        when {

            errorCount > 0 ->
                Color(0xFFDC2626)

            activeCount > 0 ->
                Color(0xFF15803D)

            else ->
                Color.Gray
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 6.dp
                ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(14.dp)
                            .clip(
                                RoundedCornerShape(50)
                            )
                            .background(
                                roomStatusColor
                            )
                )

                Spacer(
                    modifier =
                        Modifier.size(10.dp)
                )

                Text(
                    text = room.name,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    "${room.devices.size} devices",
                fontSize = 13.sp
            )

            Text(
                text =
                    "$activeCount active",
                fontSize = 13.sp,
                color = roomStatusColor
            )

            if (errorCount > 0) {

                Text(
                    text =
                        "$errorCount device(s) need attention",
                    fontSize = 12.sp,
                    color =
                        Color(0xFFDC2626)
                )
            }
        }
    }
}