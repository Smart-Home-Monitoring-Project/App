package com.example.smarthomemonitoring.ui


import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.smarthomemonitoring.FirebaseRepository
import com.example.smarthomemonitoring.model.Device
import com.example.smarthomemonitoring.model.DeviceSchedule
import com.example.smarthomemonitoring.model.DeviceSwitch
import com.example.smarthomemonitoring.model.Room
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL


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

    val roomList =
        remember {
            mutableStateListOf<Room>()
        }


    /*
     * Read house name.
     */
    LaunchedEffect(Unit) {

        FirebaseRepository.houseReference
            .child("info")
            .child("name")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        houseName =
                            snapshot.getValue(
                                String::class.java
                            ) ?: "Smart Home"
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        errorMessage =
                            "Unable to read house information: " +
                                    error.message
                    }
                }
            )
    }


    /*
     * Realtime Firebase listener.
     */
    DisposableEffect(Unit) {

        val listener =
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    roomList.clear()

                    for (
                    floorSnapshot
                    in snapshot.children
                    ) {

                        val floorId =
                            floorSnapshot.key
                                ?: continue

                        val floorName =
                            getFloorName(floorId)


                        val roomsSnapshot =
                            floorSnapshot
                                .child("rooms")


                        for (
                        roomSnapshot
                        in roomsSnapshot.children
                        ) {

                            val roomId =
                                roomSnapshot.key
                                    ?: continue


                            /*
                             * Firebase currently doesn't contain
                             * a name for room-garden.
                             *
                             * Give it a proper display name here
                             * instead of showing "Unknown Room".
                             */
                            val roomName =
                                getRoomName(
                                    roomId = roomId,
                                    firebaseName =
                                        roomSnapshot
                                            .child("name")
                                            .getValue(
                                                String::class.java
                                            )
                                )


                            val devices =
                                roomSnapshot
                                    .child("devices")
                                    .children
                                    .map {
                                        deviceFromSnapshot(it)
                                    }


                            roomList.add(
                                Room(
                                    id =
                                        roomId,

                                    name =
                                        roomName,

                                    floor =
                                        floorId,

                                    floorName =
                                        floorName,

                                    devices =
                                        devices
                                )
                            )
                        }
                    }


                    /*
                     * Always show Floor 1 first,
                     * then Floor 2.
                     */
                    roomList.sortWith(
                        compareBy<Room> {

                            when (it.floor) {

                                "floor1" -> 1

                                "floor2" -> 2

                                else -> 99
                            }

                        }.thenBy {
                            it.name
                        }
                    )


                    isLoading = false

                    errorMessage = null
                }


                override fun onCancelled(
                    error: DatabaseError
                ) {

                    isLoading = false

                    errorMessage =
                        "Unable to read rooms: " +
                                error.message
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

    BackHandler(
        enabled = selectedRoom != null
    ) {
        selectedRoom = null
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

        when {

            isLoading -> {

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),

                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator()
                }
            }


            selectedRoom != null -> {

                RoomScreen(
                    room =
                        selectedRoom!!,

                    onBack = {
                        selectedRoom = null
                    }
                )
            }


            else -> {

                /*
                 * GROUP ROOMS BY FLOOR
                 */
                val roomsByFloor =
                    roomList.groupBy {
                        it.floor
                    }


                LazyColumn(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(
                                horizontal = 16.dp
                            )
                ) {

                    item {

                        Spacer(
                            modifier =
                                Modifier.height(16.dp)
                        )

                        Text(
                            text = "Welcome Home 👋",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                "Select a room to control your devices.",
                            fontSize = 16.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.height(20.dp)
                        )
                    }


                    /*
                     * FLOOR 1
                     */
                    val floor1Rooms =
                        roomsByFloor["floor1"]
                            .orEmpty()


                    if (floor1Rooms.isNotEmpty()) {

                        item {

                            FloorHeader(
                                title = "FLOOR 1"
                            )
                        }


                        items(

                            items = floor1Rooms,

                            key = {
                                    room ->
                                "floor1_${room.id}"
                            }

                        ) { room ->

                            RoomCard(
                                room = room,

                                onClick = {
                                    selectedRoom =
                                        room
                                }
                            )
                        }
                    }


                    /*
                     * FLOOR 2
                     */
                    val floor2Rooms =
                        roomsByFloor["floor2"]
                            .orEmpty()


                    if (floor2Rooms.isNotEmpty()) {

                        item {

                            Spacer(
                                modifier =
                                    Modifier.height(18.dp)
                            )


                            FloorHeader(
                                title = "FLOOR 2"
                            )
                        }


                        items(

                            items = floor2Rooms,

                            key = {
                                    room ->
                                "floor2_${room.id}"
                            }

                        ) { room ->

                            RoomCard(
                                room = room,

                                onClick = {
                                    selectedRoom =
                                        room
                                }
                            )
                        }
                    }


                    item {

                        Spacer(
                            modifier =
                                Modifier.height(20.dp)
                        )

                        Text(
                            text =
                                "Connected to shared Firebase",

                            fontSize = 13.sp,

                            color = Color.Gray
                        )

                        Spacer(
                            modifier =
                                Modifier.height(20.dp)
                        )
                    }
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


/*
 * Floor heading.
 */
@Composable
private fun FloorHeader(
    title: String
) {

    Text(
        text = title,

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 4.dp,
                    bottom = 8.dp
                ),

        fontSize = 20.sp,

        fontWeight = FontWeight.Bold,

        color = Color.White
    )
}


/*
 * Floor names.
 */
private fun getFloorName(
    floorId: String
): String {

    return when (floorId) {

        "floor1" ->
            "Floor 1"

        "floor2" ->
            "Floor 2"

        else ->
            floorId
                .replaceFirstChar {
                    it.uppercase()
                }
    }
}


/*
 * Room names.
 */
private fun getRoomName(
    roomId: String,
    firebaseName: String?
): String {

    /*
     * If Firebase has a proper name,
     * always use it.
     */
    if (
        !firebaseName.isNullOrBlank()
    ) {

        return firebaseName
    }


    /*
     * Garden room currently has no
     * "name" property in Firebase.
     */
    if (
        roomId == "room-garden"
    ) {

        return "Garden / Exterior"
    }


    return "Room"
}


/*
 * Firebase -> Device.
 */
private fun deviceFromSnapshot(
    snapshot: DataSnapshot
): Device {

    val scheduleSnapshot =
        snapshot.child("schedule")


    val switches =
        snapshot
            .child("switches")
            .children
            .map { switchSnapshot ->

                DeviceSwitch(

                    id =
                        switchSnapshot.key
                            ?: "",

                    name =
                        switchSnapshot
                            .child("name")
                            .getValue(
                                String::class.java
                            )
                            ?: (
                                    switchSnapshot.key
                                        ?: "Switch"
                                    ),

                    status =
                        switchSnapshot
                            .child("status")
                            .getValue(
                                String::class.java
                            )
                            ?: "OFF",

                    controlsDeviceId =
                        switchSnapshot
                            .child("controlsDeviceId")
                            .getValue(
                                String::class.java
                            )
                )
            }


    return Device(

        id =
            snapshot.key
                ?: "",

        name =
            snapshot
                .child("name")
                .getValue(
                    String::class.java
                )
                ?: "Unknown Device",

        powerDrawWatts =
            snapshot
                .child("powerDrawWatts")
                .getValue(
                    Int::class.java
                )
                ?: 0,

        status =
            snapshot
                .child("status")
                .getValue(
                    String::class.java
                )
                ?: "OFF",

        type =
            snapshot
                .child("type")
                .getValue(
                    String::class.java
                )
                ?: "",

        maxOnDuration =
            snapshot
                .child("maxOnDuration")
                .getValue(
                    Long::class.java
                ),

        safetyCutoff =
            snapshot
                .child("safetyCutoff")
                .getValue(
                    Boolean::class.java
                )
                ?: false,

        turnedOnAt =
            snapshot
                .child("turnedOnAt")
                .getValue(
                    Long::class.java
                ),

        turnedOffAt =
            snapshot
                .child("turnedOffAt")
                .getValue(
                    Long::class.java
                ),

        snapshotUri =
            snapshot
                .child("snapshotUri")
                .getValue(
                    String::class.java
                ),

        streamUri =
            snapshot
                .child("streamUri")
                .getValue(
                    String::class.java
                ),

        schedule =
            if (
                scheduleSnapshot.exists()
            ) {

                DeviceSchedule(

                    enabled =
                        scheduleSnapshot
                            .child("enabled")
                            .getValue(
                                Boolean::class.java
                            )
                            ?: false,

                    onTime =
                        scheduleSnapshot
                            .child("onTime")
                            .getValue(
                                String::class.java
                            ),

                    offTime =
                        scheduleSnapshot
                            .child("offTime")
                            .getValue(
                                String::class.java
                            )
                )

            } else {

                null
            },

        switches =
            switches
    )
}


/*
 * Room card.
 */
@Composable
private fun RoomCard(
    room: Room,
    onClick: () -> Unit
) {

    val activeDevices =
        room.devices.count {
            it.status.uppercase() == "ON"
        }


    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 6.dp
                )
                .clickable(
                    onClick = onClick
                ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(18.dp)
        ) {

            Text(
                text = room.name,

                fontSize = 21.sp,

                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(6.dp)
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


/*
 * Room device screen.
 */
@Composable
private fun RoomScreen(
    room: Room,
    onBack: () -> Unit
) {

    var devices by remember(
        room.id
    ) {
        mutableStateOf(
            room.devices
        )
    }


    var cameraDevice by remember {
        mutableStateOf<Device?>(null)
    }


    DisposableEffect(
        room.floor,
        room.id
    ) {

        val reference =
            FirebaseRepository
                .floorsReference
                .child(room.floor)
                .child("rooms")
                .child(room.id)
                .child("devices")


        val listener =
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    devices =
                        snapshot
                            .children
                            .map {
                                deviceFromSnapshot(it)
                            }
                }


                override fun onCancelled(
                    error: DatabaseError
                ) {
                }
            }


        reference.addValueEventListener(
            listener
        )


        onDispose {

            reference.removeEventListener(
                listener
            )
        }
    }


    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
    ) {

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            TextButton(
                onClick = onBack
            ) {

                Text("← Back")
            }


            Column {

                Text(
                    text = room.name,

                    fontSize = 23.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text = room.floorName,

                    color = Color.Gray,

                    fontSize = 13.sp
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        LazyColumn {

            items(

                items = devices,

                key = {
                        device ->
                    device.id
                }

            ) { device ->

                DeviceCard(

                    room = room,

                    device = device,

                    onViewCamera = {
                        cameraDevice = it
                    }
                )
            }
        }
    }


    cameraDevice?.let { device ->

        CameraDialog(

            device = device,

            onDismiss = {
                cameraDevice = null
            }
        )
    }
}


/*
 * Device card.
 */
@Composable
private fun DeviceCard(
    room: Room,
    device: Device,
    onViewCamera: (Device) -> Unit
) {

    /*
     * IMPORTANT:
     *
     * A multi_switch is a controller ONLY
     * when it actually contains switches.
     *
     * r5-multiswitch -> 3 switches
     *
     * r1-msw-1 -> no switches
     * r1-msw-2 -> no switches
     * r1-msw-3 -> no switches
     *
     * Therefore the latter are treated as
     * normal devices.
     */
    val isRealMultiSwitch =
        device.type == "multi_switch" &&
                device.switches.isNotEmpty()


    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 7.dp
                )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = device.name,

                        fontSize = 18.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )


                    Text(
                        text =
                            if (isRealMultiSwitch) {
                                "Controller power: ${device.powerDrawWatts} W"
                            } else {
                                "${device.powerDrawWatts} W"
                            },

                        fontSize = 14.sp,

                        color = Color.Gray
                    )


                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )


                    Text(
                        text =
                            device.status.uppercase(),

                        fontSize = 13.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            statusColor(
                                device.status
                            )
                    )
                }


                /*
                 * REAL multi-switch:
                 * don't show a general ON/OFF switch.
                 */
                if (
                    isRealMultiSwitch
                ) {

                    Text(
                        text =
                            "${device.switches.size} switches",

                        color = Color.Gray
                    )
                }

                /*
                 * Normal device.
                 *
                 * This now includes:
                 *
                 * r1-msw-1
                 * r1-msw-2
                 * r1-msw-3
                 *
                 * because they don't actually contain
                 * individual switch children.
                 */
                else if (
                    device.type !=
                    "security_camera"
                ) {

                    val status =
                        device.status.uppercase()


                    Switch(

                        checked =
                            status == "ON",

                        enabled =
                            status == "ON" ||
                                    status == "OFF",

                        onCheckedChange = {
                                newValue ->

                            FirebaseRepository
                                .setDeviceStatusAndLog(

                                    floorId =
                                        room.floor,

                                    roomId =
                                        room.id,

                                    deviceId =
                                        device.id,

                                    deviceName =
                                        device.name,

                                    newStatus =
                                        if (newValue) {
                                            "ON"
                                        } else {
                                            "OFF"
                                        }
                                )
                        }
                    )
                }
            }


            /*
             * REAL multi-switch controls.
             */
            if (
                isRealMultiSwitch
            ) {

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )


                Text(
                    text =
                        "Individually addressable switches",

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )


                device.switches.forEach {
                        deviceSwitch ->

                    MultiSwitchRow(

                        room = room,

                        multiSwitch = device,

                        deviceSwitch =
                            deviceSwitch
                    )
                }
            }


            /*
             * Iron safety information.
             */
            if (
                device.safetyCutoff
            ) {

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                Text(
                    text =
                        "⚠ Safety cutoff enabled",

                    color =
                        Color(0xFFD97706),

                    fontSize =
                        13.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }


            if (
                device.maxOnDuration != null
            ) {

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                val durationSeconds =
                    device.maxOnDuration ?: 0L

                val minutes =
                    durationSeconds / 60L

                val seconds =
                    durationSeconds % 60L

                val durationText =
                    if (seconds == 0L) {
                        "${minutes} minute(s)"
                    } else {
                        "${minutes} min ${seconds} sec"
                    }

                Text(
                    text =
                        "Maximum ON duration: $durationText",

                    color = Color.Gray,

                    fontSize = 12.sp
                )
            }


            /*
             * Automatic schedule.
             */
            device.schedule?.let {
                    schedule ->

                if (
                    schedule.enabled
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )


                    Text(
                        text = "Schedule",

                        fontWeight =
                            FontWeight.Bold,

                        fontSize = 13.sp
                    )


                    Text(
                        text =
                            "ON ${schedule.onTime ?: "--:--"}  •  " +
                                    "OFF ${schedule.offTime ?: "--:--"}",

                        fontSize = 12.sp,

                        color = Color.Gray
                    )
                }
            }


            /*
             * CCTV.
             */
            if (
                device.type ==
                "security_camera"
            ) {

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )


                Button(
                    onClick = {
                        onViewCamera(device)
                    }
                ) {

                    Text("VIEW CAMERA")
                }
            }


            /*
             * Outlet.
             */
            if (
                device.type ==
                "electrical_outlet"
            ) {

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                Text(
                    text =
                        "Electrical outlet",

                    fontSize = 12.sp,

                    color = Color.Gray
                )
            }
        }
    }
}


