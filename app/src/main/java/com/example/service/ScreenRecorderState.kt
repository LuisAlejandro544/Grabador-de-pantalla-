package com.example.service

/**
 * Estados del grabador de pantalla para sincronizar de manera reactiva
 * la interfaz de usuario con el ciclo de vida del servicio en segundo plano.
 */
sealed class ScreenRecorderState {
    // El grabador está en reposo listo para iniciar
    data object Idle : ScreenRecorderState()

    // Cuenta regresiva antes de comenzar la captura (ej: 3, 2, 1...)
    data class Countdown(val secondsRemaining: Int) : ScreenRecorderState()

    // Grabación activa registrando pantalla y opcionalmente micrófono
    data class Recording(
        val durationMs: Long = 0L,
        val resolutionText: String = "",
        val fpsText: String = "",
        val hasAudio: Boolean = false
    ) : ScreenRecorderState()

    // Grabación completada y guardada con éxito
    data class Completed(val filePath: String, val durationMs: Long) : ScreenRecorderState()

    // Error durante la inicialización o ejecución
    data class Error(val message: String) : ScreenRecorderState()
}
