package com.example.data

import com.example.model.RecordingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repositorio que orquesta la persistencia en Room y la gestión de archivos
 * físicos de video en el almacenamiento interno/externo del dispositivo.
 */
class RecordingRepository(private val recordingDao: RecordingDao) {

    /**
     * Flujo reactivo con la lista completa de grabaciones.
     */
    val allRecordings: Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()

    /**
     * Guarda una nueva grabación en la base de datos tras concluir la captura.
     */
    suspend fun saveRecording(recording: RecordingEntity): Long = withContext(Dispatchers.IO) {
        recordingDao.insertRecording(recording)
    }

    /**
     * Actualiza el registro de una grabación (por ejemplo, al cambiar su título).
     */
    suspend fun updateRecording(recording: RecordingEntity) = withContext(Dispatchers.IO) {
        recordingDao.updateRecording(recording)
    }

    /**
     * Elimina el registro de la base de datos y borra el archivo MP4 del almacenamiento.
     */
    suspend fun deleteRecording(recording: RecordingEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
            // Continuar incluso si el archivo físico ya no existía
        }
        recordingDao.deleteRecording(recording)
    }
}
