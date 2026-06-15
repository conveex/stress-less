package com.stressless.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stressless.app.data.remote.dto.app.AppHomeResponseDto
import com.stressless.app.ui.components.ErrorState
import com.stressless.app.ui.components.LoadingState
import com.stressless.app.util.isHubEffectivelyOnline
import com.stressless.app.util.isRecentlySeen
import com.stressless.app.util.operationalStateLabel
import com.stressless.app.util.physiologicalStateLabel
import com.stressless.app.util.relativeSeenText
import kotlin.math.roundToInt

@Composable
fun HomeRoute(
    onGoToManualControl: () -> Unit,
    onGoToSystemModes: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            LoadingState("Cargando inicio...")
        }

        uiState.errorMessage != null -> {
            ErrorState(
                message = uiState.errorMessage ?: "Error desconocido",
                onRetry = viewModel::loadHome
            )
        }

        uiState.data != null -> {
            HomeScreen(
                data = uiState.data!!,
                onGoToManualControl = onGoToManualControl,
                onGoToSystemModes = onGoToSystemModes
            )
        }
    }
}

@Composable
private fun HomeScreen(
    data: AppHomeResponseDto,
    onGoToManualControl: () -> Unit,
    onGoToSystemModes: () -> Unit
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
                text = "Hola, ${data.user.name}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Resumen actual de tu estado y habitación inteligente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CurrentStateCard(data)

            BiometricsSummaryCard(data)

            RoomSummaryCard(data)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onGoToSystemModes,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Spa,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Modos")
                }

                OutlinedButton(
                    onClick = onGoToManualControl,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SettingsRemote,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Manual")
                }
            }
        }
    }
}

@Composable
private fun CurrentStateCard(data: AppHomeResponseDto) {
    val stress = data.stress
    val confidencePercent = (stress.confidence * 100).roundToInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.padding(6.dp))

                Text(
                    text = "Estado actual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = physiologicalStateLabel(stress.detectedState),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stateDescription(stress.detectedState),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            AssistChip(
                onClick = { },
                label = {
                    Text("Confianza: $confidencePercent%")
                },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun BiometricsSummaryCard(data: AppHomeResponseDto) {
    val stress = data.stress

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Biometría",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            MetricRow(
                label = "Ritmo cardiaco",
                value = stress.bpmCurrent?.let { "${it.roundToInt()} BPM" } ?: "Sin dato",
                progress = normalizedProgress(stress.bpmCurrent, 40.0, 140.0)
            )

            MetricRow(
                label = "Respuesta galvánica",
                value = stress.gsrCurrent?.let { "${it.roundToInt()} GSR" } ?: "Sin dato",
                progress = normalizedProgress(stress.gsrCurrent, 0.0, 1200.0)
            )

            MetricRow(
                label = "Movimiento",
                value = stress.movementAtDetection?.let { "%.2f".format(it) } ?: "Sin dato",
                progress = normalizedProgress(stress.movementAtDetection, 0.0, 1.0)
            )

            val bandOnline = isRecentlySeen(data.band?.lastSeenAt)

            Text(
                text = "Pulsera: ${if (bandOnline) "En línea" else "Desconectada"} · Batería: ${data.band?.batteryLevel?.toString() ?: "--"}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Última conexión: ${relativeSeenText(data.band?.lastSeenAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    progress: Float
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RoomSummaryCard(data: AppHomeResponseDto) {
    val hubOnline = isHubEffectivelyOnline(
        status = data.hub?.status,
        lastSeenAt = data.hub?.lastSeenAt,
        ipAddress = data.hub?.ipAddress
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = data.room?.name ?: "Habitación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = if (hubOnline) "Hub en línea" else "Hub desconectado"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.SettingsRemote,
                            contentDescription = null
                        )
                    }
                )

                AssistChip(
                    onClick = { },
                    label = {
                        Text(data.hub?.operationalState?.let { operationalStateLabel(it) } ?: "Sin modo")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Spa,
                            contentDescription = null
                        )
                    }
                )
            }

            Text(
                text = "Última conexión hub: ${relativeSeenText(data.hub?.lastSeenAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DeviceMiniStatus(
                icon = Icons.Outlined.Lightbulb,
                label = "Perfil",
                value = data.activeProfile?.name ?: "Sin perfil"
            )

            DeviceMiniStatus(
                icon = Icons.Outlined.Air,
                label = "Último comando",
                value = data.lastCommand?.status ?: "Sin comando"
            )
        }
    }
}

@Composable
private fun DeviceMiniStatus(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(1f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun normalizedProgress(
    value: Double?,
    min: Double,
    max: Double
): Float {
    if (value == null) return 0f
    return ((value - min) / (max - min)).coerceIn(0.0, 1.0).toFloat()
}

private fun stateDescription(state: String): String {
    return when (state) {
        "HIGH_STRESS" -> "El sistema detecta señales elevadas asociadas a estrés."
        "MODERATE_STRESS" -> "El sistema detecta una activación moderada."
        "NORMAL" -> "El sistema no detecta señales elevadas de estrés en este momento."
        "RELAXED" -> "Tu estado actual parece relajado."
        "NO_DATA" -> "Aún no hay datos suficientes para estimar tu estado."
        else -> "Estado fisiológico estimado por el sistema."
    }
}