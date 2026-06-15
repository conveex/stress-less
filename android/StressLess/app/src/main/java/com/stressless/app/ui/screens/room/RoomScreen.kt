package com.stressless.app.ui.screens.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stressless.app.data.remote.dto.app.RoomDeviceResponseDto
import com.stressless.app.data.remote.dto.app.RoomPrimaryResponseDto
import com.stressless.app.ui.components.ErrorState
import com.stressless.app.ui.components.LoadingState
import com.stressless.app.util.isRecentlySeen
import com.stressless.app.util.relativeSeenText

@Composable
fun RoomRoute(
    onGoToManualControl: () -> Unit,
    viewModel: RoomViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            LoadingState("Cargando habitación...")
        }

        uiState.errorMessage != null -> {
            ErrorState(
                message = uiState.errorMessage ?: "Error desconocido",
                onRetry = viewModel::loadRoom
            )
        }

        uiState.data != null -> {
            RoomScreen(
                data = uiState.data!!,
                onGoToManualControl = onGoToManualControl
            )
        }
    }
}

@Composable
private fun RoomScreen(
    data: RoomPrimaryResponseDto,
    onGoToManualControl: () -> Unit
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
                text = data.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            HubStatusCard(data)

            Text(
                text = "Dispositivos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (data.devices.isEmpty()) {
                Text(
                    text = "No hay dispositivos registrados para esta habitación.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                data.devices.forEach { device ->
                    DeviceCard(device)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onGoToManualControl,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.SettingsRemote,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Abrir control manual")
            }
        }
    }
}

@Composable
private fun HubStatusCard(data: RoomPrimaryResponseDto) {
    val hub = data.hub
    val hubOnline = isRecentlySeen(hub?.lastSeenAt)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Hub principal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(hub?.hubLogicalId ?: "Sin hub vinculado")

            Text(
                text = "Estado: ${if (hubOnline) "En línea" else "Desconectado"} · Operación: ${hub?.operationalState ?: "--"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Última conexión: ${relativeSeenText(hub?.lastSeenAt)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!hub?.ipAddress.isNullOrBlank()) {
                Text(
                    text = "IP local: ${hub?.ipAddress}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: RoomDeviceResponseDto
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = iconForDevice(device.type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Column {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${device.type} · ${if (device.enabled) "Disponible" else "Deshabilitado"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = device.deviceKey,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                device.capabilities.forEach { capability ->
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(capability)
                        }
                    )
                }
            }
        }
    }
}

private fun iconForDevice(type: String) =
    when (type) {
        "LIGHT" -> Icons.Outlined.Lightbulb
        "FAN", "CLIMATE" -> Icons.Outlined.Air
        "DISPLAY" -> Icons.Outlined.Subtitles
        "AUDIO", "BUZZER" -> Icons.Outlined.Speaker
        else -> Icons.Outlined.SettingsRemote
    }