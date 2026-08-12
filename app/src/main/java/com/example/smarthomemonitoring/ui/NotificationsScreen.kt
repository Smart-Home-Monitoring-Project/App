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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.smarthomemonitoring.model.NotificationItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    modifier: Modifier = Modifier
) {

    var notifications by remember {
        mutableStateOf<List<NotificationItem>>(emptyList())
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

                val notificationList =
                    mutableListOf<NotificationItem>()

                for (notificationSnapshot in snapshot.children) {

                    if (notificationSnapshot.key == null) {
                        continue
                    }

                    val notification =
                        NotificationItem(
                            id = notificationSnapshot.key ?: "",

                            deviceId =
                                notificationSnapshot
                                    .child("deviceId")
                                    .getValue(String::class.java)
                                    ?: "",

                            deviceName =
                                notificationSnapshot
                                    .child("deviceName")
                                    .getValue(String::class.java)
                                    ?: "",

                            floorId =
                                notificationSnapshot
                                    .child("floorId")
                                    .getValue(String::class.java)
                                    ?: "",

                            houseId =
                                notificationSnapshot
                                    .child("houseId")
                                    .getValue(String::class.java)
                                    ?: "",

                            message =
                                notificationSnapshot
                                    .child("message")
                                    .getValue(String::class.java)
                                    ?: "",

                            read =
                                notificationSnapshot
                                    .child("read")
                                    .getValue(Boolean::class.java)
                                    ?: false,

                            roomId =
                                notificationSnapshot
                                    .child("roomId")
                                    .getValue(String::class.java)
                                    ?: "",

                            timestamp =
                                notificationSnapshot
                                    .child("timestamp")
                                    .getValue(Long::class.java)
                                    ?: 0L,

                            type =
                                notificationSnapshot
                                    .child("type")
                                    .getValue(String::class.java)
                                    ?: ""
                        )

                    notificationList.add(notification)
                }

                notifications =
                    notificationList.sortedByDescending {
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

        FirebaseRepository.notificationsReference
            .addValueEventListener(listener)

        onDispose {
            FirebaseRepository.notificationsReference
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
                        text = "Loading notifications..."
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
                        text = "Unable to load notifications",
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

            notifications.isEmpty() -> {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text(
                        text = "No notifications",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "You are all caught up."
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
                            text = "Notifications",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        val unreadCount =
                            notifications.count {
                                !it.read
                            }

                        Text(
                            text =
                                if (unreadCount == 0) {
                                    "All notifications are read"
                                } else {
                                    "$unreadCount unread notification(s)"
                                }
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )
                    }

                    items(
                        items = notifications,
                        key = { it.id }
                    ) { notification ->

                        NotificationCard(
                            notification = notification
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun NotificationCard(
    notification: NotificationItem
) {

    val backgroundColor =
        if (notification.read) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.errorContainer
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(
                    text =
                        if (notification.type.isNotEmpty()) {
                            notification.type
                        } else {
                            "Notification"
                        },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text =
                        if (notification.read) {
                            "READ"
                        } else {
                            "NEW"
                        },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            if (notification.deviceName.isNotEmpty()) {

                Text(
                    text = notification.deviceName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )
            }

            Text(
                text =
                    if (notification.message.isNotEmpty()) {
                        notification.message
                    } else {
                        "No message"
                    }
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text =
                    formatNotificationTime(
                        notification.timestamp
                    ),
                fontSize = 12.sp
            )

            if (!notification.read) {

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Button(
                    onClick = {

                        FirebaseRepository
                            .notificationsReference
                            .child(notification.id)
                            .child("read")
                            .setValue(true)
                    }
                ) {

                    Text("Mark as read")
                }
            }
        }
    }
}


private fun formatNotificationTime(
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