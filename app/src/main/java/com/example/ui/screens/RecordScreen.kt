package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.AudioSourceOption
import com.example.data.CountdownOption
import com.example.service.ScreenRecorderState
import com.example.viewmodel.RecorderViewModel
import java.util.Locale

/**
 * Pantalla principal de grabación ("Grabar").
 * Permite al usuario iniciar/detener la captura de pantalla con retroalimentación visual dinámica,
 * monitorear el temporizador de grabación en vivo, configurar micrófono y cuenta regresiva.
 */
@Composable
fun RecordScreen(
    viewModel: RecorderViewModel,
    onNavigateToLibrary: () -> Unit
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val recorderState by viewModel.recorderState.collectAsState()
    val countdownValue by viewModel.countdownValue.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val isRecording = recorderState is ScreenRecorderState.Recording

    // Launcher para pedir permiso de proyección de pantalla al sistema Android
    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.onProjectionPermissionGranted(
                resultCode = result.resultCode,
                data = result.data!!,
                context = context
            )
        }
    }

    // Launcher para permisos de Notificación en Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Continuar con la solicitud de proyección
        val mediaProjectionManager =
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    // Launcher para permiso de micrófono
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setAudioEnabled(isGranted)
    }

    // Función que arranca el flujo seguro de permisos e inicio
    val startCaptureFlow = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotif = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotif) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                val mediaProjectionManager =
                    context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                projectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
            }
        } else {
            val mediaProjectionManager =
                context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
        }
    }

    // Notificación flotante de éxito al completar grabación
    LaunchedEffect(recorderState) {
        if (recorderState is ScreenRecorderState.Completed) {
            snackbarHostState.showSnackbar(
                message = "¡Grabación guardada con éxito en la biblioteca!"
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera superior descriptiva
            Text(
                text = "Grabador de Pantalla",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Captura tu pantalla con alta fidelidad y audio nítido",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Tarjeta de estado de grabación / Monitor central
            RecordingStatusConsole(
                recorderState = recorderState,
                countdownValue = countdownValue,
                onCancelCountdown = { viewModel.cancelCountdown() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botón central de acción masiva (Iniciar / Detener)
            RecordActionButton(
                isRecording = isRecording,
                isCountingDown = countdownValue != null,
                onStartClick = { startCaptureFlow() },
                onStopClick = { viewModel.stopRecording(context) }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Tarjeta de Controles Rápidos (Audio y Cuenta atrás)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Controles Rápidos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Selector de Fuente de Sonido (Interno para juegos, Micrófono o Mute)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (config.audioSource) {
                                        AudioSourceOption.INTERNAL_ONLY -> Icons.Default.GraphicEq
                                        AudioSourceOption.MIC -> Icons.Default.Mic
                                        AudioSourceOption.MUTE -> Icons.Default.MicOff
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Fuente de Audio",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = config.audioSource.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AudioSourceOption.entries.forEach { opt ->
                                FilterChip(
                                    selected = config.audioSource == opt,
                                    onClick = {
                                        if (opt == AudioSourceOption.MIC) {
                                            val hasAudioPerm = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (!hasAudioPerm) {
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                viewModel.setAudioSource(opt)
                                            }
                                        } else {
                                            viewModel.setAudioSource(opt)
                                        }
                                    },
                                    label = { Text(opt.shortLabel) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (opt) {
                                                AudioSourceOption.INTERNAL_ONLY -> Icons.Default.GraphicEq
                                                AudioSourceOption.MIC -> Icons.Default.Mic
                                                AudioSourceOption.MUTE -> Icons.Default.MicOff
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chip_audio_${opt.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selector rápido de cuenta regresiva
                    Text(
                        text = "Cuenta regresiva antes de empezar",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CountdownOption.entries.forEach { option ->
                            FilterChip(
                                selected = config.countdown == option,
                                onClick = { viewModel.setCountdown(option) },
                                label = { Text(option.label) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Chips informativos de resolución y tasa de cuadros
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HighQuality,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = config.resolution.label.split(" ").firstOrNull() ?: "720p",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = config.fps.label.split(" ").firstOrNull() ?: "30 FPS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de Consejos e Instrucciones para el usuario
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Modo en segundo plano",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Al iniciar, puedes minimizar la app y abrir cualquier juego o aplicación. La notificación permanente te permitirá detener la grabación cuando quieras.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

/**
 * Consola central que muestra el estado de la captura, contador de tiempo
 * o animación de cuenta regresiva.
 */
@Composable
private fun RecordingStatusConsole(
    recorderState: ScreenRecorderState,
    countdownValue: Int?,
    onCancelCountdown: () -> Unit
) {
    val isRecording = recorderState is ScreenRecorderState.Recording

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) Color(0xFF1E1014) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (countdownValue != null) {
                // Vista de cuenta regresiva previa
                Text(
                    text = "Preparando grabación...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$countdownValue",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onCancelCountdown) {
                    Text("Cancelar")
                }
            } else if (isRecording) {
                val rec = recorderState as ScreenRecorderState.Recording
                val seconds = (rec.durationMs / 1000) % 60
                val minutes = (rec.durationMs / (1000 * 60)) % 60
                val hours = (rec.durationMs / (1000 * 60 * 60))
                val formattedTime = if (hours > 0) {
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                }

                // Indicador de "Grabando en vivo"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE53935).copy(alpha = 0.2f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935).copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GRABANDO EN VIVO",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF5350)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reloj de duración
                Text(
                    text = formattedTime,
                    fontSize = 54.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Detalles en vivo
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rec.resolutionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    Text(text = "•", color = Color.Gray)
                    Text(
                        text = rec.fpsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    if (rec.hasAudio) {
                        Text(text = "•", color = Color.Gray)
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Audio activo",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                // Estado en espera (Idle)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Listo para Grabar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Presiona el botón inferior para iniciar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Botón prominente táctil con estilos diferenciados para Start / Stop.
 */
@Composable
private fun RecordActionButton(
    isRecording: Boolean,
    isCountingDown: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val buttonColor by animateColorAsState(
        targetValue = if (isRecording) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
        label = "btnColor"
    )

    Button(
        onClick = {
            if (isRecording) onStopClick() else onStartClick()
        },
        enabled = !isCountingDown,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .testTag("record_action_button"),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isRecording) "DETENER GRABACIÓN" else "INICIAR GRABACIÓN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
