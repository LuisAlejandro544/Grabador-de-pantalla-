package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.RecordingEntity
import com.example.ui.components.VideoPlayerDialog
import com.example.viewmodel.RecorderViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla de la biblioteca de grabaciones ("Grabaciones").
 * Muestra el historial completo de videos grabados guardados localmente,
 * permitiendo su reproducción, compartición, renombrado y eliminación.
 */
@Composable
fun LibraryScreen(
    viewModel: RecorderViewModel,
    onNavigateToRecord: () -> Unit
) {
    val context = LocalContext.current
    val recordings by viewModel.recordings.collectAsState()

    // Estados para diálogos modales
    var videoToPlay by remember { mutableStateOf<RecordingEntity?>(null) }
    var videoToDelete by remember { mutableStateOf<RecordingEntity?>(null) }
    var videoToRename by remember { mutableStateOf<RecordingEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    // Cálculo del espacio total utilizado por las grabaciones
    val totalBytes = remember(recordings) {
        recordings.sumOf { it.fileSizeBytes }
    }
    val totalSizeMb = String.format(Locale.getDefault(), "%.1f MB", totalBytes / (1024.0 * 1024.0))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Título de la sección
        Text(
            text = "Biblioteca de Grabaciones",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Resumen de cantidad y almacenamiento
        if (recordings.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${recordings.size} ${if (recordings.size == 1) "video grabado" else "videos grabados"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Espacio: $totalSizeMb",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (recordings.isEmpty()) {
            // Estado vacío ilustrado
            EmptyRecordingsState(onNavigateToRecord = onNavigateToRecord)
        } else {
            // Lista de grabaciones
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recordings, key = { it.id }) { recording ->
                    RecordingItemCard(
                        recording = recording,
                        onPlayClick = { videoToPlay = recording },
                        onShareClick = { shareVideo(context, File(recording.filePath)) },
                        onRenameClick = {
                            videoToRename = recording
                            renameInputText = recording.title
                        },
                        onDeleteClick = { videoToDelete = recording }
                    )
                }
            }
        }
    }

    // Diálogo de reproducción de video en pantalla
    videoToPlay?.let { rec ->
        VideoPlayerDialog(
            recording = rec,
            onDismiss = { videoToPlay = null }
        )
    }

    // Diálogo de confirmación de eliminación
    videoToDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("¿Eliminar grabación?") },
            text = {
                Text("Se eliminará permanentemente \"${rec.title}\" de la base de datos y de la memoria de tu dispositivo.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecording(rec)
                        videoToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de renombrar archivo
    videoToRename?.let { rec ->
        AlertDialog(
            onDismissRequest = { videoToRename = null },
            title = { Text("Renombrar grabación") },
            text = {
                Column {
                    Text(
                        text = "Introduce un nuevo nombre para este video:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rename_text_field"),
                        label = { Text("Título") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateRecordingTitle(rec, renameInputText)
                        videoToRename = null
                    },
                    enabled = renameInputText.isNotBlank()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToRename = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Tarjeta individual para mostrar los detalles de un video grabado.
 */
@Composable
private fun RecordingItemCard(
    recording: RecordingEntity,
    onPlayClick: () -> Unit,
    onShareClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val durationFormatted = remember(recording.durationMs) {
        val seconds = (recording.durationMs / 1000) % 60
        val minutes = (recording.durationMs / (1000 * 60)) % 60
        val hours = (recording.durationMs / (1000 * 60 * 60))
        if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    val dateFormatted = remember(recording.createdAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(recording.createdAt))
    }

    val sizeFormatted = remember(recording.fileSizeBytes) {
        val mb = recording.fileSizeBytes / (1024.0 * 1024.0)
        String.format(Locale.getDefault(), "%.1f MB", mb)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recording_card_${recording.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniatura decorativa con duración
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Reproducir",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Detalles del video
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Metadatos (Duración, tamaño, resolución)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = durationFormatted,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Text(
                            text = sizeFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(text = "•", color = MaterialTheme.colorScheme.outline)

                        Text(
                            text = recording.resolution,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (recording.hasAudio) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Grabado con audio",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de acciones inferiores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Renombrar
                IconButton(onClick = onRenameClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Renombrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Compartir
                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartir video",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Eliminar
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar video",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Reproducir botón principal
                FilledTonalButton(
                    onClick = onPlayClick,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver")
                }
            }
        }
    }
}

/**
 * Pantalla vacía cuando no existen videos en la biblioteca.
 */
@Composable
private fun EmptyRecordingsState(onNavigateToRecord: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Sin grabaciones aún",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tus videos capturados aparecerán aquí. Puedes compartirlos o reproducirlos cuando quieras.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onNavigateToRecord,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Videocam, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hacer una grabación")
            }
        }
    }
}

/**
 * Lanza el Intent para compartir el archivo MP4 mediante FileProvider.
 */
private fun shareVideo(context: Context, file: File) {
    try {
        if (!file.exists()) return
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir grabación"))
    } catch (_: Exception) {}
}
