package com.example.smarthomemonitoring.ui

sealed class AppScreen(
    val route: String,
    val title: String
) {

    data object Dashboard : AppScreen(
        route = "dashboard",
        title = "Dashboard"
    )

    data object Devices : AppScreen(
        route = "devices",
        title = "Devices"
    )

    data object FloorPlan : AppScreen(
        route = "floor_plan",
        title = "Floor Plan"
    )

    data object Notifications : AppScreen(
        route = "notifications",
        title = "Notifications"
    )

    data object Activity : AppScreen(
        route = "activity",
        title = "Activity"
    )
}