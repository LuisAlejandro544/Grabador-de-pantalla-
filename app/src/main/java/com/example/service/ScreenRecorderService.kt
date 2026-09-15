package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ScreenRecorderApp
import com.example.data.AudioSourceOption
import com.example.model.RecordingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Servicio en primer plano (Foreground Service) que gestiona la captura
 * de pantalla mediante MediaProjection y MediaRecorder.
 * 
 * Permite grabar:
 * 1. Solo audio interno del juego o app (Android 10+ mediante AudioPlaybackCaptureConfiguration).
 * 2. Solo micrófono (voz y ambiente).
 * 3. Video sin sonido.
 * 
 * Cumple con los requerimientos modernos de Android (incluyendo Android 14 y 15)
 * donde MediaProjection requiere FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION activo
 * antes de invocar getMediaProjection().
 */
class ScreenRecorderService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var internalAudioRecorder: InternalAudioRecorder? = null

    private var currentOutputFile: File? = null
    private var tempVideoFile: File? = null
    private var tempAudioFile: File? = null
    private var currentAudioSource: AudioSourceOption = AudioSourceOption.INTERNAL_ONLY

    private var recordingStartTime: Long = 0L
    private var currentWidth: Int = 1080
    private var currentHeight: Int = 1920
    private var currentFps: Int = 30
    private var currentHasAudio: Boolean = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.d(TAG, "MediaProjection detenido por el sistema")
            stopRecording()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                val width = intent.getIntExtra(EXTRA_WIDTH, 1080)
                val height = intent.getIntExtra(EXTRA_HEIGHT, 1920)
                val fps = intent.getIntExtra(EXTRA_FPS, 30)
                val bitrate = intent.getIntExtra(EXTRA_BITRATE, 8_000_000)
                val audioSourceStr = intent.getStringExtra(EXTRA_AUDIO_SOURCE)
                val audioSource = if (audioSourceStr != null) {
                    try {
                        AudioSourceOption.valueOf(audioSourceStr)
                    } catch (_: Exception) {
                        AudioSourceOption.INTERNAL_ONLY
                    }
                } else {
                    val legacyAudio = intent.getBooleanExtra(EXTRA_AUDIO, true)
                    if (legacyAudio) AudioSourceOption.INTERNAL_ONLY else AudioSourceOption.MUTE
                }

                if (resultCode != 0 && data != null) {
                    startRecordingInternal(resultCode, data, width, height, fps, bitrate, audioSource)
                } else {
                    _recorderState.value = ScreenRecorderState.Error("Datos de proyección inválidos")
                    stopSelf()
                }
            }

            ACTION_STOP -> {
                stopRecording()
            }
        }
        return START_NOT_STICKY
    }

    /**
     * Inicializa y arranca la captura de pantalla y audio (interno o micrófono).
     */
    @SuppressLint("WrongConstant")
    private fun startRecordingInternal(
        resultCode: Int,
        data: Intent,
        width: Int,
        height: Int,
        fps: Int,
        bitrate: Int,
        audioSource: AudioSourceOption
    ) {
        currentWidth = width
        currentHeight = height
        currentFps = fps
        currentAudioSource = audioSource
        currentHasAudio = audioSource != AudioSourceOption.MUTE

        try {
            // 1. Mostrar notificación en primer plano ANTES de solicitar MediaProjection (Requerido en Android 14+)
            val notification = createNotification(0L)
            val fgType = if (audioSource == AudioSourceOption.MIC) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                }
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(ScreenRecorderApp.NOTIFICATION_ID, notification, fgType)
            } else {
                startForeground(ScreenRecorderApp.NOTIFICATION_ID, notification)
            }

            // 2. Obtener densidad de pantalla y dimensiones
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            val screenDensity = metrics.densityDpi

            // Ajustar a múltiplos pares requeridos por codecs H264
            val videoWidth = if (width % 2 == 0) width else width - 1
            val videoHeight = if (height % 2 == 0) height else height - 1

            // 3. Crear archivo de salida y temporales en el directorio de películas
            val recordingsDir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Recordings")
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val finalOutputFile = File(recordingsDir, "REC_$timeStamp.mp4")
            currentOutputFile = finalOutputFile

            val isInternalAudio = audioSource == AudioSourceOption.INTERNAL_ONLY
            if (isInternalAudio) {
                tempVideoFile = File(recordingsDir, "TEMP_VID_$timeStamp.mp4")
                tempAudioFile = File(recordingsDir, "TEMP_AUD_$timeStamp.mp4")
            } else {
                tempVideoFile = null
                tempAudioFile = null
            }

            // 4. Configurar MediaRecorder
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder = recorder

            val hasMicPermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            // Solo configuramos AudioSource.MIC en MediaRecorder si el usuario seleccionó Micrófono
            val recordMic = audioSource == AudioSourceOption.MIC && hasMicPermission
            if (recordMic) {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            }
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

            if (recordMic) {
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioSamplingRate(44100)
                recorder.setAudioEncodingBitRate(128000)
            }

            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            recorder.setVideoSize(videoWidth, videoHeight)
            recorder.setVideoFrameRate(fps)
            recorder.setVideoEncodingBitRate(bitrate)

            // Si es audio interno, el MediaRecorder graba video puro en el archivo temporal
            val videoTargetFile = if (isInternalAudio) tempVideoFile!! else finalOutputFile
            recorder.setOutputFile(videoTargetFile.absolutePath)

            recorder.prepare()

            // 5. Inicializar MediaProjection y VirtualDisplay
            val projection = mediaProjectionManager?.getMediaProjection(resultCode, data)
            mediaProjection = projection
            projection?.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))

            virtualDisplay = projection?.createVirtualDisplay(
                "ScreenRecorderDisplay",
                videoWidth,
                videoHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.surface,
                null,
                null
            )

            // 6. Iniciar grabación física de video
            recorder.start()
            recordingStartTime = System.currentTimeMillis()
            vibrateFeedback()

            // 7. Si se seleccionó audio interno, iniciar capturador nativo AudioPlaybackCaptureConfiguration
            if (isInternalAudio && projection != null && tempAudioFile != null) {
                val internalRec = InternalAudioRecorder(projection, tempAudioFile!!)
                val started = internalRec.start()
                if (started) {
                    internalAudioRecorder = internalRec
                    Log.d(TAG, "Capturador de audio interno arrancado con éxito.")
                } else {
                    Log.w(TAG, "No se pudo iniciar el capturador de audio interno. Se grabará video mudo.")
                }
            }

            // 8. Notificar estado y lanzar temporizador
            _recorderState.value = ScreenRecorderState.Recording(
                durationMs = 0L,
                resolutionText = "${videoWidth}x${videoHeight}",
                fpsText = "$fps FPS",
                hasAudio = currentHasAudio
            )

            startTimer(videoWidth, videoHeight, fps, currentHasAudio)
            Log.d(TAG, "Grabación iniciada exitosamente. Destino: ${finalOutputFile.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar grabación", e)
            _recorderState.value = ScreenRecorderState.Error("Error al iniciar: ${e.localizedMessage}")
            cleanUp()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /**
     * Mantiene actualizado el contador de tiempo tanto en la notificación como en el estado UI.
     */
    private fun startTimer(width: Int, height: Int, fps: Int, audio: Boolean) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (true) {
                delay(1000L)
                val elapsed = System.currentTimeMillis() - recordingStartTime
                _recorderState.value = ScreenRecorderState.Recording(
                    durationMs = elapsed,
                    resolutionText = "${width}x${height}",
                    fpsText = "$fps FPS",
                    hasAudio = audio
                )
                // Actualizar notificación cada segundo
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.notify(ScreenRecorderApp.NOTIFICATION_ID, createNotification(elapsed))
            }
        }
    }

    /**
     * Detiene la captura, guarda los metadatos en la base de datos y libera recursos.
     */
    private fun stopRecording() {
        timerJob?.cancel()
        timerJob = null

        val duration = if (recordingStartTime > 0) System.currentTimeMillis() - recordingStartTime else 0L

        // 1. Detener capturador de audio interno primero para que vacíe buffers
        try {
            internalAudioRecorder?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Excepción al detener internalAudioRecorder", e)
        }
        internalAudioRecorder = null

        // 2. Detener MediaRecorder
        try {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
        } catch (e: Exception) {
            Log.w(TAG, "Excepción al detener MediaRecorder", e)
        }

        cleanUp()
        vibrateFeedback()

        // 3. Si se grabó audio interno, fusionar el archivo de video puro con el de audio interno
        val savedFile = currentOutputFile
        if (currentAudioSource == AudioSourceOption.INTERNAL_ONLY && tempVideoFile != null && savedFile != null) {
            val vid = tempVideoFile!!
            val aud = tempAudioFile
            if (vid.exists()) {
                Log.d(TAG, "Iniciando multiplexación de video y audio interno...")
                val mergeSuccess = MediaMuxerHelper.mergeAudioVideo(vid, aud, savedFile)
                if (mergeSuccess) {
                    Log.d(TAG, "Multiplexación completada con éxito.")
                } else {
                    Log.w(TAG, "Multiplexación falló o no requirió audio, se conserva el archivo base.")
                }
                try {
                    vid.delete()
                    aud?.delete()
                } catch (_: Exception) {}
            }
        }

        // 4. Guardar metadatos en base de datos si el archivo final se generó con éxito
        if (savedFile != null && savedFile.exists() && savedFile.length() > 0) {
            val title = savedFile.nameWithoutExtension
            val entity = RecordingEntity(
                title = title,
                filePath = savedFile.absolutePath,
                durationMs = duration,
                fileSizeBytes = savedFile.length(),
                resolution = "${currentWidth}x${currentHeight}",
                fps = currentFps,
                hasAudio = currentHasAudio
            )

            serviceScope.launch {
                try {
                    ScreenRecorderApp.instance.repository.saveRecording(entity)
                    Log.d(TAG, "Grabación guardada en base de datos: $title")
                } catch (e: Exception) {
                    Log.e(TAG, "Error guardando en base de datos", e)
                }
            }

            _recorderState.value = ScreenRecorderState.Completed(
                filePath = savedFile.absolutePath,
                durationMs = duration
            )
        } else {
            _recorderState.value = ScreenRecorderState.Idle
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Libera de forma segura todos los componentes de hardware y virtual display.
     */
    private fun cleanUp() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
        } catch (_: Exception) {}

        try {
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (_: Exception) {}

        try {
            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
            mediaProjection = null
        } catch (_: Exception) {}
    }

    /**
     * Genera una vibración táctil sutil para confirmar inicio/fin de grabación.
     */
    private fun vibrateFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(80)
                }
            }
        } catch (_: Exception) {
            // Vibrador opcional
        }
    }

    /**
     * Construye la notificación permanente con contador y botón para detener la grabación.
     */
    private fun createNotification(elapsedMs: Long): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ScreenRecorderService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val seconds = (elapsedMs / 1000) % 60
        val minutes = (elapsedMs / (1000 * 60)) % 60
        val hours = (elapsedMs / (1000 * 60 * 60))
        val timeString = if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }

        return NotificationCompat.Builder(this, ScreenRecorderApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_recording_title))
            .setContentText("Grabando pantalla • $timeString")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setContentIntent(appPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.notification_stop_action),
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        cleanUp()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ScreenRecorderService"

        const val ACTION_START = "com.example.action.START"
        const val ACTION_STOP = "com.example.action.STOP"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_WIDTH = "extra_width"
        const val EXTRA_HEIGHT = "extra_height"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_BITRATE = "extra_bitrate"
        const val EXTRA_AUDIO = "extra_audio"
        const val EXTRA_AUDIO_SOURCE = "extra_audio_source"

        private val _recorderState = MutableStateFlow<ScreenRecorderState>(ScreenRecorderState.Idle)
        val recorderState: StateFlow<ScreenRecorderState> = _recorderState.asStateFlow()

        fun resetState() {
            _recorderState.value = ScreenRecorderState.Idle
        }
    }
}
