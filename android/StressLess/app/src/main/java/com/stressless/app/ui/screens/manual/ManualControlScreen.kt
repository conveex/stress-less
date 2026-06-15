package com.stressless.app.ui.screens.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ManualControlRoute(
    viewModel: ManualControlViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ManualControlScreen(
        uiState = uiState,
        onLedOnChange = viewModel::onLedOnChange,
        onLedBrightnessChange = viewModel::onLedBrightnessChange,
        onLedColorChange = viewModel::onLedColorChange,
        onFanOnChange = viewModel::onFanOnChange,
        onFanSpeedChange = viewModel::onFanSpeedChange,
        onLcdMessageChange = viewModel::onLcdMessageChange,
        onBuzzerOnChange = viewModel::onBuzzerOnChange,
        onBuzzerVolumeChange = viewModel::onBuzzerVolumeChange,
        onSendLed = viewModel::sendLedCommand,
        onSendFan = viewModel::sendFanCommand,
        onSendLcd = viewModel::sendLcdCommand,
        onSendBuzzer = viewModel::sendBuzzerCommand,
        onSendAll = viewModel::sendAllCommand
    )
}

@Composable
private fun ManualControlScreen(
    uiState: ManualControlUiState,
    onLedOnChange: (Boolean) -> Unit,
    onLedBrightnessChange: (Float) -> Unit,
    onLedColorChange: (String) -> Unit,
    onFanOnChange: (Boolean) -> Unit,
    onFanSpeedChange: (String) -> Unit,
    onLcdMessageChange: (String) -> Unit,
    onBuzzerOnChange: (Boolean) -> Unit,
    onBuzzerVolumeChange: (Float) -> Unit,
    onSendLed: () -> Unit,
    onSendFan: () -> Unit,
    onSendLcd: () -> Unit,
    onSendBuzzer: () -> Unit,
    onSendAll: () -> Unit
) {
    val manualEnabled = uiState.operationalState == "MANUAL"
    val controlsEnabled = manualEnabled && !uiState.isLoading

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
                text = "Control manual",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Envía comandos directos al hub ${uiState.hubLogicalId}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CurrentModeCard(
                operationalState = uiState.operationalState,
                manualEnabled = manualEnabled
            )

            LedControlCard(
                uiState = uiState,
                enabled = controlsEnabled,
                onLedOnChange = onLedOnChange,
                onLedBrightnessChange = onLedBrightnessChange,
                onLedColorChange = onLedColorChange,
                onSendLed = onSendLed
            )

            FanControlCard(
                uiState = uiState,
                enabled = controlsEnabled,
                onFanOnChange = onFanOnChange,
                onFanSpeedChange = onFanSpeedChange,
                onSendFan = onSendFan
            )

            LcdControlCard(
                uiState = uiState,
                enabled = controlsEnabled,
                onLcdMessageChange = onLcdMessageChange,
                onSendLcd = onSendLcd
            )

            BuzzerControlCard(
                uiState = uiState,
                enabled = controlsEnabled,
                onBuzzerOnChange = onBuzzerOnChange,
                onBuzzerVolumeChange = onBuzzerVolumeChange,
                onSendBuzzer = onSendBuzzer
            )

            Button(
                onClick = onSendAll,
                enabled = controlsEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.SettingsRemote,
                    contentDescription = null
                )
                Text(" Enviar configuración completa")
            }

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

            uiState.commandStatus?.let {
                AssistChip(
                    onClick = { },
                    label = {
                        Text("Estado comando: $it")
                    }
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
private fun CurrentModeCard(
    operationalState: String,
    manualEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (manualEnabled) Icons.Outlined.Info else Icons.Outlined.Lock,
                contentDescription = null,
                tint = if (manualEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Modo actual: ${operationalStateLabel(operationalState)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (manualEnabled) {
                        "Puedes enviar comandos manuales al hub."
                    } else {
                        "El control manual está bloqueado. Activa el modo MANUAL para enviar comandos."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LedControlCard(
    uiState: ManualControlUiState,
    enabled: Boolean,
    onLedOnChange: (Boolean) -> Unit,
    onLedBrightnessChange: (Float) -> Unit,
    onLedColorChange: (String) -> Unit,
    onSendLed: () -> Unit
) {
    ControlCard(
        icon = Icons.Outlined.Lightbulb,
        title = "LED RGB",
        subtitle = "Encendido, brillo y color"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Encendido")
            Switch(
                checked = uiState.ledOn,
                onCheckedChange = onLedOnChange,
                enabled = enabled
            )
        }

        Text("Brillo ${uiState.ledBrightness.toInt()}%")

        Slider(
            value = uiState.ledBrightness,
            onValueChange = onLedBrightnessChange,
            valueRange = 0f..100f,
            enabled = enabled
        )

        Text("Color HEX")

        OutlinedTextField(
            value = uiState.ledColorHex,
            onValueChange = onLedColorChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ejemplo: #00FFAA") },
            singleLine = true,
            enabled = enabled
        )

        Text(
            text = "Colores rápidos",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ColorChip(
                    color = "#00FFAA",
                    selected = uiState.ledColorHex,
                    enabled = enabled,
                    onClick = onLedColorChange,
                    modifier = Modifier.weight(1f)
                )

                ColorChip(
                    color = "#00AAFF",
                    selected = uiState.ledColorHex,
                    enabled = enabled,
                    onClick = onLedColorChange,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ColorChip(
                    color = "#FFB000",
                    selected = uiState.ledColorHex,
                    enabled = enabled,
                    onClick = onLedColorChange,
                    modifier = Modifier.weight(1f)
                )

                ColorChip(
                    color = "#FFFFFF",
                    selected = uiState.ledColorHex,
                    enabled = enabled,
                    onClick = onLedColorChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Button(
            onClick = onSendLed,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Enviar LED")
        }
    }
}

@Composable
private fun FanControlCard(
    uiState: ManualControlUiState,
    enabled: Boolean,
    onFanOnChange: (Boolean) -> Unit,
    onFanSpeedChange: (String) -> Unit,
    onSendFan: () -> Unit
) {
    ControlCard(
        icon = Icons.Outlined.Air,
        title = "Ventilador",
        subtitle = "Encendido y velocidad"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Encendido")
            Switch(
                checked = uiState.fanOn,
                onCheckedChange = onFanOnChange,
                enabled = enabled
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpeedChip(
                speed = "LOW",
                selected = uiState.fanSpeed,
                enabled = enabled,
                onClick = onFanSpeedChange
            )

            SpeedChip(
                speed = "MEDIUM",
                selected = uiState.fanSpeed,
                enabled = enabled,
                onClick = onFanSpeedChange
            )

            SpeedChip(
                speed = "HIGH",
                selected = uiState.fanSpeed,
                enabled = enabled,
                onClick = onFanSpeedChange
            )
        }

        Button(
            onClick = onSendFan,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Enviar ventilador")
        }
    }
}

@Composable
private fun LcdControlCard(
    uiState: ManualControlUiState,
    enabled: Boolean,
    onLcdMessageChange: (String) -> Unit,
    onSendLcd: () -> Unit
) {
    ControlCard(
        icon = Icons.Outlined.Subtitles,
        title = "LCD",
        subtitle = "Mensaje mostrado en la habitación"
    ) {
        OutlinedTextField(
            value = uiState.lcdMessage,
            onValueChange = onLcdMessageChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Mensaje") },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            )
        )

        Text(
            text = "${uiState.lcdMessage.length}/32 caracteres",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onSendLcd,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Enviar mensaje")
        }
    }
}

@Composable
private fun BuzzerControlCard(
    uiState: ManualControlUiState,
    enabled: Boolean,
    onBuzzerOnChange: (Boolean) -> Unit,
    onBuzzerVolumeChange: (Float) -> Unit,
    onSendBuzzer: () -> Unit
) {
    ControlCard(
        icon = Icons.Outlined.Speaker,
        title = "Buzzer",
        subtitle = "Alerta sonora"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Encendido")
            Switch(
                checked = uiState.buzzerOn,
                onCheckedChange = onBuzzerOnChange,
                enabled = enabled
            )
        }

        Text("Volumen ${uiState.buzzerVolume.toInt()}%")

        Slider(
            value = uiState.buzzerVolume,
            onValueChange = onBuzzerVolumeChange,
            valueRange = 0f..100f,
            enabled = enabled
        )

        Button(
            onClick = onSendBuzzer,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Enviar buzzer")
        }
    }
}

@Composable
private fun ControlCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            content()
        }
    }
}

@Composable
private fun ColorChip(
    color: String,
    selected: String,
    enabled: Boolean,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = color == selected,
        onClick = { onClick(color) },
        enabled = enabled,
        label = {
            Text(color)
        },
        modifier = modifier
    )
}

@Composable
private fun SpeedChip(
    speed: String,
    selected: String,
    enabled: Boolean,
    onClick: (String) -> Unit
) {
    FilterChip(
        selected = speed == selected,
        onClick = { onClick(speed) },
        enabled = enabled,
        label = {
            Text(speed)
        }
    )
}

private fun operationalStateLabel(state: String): String {
    return when (state) {
        "ACTIVE" -> "AUTOMÁTICO"
        "PAUSED" -> "PAUSADO"
        "MANUAL" -> "MANUAL"
        "EXIT_MODE" -> "MODO SALIDA"
        else -> state
    }
}