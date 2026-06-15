package com.stressless.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = NavRoutes.HOME,
        label = "Inicio",
        icon = Icons.Outlined.Home
    ),
    BottomNavItem(
        route = NavRoutes.ROOM,
        label = "Habitación",
        icon = Icons.Outlined.MeetingRoom
    ),
    BottomNavItem(
        route = NavRoutes.PROFILES,
        label = "Perfiles",
        icon = Icons.Outlined.Spa
    ),
    BottomNavItem(
        route = NavRoutes.HISTORY,
        label = "Historial",
        icon = Icons.Outlined.BarChart
    )
)