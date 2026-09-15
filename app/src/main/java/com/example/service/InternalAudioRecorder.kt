package com.example.service

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Grabador de audio interno del sistema operativo y aplicaciones/juegos.
 * 
 * Utiliza la API AudioPlaybackCaptureConfiguration (disponible desde Android 10 / API 29)
 * para capturar de forma nativa y directa el flujo de sonido del juego mediante MediaProjection.
 * 
 * Codifica las muestras PCM en tiempo real a formato AAC (128 kbps, 44.1 kHz estéreo)
 * y las almacena en un archivo contenedor MP4/M4A temporal que luego se fusionará con el video.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class InternalAudioRecorder(
    private val mediaProjection: MediaProjection,
    private val outputFile: File
) {
    private val isRecording = AtomicBoolean(false)
    private var workerThread: Thread? = null

    private var audioRecord: AudioRecord? = null
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var audioTrackIndex = -1
    private var muxerStarted = false

    companion object {
        private const val TAG = "InternalAudioRecorder"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_COUNT = 2
        private const val BIT_RATE = 128_000
        private const val TIMEOUT_USEC = 10_000L
    }

    /**
     * Inicia la captura y codificación en un hilo de trabajo dedicado.
     */
    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (isRecording.get()) {
            Log.w(TAG, "La captura de audio interno ya se encuentra en ejecución.")
            return false
        }

        try {
            // 1. Configurar AudioPlaybackCaptureConfiguration con las fuentes de juego y multimedia
            val captureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            // 2. Inicializar AudioRecord con la configuración de captura interna
            val record = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(captureConfig)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 4)
                .build()

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord no pudo inicializarse con AudioPlaybackCaptureConfiguration")
                record.release()
                return false
            }
            audioRecord = record

            // 3. Configurar codificador MediaCodec para formato AAC
            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                SAMPLE_RATE,
                CHANNEL_COUNT
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            mediaCodec = codec

            // 4. Inicializar multiplexor MP4 para la pista de audio
            if (outputFile.exists()) {
                outputFile.delete()
            }
            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxerStarted = false
            audioTrackIndex = -1

            isRecording.set(true)
            record.startRecording()

            // 5. Lanzar hilo de lectura y codificación
            workerThread = Thread({
                recordLoop(record, codec, minBufferSize)
            }, "InternalAudioCaptureThread").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }

            Log.d(TAG, "Captura de audio interno iniciada exitosamente.")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Fallo al iniciar el grabador de audio interno", e)
            release()
            return false
        }
    }

    /**
     * Ciclo continuo de procesamiento: lee PCM de AudioRecord y lo codifica en AAC.
     */
    private fun recordLoop(record: AudioRecord, codec: MediaCodec, bufferSize: Int) {
        val pcmBuffer = ByteArray(bufferSize)
        val bufferInfo = MediaCodec.BufferInfo()
        var totalBytesRead = 0L

        try {
            while (isRecording.get()) {
                val bytesRead = record.read(pcmBuffer, 0, pcmBuffer.size)
                if (bytesRead > 0) {
                    val inputIndex = codec.dequeueInputBuffer(TIMEOUT_USEC)
                    if (inputIndex >= 0) {
                        val inputBuffer: ByteBuffer? = codec.getInputBuffer(inputIndex)
                        inputBuffer?.clear()
                        inputBuffer?.put(pcmBuffer, 0, bytesRead)

                        // Calcular timestamp en microsegundos basado en la tasa de muestreo y tamaño
                        val ptsUs = (totalBytesRead * 1_000_000L) / (SAMPLE_RATE * CHANNEL_COUNT * 2)
                        totalBytesRead += bytesRead

                        codec.queueInputBuffer(inputIndex, 0, bytesRead, ptsUs, 0)
                    }
                }

                drainEncoder(codec, bufferInfo, false)
            }

            // Enviar señal de fin de flujo (EOS) al detener
            val eosIndex = codec.dequeueInputBuffer(TIMEOUT_USEC)
            if (eosIndex >= 0) {
                val ptsUs = (totalBytesRead * 1_000_000L) / (SAMPLE_RATE * CHANNEL_COUNT * 2)
                codec.queueInputBuffer(eosIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
            drainEncoder(codec, bufferInfo, true)

        } catch (e: Exception) {
            Log.e(TAG, "Error en el bucle de captura de audio interno", e)
        }
    }

    /**
     * Extrae los paquetes codificados de MediaCodec y los escribe en el archivo con MediaMuxer.
     */
    private fun drainEncoder(codec: MediaCodec, bufferInfo: MediaCodec.BufferInfo, endOfStream: Boolean) {
        val muxer = mediaMuxer ?: return

        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (muxerStarted) {
                    Log.w(TAG, "El formato de salida cambió dos veces inesperadamente.")
                } else {
                    val newFormat = codec.outputFormat
                    audioTrackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    muxerStarted = true
                    Log.d(TAG, "Pista de audio interno agregada al MediaMuxer (Track $audioTrackIndex)")
                }
            } else if (outputIndex >= 0) {
                val encodedData = codec.getOutputBuffer(outputIndex)
                if (encodedData != null && muxerStarted) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size != 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                    }
                }

                codec.releaseOutputBuffer(outputIndex, false)

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            } else {
                if (!endOfStream) {
                    break
                }
            }
        }
    }

    /**
     * Detiene la captura de audio interno y espera a que el hilo finalice.
     */
    fun stop() {
        if (!isRecording.compareAndSet(true, false)) {
            return
        }

        try {
            workerThread?.join(1500)
        } catch (_: InterruptedException) {}
        workerThread = null

        release()
    }

    /**
     * Libera de manera segura los recursos de hardware y audio del sistema.
     */
    private fun release() {
        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
        try {
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        try {
            mediaCodec?.stop()
        } catch (_: Exception) {}
        try {
            mediaCodec?.release()
        } catch (_: Exception) {}
        mediaCodec = null

        try {
            if (muxerStarted) {
                mediaMuxer?.stop()
            }
        } catch (_: Exception) {}
        try {
            mediaMuxer?.release()
        } catch (_: Exception) {}
        mediaMuxer = null
        muxerStarted = false
    }
}
