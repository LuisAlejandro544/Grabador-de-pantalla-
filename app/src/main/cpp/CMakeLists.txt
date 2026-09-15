cmake_minimum_required(VERSION 3.22.1)

project("screenrecorder_native")

# Configuración obligatoria del estándar C++20
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Librería compartida para procesamiento gráfico y codificación nativa
add_library(
    screenrecorder_native
    SHARED
    native-lib.cpp
)

# Enlace con librerías nativas del sistema Android (OpenGL ES, EGL, Log, MediaNDK)
find_library(log-lib log)
find_library(gles2-lib GLESv2)
find_library(egl-lib EGL)
find_library(mediandk-lib mediandk)
find_library(android-lib android)

target_link_libraries(
    screenrecorder_native
    ${log-lib}
    ${gles2-lib}
    ${egl-lib}
    ${mediandk-lib}
    ${android-lib}
)
