package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa una grabación de pantalla guardada en el dispositivo.
 * 
 * Almacena los metadatos esenciales del archivo de video para mostrarlos
 * en la biblioteca (nombre, duración, peso en bytes, resolución y fecha).
 * Se persiste en la base de datos local Room.
 */
@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Título descriptivo o editable de la grabación
    val title: String,
    
    // Ruta absoluta del archivo de video (.mp4) en el almacenamiento local
    val filePath: String,
    
    // Duración de la grabación en milisegundos
    val durationMs: Long,
    
    // Tamaño del archivo en bytes para calcular MB/GB
    val fileSizeBytes: Long,
    
    // Resolución en formato texto, ej: "1920x1080" o "1280x720"
    val resolution: String,
    
    // Tasa de cuadros por segundo configurada (30 o 60 fps)
    val fps: Int,
    
    // Indica si se grabó con audio del micrófono
    val hasAudio: Boolean,
    
    // Fecha y hora de creación en epoch milliseconds
    val createdAt: Long = System.currentTimeMillis()
)
