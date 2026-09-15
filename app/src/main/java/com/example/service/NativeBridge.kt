package com.example.service

import android.util.Log

/**
 * Puente de comunicación JNI entre la capa Kotlin y la librería nativa C++20 / OpenGL ES.
 */
object NativeBridge {
    private const val TAG = "NativeBridge"
    private var isLoaded = false

    init {
        try {
            System.loadLibrary("screenrecorder_native")
            isLoaded = true
            Log.i(TAG, "Librería nativa C++20/OpenGL cargada correctamente.")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Librería nativa no disponible en esta arquitectura: ${e.message}")
            isLoaded = false
        }
    }

    /**
     * Retorna el estado y versión del motor nativo.
     */
    external fun getNativeEngineInfo(): String

    fun isNativeReady(): Boolean = isLoaded
}
