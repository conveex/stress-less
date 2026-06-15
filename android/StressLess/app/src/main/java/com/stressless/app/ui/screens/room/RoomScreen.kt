package com.stressless.app.ui.screens.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RoomRoute(
    onGoToManualControl: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Mi habitación",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            HubStatusCard()

            Text(
                text = "Dispositivos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            DeviceCard(
                icon = Icons.Outlined.Lightbulb,
                name = "LED RGB",
                type = "Iluminación",
                status = "Disponible",
                capabilities = listOf("ON/OFF", "BRILLO", "COLOR")
            )

            DeviceCard(
                icon = Icons.Outlined.Air,
                name = "Ventilador",
                type = "Clima",
                status = "Disponible",
                capabilities = listOf("ON/OFF", "VELOCIDAD")
            )

            DeviceCard(
                icon = Icons.Outlined.Subtitles,
                name = "LCD 16x2",
                type = "Display",
                status = "Disponible",
                capabilities = listOf("MENSAJE")
            )

            DeviceCard(
                icon = Icons.Outlined.Speaker,
                name = "Buzzer",
                type = "Audio",
                status = "Disponible",
                capabilities = listOf("ON/OFF", "VOLUMEN")
            )

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
private fun HubStatusCard() {
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

            Text("hub-001")
            Text(
                text = "Estado: ACTIVE · Operación: Automático",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeviceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    type: String,
    status: String,
    capabilities: List<String>
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "$type · $status",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                capabilities.forEach { capability ->
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