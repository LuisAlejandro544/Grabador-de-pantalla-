package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CountdownOption
import com.example.data.VideoBitrate
import com.example.data.VideoFps
import com.example.data.VideoResolution
import com.example.service.NativeBridge
import com.example.viewmodel.RecorderViewModel

/**
 * Pantalla de Configuración ("Ajustes").
 * Permite personalizar la calidad del video, fotogramas por segundo, tasa de bits,
 * audio, temporizador previo y consultar la arquitectura del dispositivo móvil.
 */
@Composable
fun SettingsScreen(
    viewModel: RecorderViewModel
) {
    val config by viewModel.config.collectAsState()

    // Detección de arquitecturas compatibles (32 y 64 bits)
    val supportedAbis = Build.SUPPORTED_ABIS.joinToString(", ")
    val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Ajustes de Grabación",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        // Sección 1: Calidad de Video
        SettingsCard(
            title = "Resolución de Video",
            icon = Icons.Default.VideoSettings
        ) {
            VideoResolution.entries.forEach { res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setResolution(res) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = config.resolution == res,
                        onClick = { viewModel.setResolution(res) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = res.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${res.width}x${res.height} • ${res.description}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sección 2: Tasa de Cuadros (FPS) y Bitrate
        SettingsCard(
            title = "Rendimiento y Fluidez",
            icon = Icons.Default.Speed
        ) {
            Text(
                text = "Tasa de Cuadros (FPS)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoFps.entries.forEach { fpsOption ->
                    FilterChip(
                        selected = config.fps == fpsOption,
                        onClick = { viewModel.setFps(fpsOption) },
                        label = { Text(fpsOption.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tasa de Bits (Bitrate)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoBitrate.entries.forEach { bitrateOption ->
                    FilterChip(
                        selected = config.bitrate == bitrateOption,
                        onClick = { viewModel.setBitrate(bitrateOption) },
                        label = { Text(bitrateOption.label) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sección 3: Fuente de Audio y Temporizador
        SettingsCard(
            title = "Fuente de Sonido y Temporizador",
            icon = Icons.Default.Audiotrack
        ) {
            Text(
                text = "Modo de Captura de Audio",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Compatible con Android 10+ para captura directa interna sin ruido ambiental",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            com.example.data.AudioSourceOption.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setAudioSource(option) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = config.audioSource == option,
                        onClick = { viewModel.setAudioSource(option) },
                        modifier = Modifier.testTag("audio_source_${option.name.lowercase()}")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (option == com.example.data.AudioSourceOption.INTERNAL_ONLY) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Recomendado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = option.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Cuenta regresiva de inicio",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CountdownOption.entries.forEach { countdownOption ->
                    FilterChip(
                        selected = config.countdown == countdownOption,
                        onClick = { viewModel.setCountdown(countdownOption) },
                        label = { Text(countdownOption.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sección 4: Información de Arquitectura y Hardware
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Arquitectura del Dispositivo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoRow(label = "Arquitectura CPU:", value = if (is64Bit) "64-bit y 32-bit compatible" else "32-bit compatible")
                InfoRow(label = "ABIs soportadas:", value = supportedAbis)
                InfoRow(label = "Motor Gráfico / C++:", value = if (NativeBridge.isNativeReady()) "C++20 & OpenGL ES (Activo)" else "Modo Estándar")
                InfoRow(label = "Códec de video:", value = "H.264 / AVC Hardware")
                InfoRow(label = "Códec de audio:", value = "AAC Estéreo (44.1 kHz)")
                InfoRow(label = "Versión de Android:", value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                InfoRow(label = "Destino de videos:", value = "Películas/Recordings (Almacenamiento privado)")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Contenedor visual estandarizado para cada tarjeta de ajustes.
 */
@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

/**
 * Fila de datos informativos clave-valor.
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
