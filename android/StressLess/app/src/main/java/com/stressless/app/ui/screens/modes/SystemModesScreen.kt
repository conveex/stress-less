package com.stressless.app.ui.screens.modes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.SensorOccupied
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SystemModesRoute(
    viewModel: SystemModesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    SystemModesScreen(
        uiState = uiState,
        onModeClick = viewModel::changeMode
    )
}

@Composable
private fun SystemModesScreen(
    uiState: SystemModesUiState,
    onModeClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Modos del sistema",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Selecciona cómo debe comportarse la habitación.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ModeCard(
                mode = "ACTIVE",
                title = "Automático",
                description = "El sistema responde según tu estado fisiológico.",
                selectedMode = uiState.selectedMode,
                isLoading = uiState.isLoading,
                icon = Icons.Outlined.PlayCircle,
                onClick = onModeClick
            )

            ModeCard(
                mode = "PAUSED",
                title = "Pausado",
                description = "Detiene temporalmente las acciones automáticas.",
                selectedMode = uiState.selectedMode,
                isLoading = uiState.isLoading,
                icon = Icons.Outlined.PauseCircle,
                onClick = onModeClick
            )

            ModeCard(
                mode = "MANUAL",
                title = "Manual",
                description = "Permite controlar dispositivos sin automatización.",
                selectedMode = uiState.selectedMode,
                isLoading = uiState.isLoading,
                icon = Icons.Outlined.SettingsRemote,
                onClick = onModeClick
            )

            ModeCard(
                mode = "EXIT_MODE",
                title = "Modo salida",
                description = "Apaga o estabiliza dispositivos al salir de la habitación.",
                selectedMode = uiState.selectedMode,
                isLoading = uiState.isLoading,
                icon = Icons.Outlined.SensorOccupied,
                onClick = onModeClick
            )

            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            uiState.message?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            uiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    mode: String,
    title: String,
    description: String,
    selectedMode: String,
    isLoading: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (String) -> Unit
) {
    val selected = mode == selectedMode

    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { onClick(mode) },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (selected) "Modo actual" else "Activar"
                )
            }
        }
    }
}