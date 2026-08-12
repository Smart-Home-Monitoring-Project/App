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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {

    var notificationsEnabled by remember {
        mutableStateOf(true)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    /*
     * Listen to:
     *
     * houses/house1/settings/notifications/enabled
     *
     * This means the screen stays synchronized with Firebase.
     */
    DisposableEffect(Unit) {

        val notificationsReference =
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

        notificationsReference
            .addValueEventListener(listener)

        onDispose {

            notificationsReference
                .removeEventListener(listener)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement =
            Arrangement.Top
    ) {

        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        if (isLoading) {

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                CircularProgressIndicator()

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "Loading settings..."
                )
            }

        } else {

            if (errorMessage.isNotEmpty()) {

                Text(
                    text = "Unable to load settings",
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = errorMessage
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text =
                                "Receive safety and important device alerts.",
                            fontSize = 14.sp
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text =
                                if (notificationsEnabled) {
                                    "Notifications are enabled"
                                } else {
                                    "Notifications are disabled"
                                },
                            fontSize = 13.sp
                        )
                    }

                    Switch(
                        checked = notificationsEnabled,
                        enabled = !isSaving,
                        onCheckedChange = { newValue ->

                            notificationsEnabled =
                                newValue

                            isSaving = true
                            errorMessage = ""

                            FirebaseRepository.houseReference
                                .child("settings")
                                .child("notifications")
                                .child("enabled")
                                .setValue(newValue)
                                .addOnCompleteListener {

                                    isSaving = false
                                }
                                .addOnFailureListener { error ->

                                    isSaving = false

                                    errorMessage =
                                        error.message
                                            ?: "Unable to save setting"
                                }
                        }
                    )
                }
            }

            if (isSaving) {

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "Saving..."
                )
            }

            if (errorMessage.isNotEmpty()) {

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = errorMessage
                )
            }
        }
    }
}