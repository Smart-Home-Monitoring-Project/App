package com.example.smarthomemonitoring.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun MainScreen() {

    var currentScreen by remember {
        mutableStateOf<AppScreen>(
            AppScreen.Dashboard
        )
    }

    Scaffold(

        bottomBar = {

            NavigationBar {

                NavigationBarItem(
                    selected =
                        currentScreen ==
                                AppScreen.Dashboard,

                    onClick = {
                        currentScreen =
                            AppScreen.Dashboard
                    },

                    icon = {

                        Icon(
                            imageVector =
                                Icons.Default.Home,
                            contentDescription =
                                "Dashboard"
                        )
                    },

                    label = {
                        Text("Home")
                    }
                )

                NavigationBarItem(
                    selected =
                        currentScreen ==
                                AppScreen.Devices,

                    onClick = {
                        currentScreen =
                            AppScreen.Devices
                    },

                    icon = {

                        Icon(
                            imageVector =
                                Icons.Default.Devices,
                            contentDescription =
                                "Devices"
                        )
                    },

                    label = {
                        Text("Devices")
                    }
                )

                NavigationBarItem(
                    selected =
                        currentScreen ==
                                AppScreen.Notifications,

                    onClick = {
                        currentScreen =
                            AppScreen.Notifications
                    },

                    icon = {

                        Icon(
                            imageVector =
                                Icons.Default.Notifications,
                            contentDescription =
                                "Notifications"
                        )
                    },

                    label = {
                        Text("Alerts")
                    }
                )

                NavigationBarItem(
                    selected =
                        currentScreen ==
                                AppScreen.Activity,

                    onClick = {
                        currentScreen =
                            AppScreen.Activity
                    },

                    icon = {

                        Icon(
                            imageVector =
                                Icons.Default.List,
                            contentDescription =
                                "Activity"
                        )
                    },

                    label = {
                        Text("Activity")
                    }
                )
            }
        }

    ) { paddingValues ->

        when (currentScreen) {

            AppScreen.Dashboard -> {

                DashboardScreen(
                    modifier =
                        Modifier.padding(
                            paddingValues
                        ),

                    onOpenFloorPlan = {

                        currentScreen =
                            AppScreen.FloorPlan
                    }
                )
            }

            AppScreen.Devices -> {

                Box(
                    modifier =
                        Modifier.padding(
                            paddingValues
                        )
                ) {

                    HomeScreen()
                }
            }

            AppScreen.FloorPlan -> {

                FloorPlanScreen(
                    modifier =
                        Modifier.padding(
                            paddingValues
                        )
                )
            }

            AppScreen.Notifications -> {

                NotificationsScreen(
                    modifier =
                        Modifier.padding(
                            paddingValues
                        )
                )
            }

            AppScreen.Activity -> {

                ActivityScreen(
                    modifier =
                        Modifier.padding(
                            paddingValues
                        )
                )
            }
        }
    }
}