/*
 * Individual switch row.
 */
@Composable
private fun MultiSwitchRow(
    room: Room,
    multiSwitch: Device,
    deviceSwitch: DeviceSwitch
) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 5.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text =
                    deviceSwitch.name,

                fontWeight =
                    FontWeight.Medium
            )


            deviceSwitch.controlsDeviceId?.let {
                    controlledId ->

                Text(
                    text =
                        "Controls: $controlledId",

                    fontSize = 11.sp,

                    color = Color.Gray
                )
            }


            Text(
                text =
                    deviceSwitch.status.uppercase(),

                fontSize = 11.sp,

                color =
                    statusColor(
                        deviceSwitch.status
                    )
            )
        }


        val status =
            deviceSwitch.status.uppercase()


        Switch(

            checked =
                status == "ON",

            enabled =
                status == "ON" ||
                        status == "OFF",

            onCheckedChange = {
                    newValue ->

                val controlledId =
                    deviceSwitch
                        .controlsDeviceId
                        ?: return@Switch


                FirebaseRepository
                    .setMultiSwitchState(

                        floorId =
                            room.floor,

                        roomId =
                            room.id,

                        multiSwitchId =
                            multiSwitch.id,

                        switchId =
                            deviceSwitch.id,

                        controlledDeviceId =
                            controlledId,

                        switchName =
                            deviceSwitch.name,

                        newStatus =
                            if (newValue) {
                                "ON"
                            } else {
                                "OFF"
                            }
                    )
            }
        )
    }
}


