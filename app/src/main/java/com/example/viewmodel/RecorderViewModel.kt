package com.example.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScreenRecorderApp
import com.example.data.AudioSourceOption
import com.example.data.CountdownOption
import com.example.data.RecordingConfig
import com.example.data.RecordingRepository
import com.example.data.VideoBitrate
import com.example.data.VideoFps
import com.example.data.VideoResolution
import com.example.model.RecordingEntity
import com.example.service.ScreenRecorderService
import com.example.service.ScreenRecorderState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel principal de la aplicación.
 * Orquesta la configuración de grabación, el estado del servicio de captura,
 * y la persistencia de las grabaciones guardadas en la biblioteca.
 */
class RecorderViewModel(
    private val repository: RecordingRepository = ScreenRecorderApp.instance.repository
) : ViewModel() {

    // Configuración actual elegida por el usuario
    private val _config = MutableStateFlow(RecordingConfig())
    val config: StateFlow<RecordingConfig> = _config.asStateFlow()

    // Estado reactivo del servicio de grabación (Idle, Countdown, Recording, Completed, Error)
    val recorderState: StateFlow<ScreenRecorderState> = ScreenRecorderService.recorderState

    // Lista en tiempo real de grabaciones almacenadas en la base de datos Room
    val recordings: StateFlow<List<RecordingEntity>> = repository.allRecordings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Estado local para la cuenta regresiva antes de arrancar la proyección
    private val _countdownValue = MutableStateFlow<Int?>(null)
    val countdownValue: StateFlow<Int?> = _countdownValue.asStateFlow()

    private var countdownJob: Job? = null

    /**
     * Inicia el proceso de cuenta atrás o arranca directamente el servicio
     * si el usuario tiene seleccionada la opción "Sin espera".
     */
    fun onProjectionPermissionGranted(
        resultCode: Int,
        data: Intent,
        context: Context
    ) {
        val currentConfig = _config.value
        val seconds = currentConfig.countdown.seconds

        if (seconds > 0) {
            countdownJob?.cancel()
            countdownJob = viewModelScope.launch {
                for (s in seconds downTo 1) {
                    _countdownValue.value = s
                    delay(1000L)
                }
                _countdownValue.value = null
                launchRecorderService(resultCode, data, context, currentConfig)
            }
        } else {
            launchRecorderService(resultCode, data, context, currentConfig)
        }
    }

    /**
     * Cancela una cuenta regresiva si el usuario pulsa cancelar antes de que comience.
     */
    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _countdownValue.value = null
    }

    /**
     * Envía el Intent al servicio en primer plano para comenzar la captura real de pantalla.
     */
    private fun launchRecorderService(
        resultCode: Int,
        data: Intent,
        context: Context,
        cfg: RecordingConfig
    ) {
        val intent = Intent(context, ScreenRecorderService::class.java).apply {
            action = ScreenRecorderService.ACTION_START
            putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecorderService.EXTRA_RESULT_DATA, data)
            putExtra(ScreenRecorderService.EXTRA_WIDTH, cfg.resolution.width)
            putExtra(ScreenRecorderService.EXTRA_HEIGHT, cfg.resolution.height)
            putExtra(ScreenRecorderService.EXTRA_FPS, cfg.fps.value)
            putExtra(ScreenRecorderService.EXTRA_BITRATE, cfg.bitrateInBps)
            putExtra(ScreenRecorderService.EXTRA_AUDIO, cfg.recordAudio)
            putExtra(ScreenRecorderService.EXTRA_AUDIO_SOURCE, cfg.audioSource.name)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /**
     * Detiene la grabación activa ordenando al servicio cerrar los streams.
     */
    fun stopRecording(context: Context) {
        val intent = Intent(context, ScreenRecorderService::class.java).apply {
            action = ScreenRecorderService.ACTION_STOP
        }
        context.startService(intent)
    }

    /**
     * Restablece el estado de finalización para volver al estado Idle.
     */
    fun dismissCompleted() {
        ScreenRecorderService.resetState()
    }

    /**
     * Elimina una grabación tanto de la base de datos Room como del disco.
     */
    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            repository.deleteRecording(recording)
        }
    }

    /**
     * Actualiza el título de una grabación existente.
     */
    fun updateRecordingTitle(recording: RecordingEntity, newTitle: String) {
        if (newTitle.isNotBlank() && newTitle != recording.title) {
            viewModelScope.launch {
                repository.updateRecording(recording.copy(title = newTitle.trim()))
            }
        }
    }

    /**
     * Métodos de modificación de configuración de video y audio.
     */
    fun setResolution(resolution: VideoResolution) {
        _config.value = _config.value.copy(resolution = resolution)
    }

    fun setFps(fps: VideoFps) {
        _config.value = _config.value.copy(fps = fps)
    }

    fun setBitrate(bitrate: VideoBitrate) {
        _config.value = _config.value.copy(bitrate = bitrate)
    }

    fun setAudioSource(source: AudioSourceOption) {
        _config.value = _config.value.copy(audioSource = source)
    }

    fun setAudioEnabled(enabled: Boolean) {
        val newSource = if (enabled) {
            if (_config.value.audioSource == AudioSourceOption.MUTE) AudioSourceOption.INTERNAL_ONLY else _config.value.audioSource
        } else {
            AudioSourceOption.MUTE
        }
        _config.value = _config.value.copy(audioSource = newSource)
    }

    fun setCountdown(countdown: CountdownOption) {
        _config.value = _config.value.copy(countdown = countdown)
    }
}
