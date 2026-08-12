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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen() {

    var houseName by remember {
        mutableStateOf("Smart Home")
    }

    var selectedRoom by remember {
        mutableStateOf<Room?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val roomList = remember {
        mutableStateListOf<Room>()
    }

    /*
     * Listen to the house name in Firebase.
     */
    LaunchedEffect(Unit) {

        FirebaseRepository.houseReference
            .child("info")
            .child("name")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {

                        houseName =
                            snapshot.getValue(String::class.java)
                                ?: "Smart Home"
                    }

                    override fun onCancelled(error: DatabaseError) {

                        errorMessage =
                            "Unable to read house information: ${error.message}"
                    }
                }
            )
    }

    /*
     * Listen continuously to floors, rooms and devices.
     *
     * This is important because if another system changes Firebase,
     * the Android app will update automatically.
     */
    LaunchedEffect(Unit) {

        FirebaseRepository.floorsReference
            .addValueEventListener(
                object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {

                        roomList.clear()

                        for (floorSnapshot in snapshot.children) {

                            val floorId =
                                floorSnapshot.key ?: ""

                            val floorName =
                                if (floorId == "floor1") {
                                    "Floor 1"
                                } else if (floorId == "floor2") {
                                    "Floor 2"
                                } else {
                                    floorId
                                }

                            val roomsSnapshot =
                                floorSnapshot.child("rooms")

                            for (roomSnapshot in roomsSnapshot.children) {

                                val devices =
                                    mutableListOf<Device>()

                                val devicesSnapshot =
                                    roomSnapshot.child("devices")

                                for (deviceSnapshot in devicesSnapshot.children) {

                                    val device =
                                        Device(
                                            id = deviceSnapshot.key ?: "",
                                            name =
                                                deviceSnapshot.child("name")
                                                    .getValue(String::class.java)
                                                    ?: "Unknown Device",

                                            powerDrawWatts =
                                                deviceSnapshot.child(
                                                    "powerDrawWatts"
                                                ).getValue(Int::class.java)
                                                    ?: 0,

                                            status =
                                                deviceSnapshot.child("status")
                                                    .getValue(String::class.java)
                                                    ?: "OFF",

                                            type =
                                                deviceSnapshot.child("type")
                                                    .getValue(String::class.java)
                                                    ?: "",

                                            maxOnDuration =
                                                deviceSnapshot.child(
                                                    "maxOnDuration"
                                                ).getValue(Long::class.java),

                                            safetyCutoff =
                                                deviceSnapshot.child(
                                                    "safetyCutoff"
                                                ).getValue(Boolean::class.java)
                                                    ?: false,

                                            turnedOnAt =
                                                deviceSnapshot.child(
                                                    "turnedOnAt"
                                                ).getValue(Long::class.java),

                                            turnedOffAt =
                                                deviceSnapshot.child(
                                                    "turnedOffAt"
                                                ).getValue(Long::class.java)
                                        )

                                    devices.add(device)
                                }

                                roomList.add(
                                    Room(
                                        id = roomSnapshot.key ?: "",
                                        name =
                                            roomSnapshot.child("name")
                                                .getValue(String::class.java)
                                                ?: "Unknown Room",
                                        floor = floorId,
                                        floorName = floorName,
                                        devices = devices
                                    )
                                )
                            }
                        }

                        isLoading = false
                    }

                    override fun onCancelled(error: DatabaseError) {

                        isLoading = false

                        errorMessage =
                            "Unable to read rooms: ${error.message}"
                    }
                }
            )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = houseName,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->

        if (isLoading) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {

                CircularProgressIndicator()
            }

        } else if (selectedRoom != null) {

            RoomScreen(
                room = selectedRoom!!,
                onBack = {
                    selectedRoom = null
                }
            )

        } else {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {

                item {

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Text(
                        text = "Welcome Home 👋",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "Select a room to control your devices.",
                        fontSize = 16.sp
                    )

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )
                }

                items(
                    items = roomList,
                    key = { room -> room.id }
                ) { room ->

                    RoomCard(
                        room = room,
                        onClick = {
                            selectedRoom = room
                        }
                    )
                }

                item {

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    Text(
                        text = "Connected to shared Firebase",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )
                }
            }
        }

        errorMessage?.let { message ->

            AlertDialog(
                onDismissRequest = {
                    errorMessage = null
                },
                title = {
                    Text("Firebase Error")
                },
                text = {
                    Text(message)
                },
                confirmButton = {

                    TextButton(
                        onClick = {
                            errorMessage = null
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}


@Composable
fun RoomCard(
    room: Room,
    onClick: () -> Unit
) {

    val activeDevices =
        room.devices.count {
            it.status == "ON"
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .clickable {
                onClick()
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = room.name,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = room.floorName,
                color = Color.Gray
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text =
                    "${room.devices.size} devices • " +
                            "$activeDevices active",
                fontSize = 14.sp
            )
        }
    }
}


@Composable
fun RoomScreen(
    room: Room,
    onBack: () -> Unit
) {

    var devices by remember(room.id) {
        mutableStateOf(room.devices)
    }

    /*
     * Re-read this room continuously.
     *
     * This means backend/simulator changes will appear
     * in the Android app automatically.
     */
    LaunchedEffect(room.id) {

        FirebaseRepository.floorsReference
            .child(room.floor)
            .child("rooms")
            .child(room.id)
            .child("devices")
            .addValueEventListener(
                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        val updatedDevices =
                            mutableListOf<Device>()

                        for (deviceSnapshot in snapshot.children) {

                            updatedDevices.add(
                                Device(
                                    id = deviceSnapshot.key ?: "",
                                    name =
                                        deviceSnapshot.child("name")
                                            .getValue(String::class.java)
                                            ?: "Unknown Device",

                                    powerDrawWatts =
                                        deviceSnapshot.child(
                                            "powerDrawWatts"
                                        ).getValue(Int::class.java)
                                            ?: 0,

                                    status =
                                        deviceSnapshot.child("status")
                                            .getValue(String::class.java)
                                            ?: "OFF",

                                    type =
                                        deviceSnapshot.child("type")
                                            .getValue(String::class.java)
                                            ?: "",

                                    maxOnDuration =
                                        deviceSnapshot.child(
                                            "maxOnDuration"
                                        ).getValue(Long::class.java),

                                    safetyCutoff =
                                        deviceSnapshot.child(
                                            "safetyCutoff"
                                        ).getValue(Boolean::class.java)
                                            ?: false
                                )
                            )
                        }

                        devices = updatedDevices
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {
                    }
                }
            )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextButton(
                onClick = onBack
            ) {

                Text("← Back")
            }

            Text(
                text = room.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = room.floorName,
            color = Color.Gray
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        LazyColumn {

            items(
                items = devices,
                key = { device -> device.id }
            ) { device ->

                DeviceCard(
                    room = room,
                    device = device
                )
            }
        }
    }
}


@Composable
fun DeviceCard(
    room: Room,
    device: Device
) {

    var isChanging by remember(device.id) {
        mutableStateOf(false)
    }

    val isOn =
        device.status.uppercase() == "ON"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = device.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "${device.powerDrawWatts} W",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = device.status,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isChanging) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp)
                    )

                } else {

                    Switch(
                        checked = isOn,
                        onCheckedChange = { newValue ->

                            isChanging = true

                            val newStatus =
                                if (newValue) {
                                    "ON"
                                } else {
                                    "OFF"
                                }

                            FirebaseRepository.setDeviceStatusAndLog(
                                floorId = room.floor,
                                roomId = room.id,
                                deviceId = device.id,
                                deviceName = device.name,
                                newStatus = newStatus
                            )

                            // Firebase listener will update the UI.
                            isChanging = false
                        }
                    )
                }
            }

            if (device.safetyCutoff) {

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "⚠ Safety cutoff enabled",
                    color = Color(0xFFD97706),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (device.type == "heavy_appliance") {

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = "Heavy appliance",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}