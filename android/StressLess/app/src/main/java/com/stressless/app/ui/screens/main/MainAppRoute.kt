package com.stressless.app.ui.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController

@Composable
fun MainAppRoute(
    onLogout: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                MainEvent.LogoutCompleted -> onLogout()
            }
        }
    }

    MainScaffold(
        navController = navController,
        onLogout = viewModel::logout
    )
}