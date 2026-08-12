package com.example.smarthomemonitoring.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomemonitoring.FirebaseRepository
import com.example.smarthomemonitoring.model.ActivityLog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    modifier: Modifier = Modifier
) {

    var logs by remember {
        mutableStateOf<List<ActivityLog>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    DisposableEffect(Unit) {

        val listener = object : ValueEventListener {

            override fun onDataChange(
                snapshot: DataSnapshot
            ) {

                val logList =
                    mutableListOf<ActivityLog>()

                for (logSnapshot in snapshot.children) {

                    if (logSnapshot.key == "placeholder") {
                        continue
                    }

                    val log =
                        ActivityLog(
                            id =
                                logSnapshot.key ?: "",

                            deviceId =
                                logSnapshot
                                    .child("deviceId")
                                    .getValue(String::class.java)
                                    ?: "",

                            deviceName =
                                logSnapshot
                                    .child("deviceName")
                                    .getValue(String::class.java)
                                    ?: "",

                            floorId =
                                logSnapshot
                                    .child("floorId")
                                    .getValue(String::class.java)
                                    ?: "",

                            fromStatus =
                                logSnapshot
                                    .child("fromStatus")
                                    .getValue(String::class.java)
                                    ?: "",

                            houseId =
                                logSnapshot
                                    .child("houseId")
                                    .getValue(String::class.java)
                                    ?: "",

                            roomId =
                                logSnapshot
                                    .child("roomId")
                                    .getValue(String::class.java)
                                    ?: "",

                            timestamp =
                                logSnapshot
                                    .child("timestamp")
                                    .getValue(Long::class.java)
                                    ?: 0L,

                            toStatus =
                                logSnapshot
                                    .child("toStatus")
                                    .getValue(String::class.java)
                                    ?: "",

                            durationSeconds =
                                logSnapshot
                                    .child("durationSeconds")
                                    .getValue(Long::class.java)
                        )

                    logList.add(log)
                }

                logs =
                    logList.sortedByDescending {
                        it.timestamp
                    }

                isLoading = false
                errorMessage = ""
            }

            override fun onCancelled(
                error: DatabaseError
            ) {

                isLoading = false

                errorMessage =
                    error.message
            }
        }

        FirebaseRepository.logsReference
            .addValueEventListener(listener)

        onDispose {
            FirebaseRepository.logsReference
                .removeEventListener(listener)
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {

        when {

            isLoading -> {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {

                    CircularProgressIndicator()

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Loading activity..."
                    )
                }
            }

            errorMessage.isNotEmpty() -> {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text(
                        text = "Unable to load activity",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = errorMessage
                    )
                }
            }

            logs.isEmpty() -> {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text(
                        text = "No activity yet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Device activity will appear here."
                    )
                }
            }

            else -> {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    item {

                        Text(
                            text = "Activity",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "${logs.size} activity record(s)"
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )
                    }

                    items(
                        items = logs,
                        key = { it.id }
                    ) { log ->

                        ActivityLogCard(
                            log = log
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ActivityLogCard(
    log: ActivityLog
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text =
                    if (log.deviceName.isNotEmpty()) {
                        log.deviceName
                    } else {
                        "Unknown device"
                    },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column {

                    Text(
                        text = "Previous status",
                        fontSize = 12.sp
                    )

                    Text(
                        text =
                            if (log.fromStatus.isNotEmpty()) {
                                log.fromStatus
                            } else {
                                "-"
                            },
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {

                    Text(
                        text = "New status",
                        fontSize = 12.sp
                    )

                    Text(
                        text =
                            if (log.toStatus.isNotEmpty()) {
                                log.toStatus
                            } else {
                                "-"
                            },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            if (log.durationSeconds != null) {

                Text(
                    text =
                        "Duration: ${
                            formatDuration(
                                log.durationSeconds
                                    ?: 0L
                            )
                        }"
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )
            }

            Text(
                text =
                    formatActivityTime(
                        log.timestamp
                    ),
                fontSize = 12.sp
            )
        }
    }
}


private fun formatActivityTime(
    timestamp: Long
): String {

    if (timestamp <= 0L) {
        return "Unknown time"
    }

    val formatter =
        SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            Locale.getDefault()
        )

    return formatter.format(
        Date(timestamp)
    )
}


private fun formatDuration(
    seconds: Long
): String {

    if (seconds < 60) {
        return "$seconds seconds"
    }

    val minutes = seconds / 60
    val remainingSeconds = seconds % 60

    return if (remainingSeconds == 0L) {
        "$minutes minute(s)"
    } else {
        "$minutes minute(s) $remainingSeconds second(s)"
    }
}