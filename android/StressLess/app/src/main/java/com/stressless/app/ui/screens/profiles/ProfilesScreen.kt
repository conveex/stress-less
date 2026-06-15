package com.stressless.app.ui.screens.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stressless.app.data.remote.dto.app.ProfileResponseDto
import com.stressless.app.ui.components.ErrorState
import com.stressless.app.ui.components.LoadingState

@Composable
fun ProfilesRoute(
    viewModel: ProfilesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            LoadingState("Cargando perfiles...")
        }

        uiState.errorMessage != null -> {
            ErrorState(
                message = uiState.errorMessage ?: "Error desconocido",
                onRetry = viewModel::loadProfiles
            )
        }

        else -> {
            ProfilesScreen(uiState.profiles)
        }
    }
}

@Composable
private fun ProfilesScreen(
    profiles: List<ProfileResponseDto>
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
                text = "Perfiles ambientales",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Configura cómo debe responder la habitación ante cada estado detectado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (profiles.isEmpty()) {
                Text(
                    text = "Aún no hay perfiles ambientales.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                profiles.forEach { profile ->
                    ProfileCard(profile)
                }
            }

            OutlinedButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = false
            ) {
                Text("Crear perfil próximamente")
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ProfileResponseDto
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
                imageVector = Icons.Outlined.Spa,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = { Text(profile.targetState) }
                    )

                    AssistChip(
                        onClick = { },
                        label = { Text("${profile.actionsCount} acciones") }
                    )
                }
            }

            Switch(
                checked = profile.isActive,
                onCheckedChange = null
            )
        }
    }
}