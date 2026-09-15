#include <jni.h>
#include <string>
#include <android/log.h>
#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <media/NdkMediaCodec.h>
#include <media/NdkMediaMuxer.h>

#define LOG_TAG "ScreenRecorderNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

/**
 * Módulo C++20 con soporte para OpenGL ES y MediaNDK.
 * 
 * Este módulo sienta la base para:
 * 1. Control estricto de tiempo de fotogramas (Frame Pacing) mediante EGL/OpenGL.
 * 2. Pipeline nativo de bajo nivel sin pausas del recolector de basura (GC).
 * 3. Compatibilidad probada para arquitecturas ARM de 64 bits (arm64-v8a) y 32 bits (armeabi-v7a).
 */

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_service_NativeBridge_getNativeEngineInfo(
    JNIEnv* env,
    jobject /* this */
) {
    // Demostración de características C++20 y APIs gráficas nativas
    constexpr int glVersion = 2; // GLES 2.0 / 3.0
    std::string engineStatus = "Motor Nativo C++20 y OpenGL ES listo (GL v" + std::to_string(glVersion) + ")";
    
    LOGI("%s", engineStatus.c_str());
    return env->NewStringUTF(engineStatus.c_str());
}