/*
 * Device status color.
 */
private fun statusColor(
    status: String
): Color {

    return when (
        status.uppercase()
    ) {

        "ON" ->
            Color(0xFF15803D)

        "OFF" ->
            Color.Gray

        "ERROR" ->
            Color(0xFFDC2626)

        "DISCONNECTED" ->
            Color(0xFFEA580C)

        else ->
            Color.Gray
    }
}


/*
 * CCTV dialog.
 */
@Composable
private fun CameraDialog(
    device: Device,
    onDismiss: () -> Unit
) {

    var bitmap by remember(
        device.id,
        device.snapshotUri
    ) {
        mutableStateOf<
                android.graphics.Bitmap?
                >(null)
    }


    var loading by remember(
        device.id,
        device.snapshotUri
    ) {
        mutableStateOf(true)
    }


    var loadError by remember(
        device.id,
        device.snapshotUri
    ) {
        mutableStateOf<String?>(null)
    }


    LaunchedEffect(
        device.snapshotUri
    ) {

        loading = true
        loadError = null
        bitmap = null


        val rawUrl =
            device.snapshotUri
                ?.trim()
                .orEmpty()


        if (
            rawUrl.isBlank()
        ) {

            loading = false

            loadError =
                "No snapshot URI is configured."

            return@LaunchedEffect
        }


        try {

            bitmap =
                withContext(
                    Dispatchers.IO
                ) {

                    val normalized =
                        normalizeUri(
                            rawUrl
                        )


                    URL(normalized)
                        .openStream()
                        .use { input ->

                            BitmapFactory
                                .decodeStream(input)
                        }
                }


            if (
                bitmap == null
            ) {

                loadError =
                    "The snapshot could not be loaded."
            }

        } catch (
            e: Exception
        ) {

            loadError =
                e.message
                    ?: "Unable to load camera snapshot."

        } finally {

            loading = false
        }
    }


    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {
            Text(device.name)
        },

        text = {

            Column {

                Box(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(190.dp),

                    contentAlignment =
                        Alignment.Center
                ) {

                    when {

                        loading -> {

                            CircularProgressIndicator()
                        }


                        bitmap != null -> {

                            AndroidView(

                                factory = {
                                        context ->

                                    ImageView(
                                        context
                                    ).apply {

                                        scaleType =
                                            ImageView.ScaleType
                                                .CENTER_CROP
                                    }
                                },

                                update = {
                                        imageView ->

                                    imageView
                                        .setImageBitmap(
                                            bitmap
                                        )
                                },

                                modifier =
                                    Modifier
                                        .fillMaxSize()
                            )
                        }


                        else -> {

                            Text(
                                loadError
                                    ?: "No camera image available."
                            )
                        }
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                Text(
                    text =
                        "Mock camera monitor",

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                Text(
                    text =
                        "Stream URI: " +
                                normalizeUri(
                                    device.streamUri
                                        ?: "Not configured"
                                ),

                    fontSize = 11.sp
                )
            }
        },


        confirmButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text("CLOSE")
            }
        }
    )
}


/*
 * Supports both a normal URL and a Markdown-style URL.
 */
private fun normalizeUri(
    value: String
): String {

    val markdown =
        Regex(
            "^\\[(.*?)\\]\\((.*?)\\)$"
        ).find(value)


    return markdown
        ?.groupValues
        ?.getOrNull(2)
        ?: value
}