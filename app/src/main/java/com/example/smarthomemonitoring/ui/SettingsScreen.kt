package com.example.smarthomemonitoring.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {

    var notificationsEnabled by remember {
        mutableStateOf(true)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * Read notification setting from Firebase.
     */
    DisposableEffect(Unit) {

        val reference =
            FirebaseRepository.houseReference
                .child("settings")
                .child("notifications")
                .child("enabled")

        val listener =
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    notificationsEnabled =
                        snapshot.getValue(Boolean::class.java)
                            ?: true

                    isLoading = false
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    isLoading = false

                    errorMessage =
                        "Unable to read settings: ${error.message}"
                }
            }

        reference.addValueEventListener(listener)

        onDispose {
            reference.removeEventListener(listener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        /*
         * Header
         */
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
                text = "Settings",
                modifier = Modifier.weight(1f),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        if (isLoading) {

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                CircularProgressIndicator()

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text("Loading settings...")
            }

        } else {

            /*
             * Notifications
             */
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Notifications",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                if (notificationsEnabled) {
                                    "Safety and home alerts are enabled."
                                } else {
                                    "Safety and home alerts are disabled."
                                },
                            fontSize = 13.sp
                        )
                    }

                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->

                            notificationsEnabled = enabled

                            FirebaseRepository
                                .houseReference
                                .child("settings")
                                .child("notifications")
                                .child("enabled")
                                .setValue(enabled)
                        }
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            /*
             * Firebase information
             */
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "Connection",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "Firebase",
                        fontSize = 14.sp
                    )

                    Text(
                        text =
                            "Connected to the shared smart-home database.",
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            /*
             * Application information
             */
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "About",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "Smart Home Monitoring",
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Mobile application for smart-home monitoring and control.",
                        fontSize = 13.sp
                    )
                }
            }
        }

        /*
         * Error message
         */
        errorMessage?.let { message ->

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )
        }
    }
}