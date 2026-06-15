package com.stressless.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun RootNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH
    ) {
        composable(NavRoutes.SPLASH) {
            PlaceholderScreen("Splash")
        }

        composable(NavRoutes.LOGIN) {
            PlaceholderScreen("Login")
        }

        composable(NavRoutes.REGISTER) {
            PlaceholderScreen("Registro")
        }

        composable(NavRoutes.HOME) {
            PlaceholderScreen("Inicio")
        }

        composable(NavRoutes.ROOM) {
            PlaceholderScreen("Mi Habitación")
        }

        composable(NavRoutes.PROFILES) {
            PlaceholderScreen("Perfiles")
        }

        composable(NavRoutes.HISTORY) {
            PlaceholderScreen("Historial")
        }

        composable(NavRoutes.SETTINGS) {
            PlaceholderScreen("Configuración")
        }

        composable(NavRoutes.MANUAL_CONTROL) {
            PlaceholderScreen("Control Manual")
        }

        composable(NavRoutes.SYSTEM_MODES) {
            PlaceholderScreen("Modos del Sistema")
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title)
    }
}