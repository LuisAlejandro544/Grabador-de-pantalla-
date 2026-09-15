package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.RecordingRepository

/**
 * Clase Application principal.
 * Inicializa la base de datos Room, el repositorio y los canales de notificación
 * indispensables para que el servicio de grabación en primer plano funcione
 * correctamente en Android 8.0 (API 26) hasta Android 14+ (API 34+).
 */
class ScreenRecorderApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: RecordingRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        repository = RecordingRepository(database.recordingDao())

        createNotificationChannel()
    }

    /**
     * Registra el canal de notificación para el Foreground Service.
     * En Android 8.0+ todo servicio en primer plano debe asociarse a un canal válido.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "screen_recording_channel"
        const val NOTIFICATION_ID = 1001

        lateinit var instance: ScreenRecorderApp
            private set
    }
}
