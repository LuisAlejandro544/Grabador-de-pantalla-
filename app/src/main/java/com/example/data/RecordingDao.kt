package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.RecordingEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para interactuar con la tabla de grabaciones.
 * Permite listar reactivamente todas las grabaciones ordenadas por fecha reciente,
 * así como insertar nuevas grabaciones tras finalizar una captura, renombrar o eliminar.
 */
@Dao
interface RecordingDao {

    /**
     * Obtiene el flujo reactivo de todas las grabaciones registradas,
     * ordenadas desde la más reciente hasta la más antigua.
     */
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    /**
     * Busca una grabación específica por su identificador primario.
     */
    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    suspend fun getRecordingById(id: Long): RecordingEntity?

    /**
     * Inserta un nuevo registro de grabación al completarse el archivo.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity): Long

    /**
     * Actualiza el título o datos de una grabación existente.
     */
    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    /**
     * Elimina el registro de una grabación en la base de datos.
     */
    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)

    /**
     * Elimina un registro por ID directamente.
     */
    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: Long)
}
