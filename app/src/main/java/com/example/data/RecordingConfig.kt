package com.example.data

/**
 * Modelos de configuración de video y audio para la grabación de pantalla.
 * Permite al usuario ajustar la calidad gráfica, tasa de bits y sonido.
 */

enum class VideoResolution(
    val label: String,
    val width: Int,
    val height: Int,
    val description: String
) {
    FHD_1080P("1080p (Full HD)", 1080, 1920, "Máxima nitidez, ideal para tutoriales y gameplay"),
    HD_720P("720p (HD)", 720, 1280, "Balance óptimo entre calidad y tamaño de archivo"),
    SD_480P("480p (SD)", 480, 854, "Menor tamaño de archivo, ideal para compartir rápido")
}

enum class VideoFps(val value: Int, val label: String) {
    FPS_30(30, "30 FPS (Estándar fluido)"),
    FPS_60(60, "60 FPS (Ultra suave)")
}

enum class VideoBitrate(val mbps: Int, val label: String) {
    LOW(4, "4 Mbps (Económico)"),
    MEDIUM(8, "8 Mbps (Recomendado)"),
    HIGH(12, "12 Mbps (Alta fidelidad)")
}

enum class CountdownOption(val seconds: Int, val label: String) {
    NONE(0, "Sin espera"),
    SEC_3(3, "3 segundos"),
    SEC_5(5, "5 segundos")
}

/**
 * Fuentes de audio disponibles para la grabación.
 * En Android 10+ (API 29+), AudioPlaybackCaptureConfiguration permite capturar
 * el audio interno de videojuegos y aplicaciones sin ruidos ambientales del micrófono.
 */
enum class AudioSourceOption(
    val label: String,
    val shortLabel: String,
    val description: String
) {
    INTERNAL_ONLY(
        label = "Solo Audio Interno (Juego / App)",
        shortLabel = "Audio Interno",
        description = "Captura directamente los sonidos y música del juego o app sin ruido del micrófono"
    ),
    MIC(
        label = "Solo Micrófono (Voz)",
        shortLabel = "Micrófono",
        description = "Captura tu voz y sonidos ambientales externos para tutoriales"
    ),
    MUTE(
        label = "Sin Audio (Silenciado)",
        shortLabel = "Sin Audio",
        description = "Graba únicamente el video sin pistas de sonido"
    )
}

/**
 * Configuración consolidada de grabación que se envía al servicio de captura.
 */
data class RecordingConfig(
    val resolution: VideoResolution = VideoResolution.HD_720P,
    val fps: VideoFps = VideoFps.FPS_30,
    val bitrate: VideoBitrate = VideoBitrate.MEDIUM,
    val audioSource: AudioSourceOption = AudioSourceOption.INTERNAL_ONLY,
    val countdown: CountdownOption = CountdownOption.SEC_3
) {
    val bitrateInBps: Int
        get() = bitrate.mbps * 1_000_000

    val recordAudio: Boolean
        get() = audioSource != AudioSourceOption.MUTE
}
