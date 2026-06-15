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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
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

            LedControlCard(
                uiState = uiState,
                onLedOnChange = onLedOnChange,
                onLedBrightnessChange = onLedBrightnessChange,
                onLedColorChange = onLedColorChange,
                onSendLed = onSendLed
            )

            FanControlCard(
                uiState = uiState,
                onFanOnChange = onFanOnChange,
                onFanSpeedChange = onFanSpeedChange,
                onSendFan = onSendFan
            )

            LcdControlCard(
                uiState = uiState,
                onLcdMessageChange = onLcdMessageChange,
                onSendLcd = onSendLcd
            )

            BuzzerControlCard(
                uiState = uiState,
                onBuzzerOnChange = onBuzzerOnChange,
                onBuzzerVolumeChange = onBuzzerVolumeChange,
                onSendBuzzer = onSendBuzzer
            )

            Button(
                onClick = onSendAll,
                enabled = !uiState.isLoading,
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
                    color = MaterialTheme.colorScheme.primary
                )
            }

            uiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun LedControlCard(
    uiState: ManualControlUiState,
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
                onCheckedChange = onLedOnChange
            )
        }

        Text("Brillo ${uiState.ledBrightness.toInt()}%")

        Slider(
            value = uiState.ledBrightness,
            onValueChange = onLedBrightnessChange,
            valueRange = 0f..100f
        )

        Text("Color")

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorChip("#00FFAA", uiState.ledColorHex, onLedColorChange)
            ColorChip("#00AAFF", uiState.ledColorHex, onLedColorChange)
            ColorChip("#FFB000", uiState.ledColorHex, onLedColorChange)
            ColorChip("#FFFFFF", uiState.ledColorHex, onLedColorChange)
        }

        Button(
            onClick = onSendLed,
            enabled = !uiState.isLoading,
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
                onCheckedChange = onFanOnChange
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpeedChip("LOW", uiState.fanSpeed, onFanSpeedChange)
            SpeedChip("MEDIUM", uiState.fanSpeed, onFanSpeedChange)
            SpeedChip("HIGH", uiState.fanSpeed, onFanSpeedChange)
        }

        Button(
            onClick = onSendFan,
            enabled = !uiState.isLoading,
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
            enabled = !uiState.isLoading,
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
                onCheckedChange = onBuzzerOnChange
            )
        }

        Text("Volumen ${uiState.buzzerVolume.toInt()}%")

        Slider(
            value = uiState.buzzerVolume,
            onValueChange = onBuzzerVolumeChange,
            valueRange = 0f..100f
        )

        Button(
            onClick = onSendBuzzer,
            enabled = !uiState.isLoading,
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
    onClick: (String) -> Unit
) {
    FilterChip(
        selected = color == selected,
        onClick = { onClick(color) },
        label = { Text(color) }
    )
}

@Composable
private fun SpeedChip(
    speed: String,
    selected: String,
    onClick: (String) -> Unit
) {
    FilterChip(
        selected = speed == selected,
        onClick = { onClick(speed) },
        label = { Text(speed) }
    )
}