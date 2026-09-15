package com.example.service

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * Utilidad de multiplexación multimedia de alto rendimiento.
 * Combina la pista de video generada por MediaRecorder y la pista de audio
 * interno generada por AudioRecord + MediaCodec en un único contenedor MP4 final.
 * 
 * No realiza recodificación (transcoding), simplemente transfiere las muestras
 * directamente a nivel de contenedor, lo que garantiza velocidad instantánea
 * y cero pérdida de fotogramas o calidad.
 */
object MediaMuxerHelper {

    private const val TAG = "MediaMuxerHelper"
    private const val BUFFER_SIZE = 1024 * 1024 // 1 MB buffer para transferencias fluidas

    /**
     * Une el archivo de video temporal y el archivo de audio temporal en el archivo de salida final.
     * Si el audio no existe o está vacío (por ejemplo, el juego no emitió sonido), copia el video directamente.
     *
     * @param videoFile Archivo MP4 con la pista de video
     * @param audioFile Archivo MP4/M4A con la pista de audio AAC
     * @param outputFile Archivo final consolidado
     * @return true si la operación se completó exitosamente
     */
    fun mergeAudioVideo(videoFile: File, audioFile: File?, outputFile: File): Boolean {
        if (!videoFile.exists() || videoFile.length() == 0L) {
            Log.e(TAG, "El archivo de video origen no existe o está vacío: ${videoFile.absolutePath}")
            return false
        }

        // Si no hay archivo de audio o está vacío, transferir directamente el video
        if (audioFile == null || !audioFile.exists() || audioFile.length() < 100L) {
            Log.w(TAG, "Audio inexistente o sin datos. Copiando solo video al destino.")
            return try {
                videoFile.copyTo(outputFile, overwrite = true)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error al copiar video sin audio", e)
                false
            }
        }

        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        return try {
            videoExtractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
            audioExtractor = MediaExtractor().apply { setDataSource(audioFile.absolutePath) }

            // Buscar el índice de pista de video
            var videoTrackIndex = -1
            var videoFormat: MediaFormat? = null
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    videoFormat = format
                    break
                }
            }

            // Buscar el índice de pista de audio
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (videoTrackIndex < 0 || videoFormat == null) {
                Log.e(TAG, "No se encontró pista de video válida en el archivo origen.")
                videoFile.copyTo(outputFile, overwrite = true)
                return true
            }

            // Si el audio no tiene pista válida de audio, salvamos el video
            if (audioTrackIndex < 0 || audioFormat == null) {
                Log.w(TAG, "No se encontró pista de audio en el archivo de audio. Guardando solo video.")
                videoFile.copyTo(outputFile, overwrite = true)
                return true
            }

            // Inicializar el multiplexor final
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerVideoTrack = muxer.addTrack(videoFormat)
            val muxerAudioTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            // 1. Escribir muestras de video
            videoExtractor.selectTrack(videoTrackIndex)
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }
                bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                bufferInfo.flags = videoExtractor.sampleFlags
                muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)
                videoExtractor.advance()
            }

            // 2. Escribir muestras de audio interno
            audioExtractor.selectTrack(audioTrackIndex)
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = audioExtractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }
                bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                bufferInfo.flags = audioExtractor.sampleFlags
                muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                audioExtractor.advance()
            }

            Log.d(TAG, "Fusión de video y audio interno completada exitosamente en ${outputFile.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la multiplexación con MediaMuxer. Rescatando archivo de video original.", e)
            try {
                videoFile.copyTo(outputFile, overwrite = true)
                true
            } catch (copyEx: Exception) {
                Log.e(TAG, "Error catastrófico al rescatar el video", copyEx)
                false
            }
        } finally {
            try {
                videoExtractor?.release()
            } catch (_: Exception) {}
            try {
                audioExtractor?.release()
            } catch (_: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {}
        }
    }
}
