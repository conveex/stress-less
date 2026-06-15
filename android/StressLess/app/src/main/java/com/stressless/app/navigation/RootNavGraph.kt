package com.stressless.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stressless.app.ui.screens.auth.login.LoginRoute
import com.stressless.app.ui.screens.auth.register.RegisterRoute
import com.stressless.app.ui.screens.main.MainAppRoute
import com.stressless.app.ui.screens.splash.SplashRoute

@Composable
fun RootNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH
    ) {
        composable(NavRoutes.SPLASH) {
            SplashRoute(
                onSessionFound = {
                    navController.navigateToHomeClearingBackStack()
                },
                onSessionMissing = {
                    navController.navigateToLoginClearingBackStack()
                }
            )
        }

        composable(NavRoutes.LOGIN) {
            LoginRoute(
                onLoginSuccess = {
                    navController.navigateToHomeClearingBackStack()
                },
                onGoToRegister = {
                    navController.navigate(NavRoutes.REGISTER)
                }
            )
        }

        composable(NavRoutes.REGISTER) {
            RegisterRoute(
                onRegisterSuccess = {
                    navController.navigateToHomeClearingBackStack()
                },
                onGoToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoutes.HOME) {
            MainAppRoute(
                onLogout = {
                    navController.navigateToLoginClearingBackStack()
                }
            )
        }
    }
}

private fun NavHostController.navigateToHomeClearingBackStack() {
    navigate(NavRoutes.HOME) {
        popUpTo(0) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToLoginClearingBackStack() {
    navigate(NavRoutes.LOGIN) {
        popUpTo(0) {
            inclusive = true
        }
        launchSingleTop = true
    }
}