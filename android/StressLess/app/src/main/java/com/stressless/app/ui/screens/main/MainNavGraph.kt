package com.stressless.app.ui.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.stressless.app.navigation.NavRoutes
import com.stressless.app.ui.screens.history.HistoryRoute
import com.stressless.app.ui.screens.home.HomeRoute
import com.stressless.app.ui.screens.manual.ManualControlRoute
import com.stressless.app.ui.screens.modes.SystemModesRoute
import com.stressless.app.ui.screens.profiles.ProfilesRoute
import com.stressless.app.ui.screens.room.RoomRoute
import com.stressless.app.ui.screens.settings.SettingsRoute

@Composable
fun MainNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME,
        modifier = modifier
    ) {
        composable(NavRoutes.HOME) {
            HomeRoute(
                onGoToManualControl = {
                    navController.navigate(NavRoutes.MANUAL_CONTROL)
                },
                onGoToSystemModes = {
                    navController.navigate(NavRoutes.SYSTEM_MODES)
                }
            )
        }

        composable(NavRoutes.ROOM) {
            RoomRoute(
                onGoToManualControl = {
                    navController.navigate(NavRoutes.MANUAL_CONTROL)
                }
            )
        }

        composable(NavRoutes.PROFILES) {
            ProfilesRoute()
        }

        composable(NavRoutes.HISTORY) {
            HistoryRoute()
        }

        composable(NavRoutes.SETTINGS) {
            SettingsRoute()
        }

        composable(NavRoutes.MANUAL_CONTROL) {
            ManualControlRoute()
        }

        composable(NavRoutes.SYSTEM_MODES) {
            SystemModesRoute()
        }
    }
}