# Grabador de Pantalla (Android)

Aplicación moderna, modular y de alto rendimiento para grabación de pantalla en Android, construida con **Jetpack Compose**, **Kotlin**, **Room Database** y un motor nativo en **C++20** con **OpenGL ES**.

Diseñada especialmente para ofrecer grabaciones fluidas a 60 FPS sin tirones, con soporte completo para procesadores de 64 bits (`arm64-v8a`) y 32 bits (`armeabi-v7a`), optimizada para distribución en tiendas de terceros como Uptodown o mediante APK directo.

---

## 🚀 Características Principales

- **Consola de Grabación en Vivo**:
  - Control de inicio y detención con un solo toque.
  - Temporizador activo con indicador pulsante.
  - Opciones de cuenta regresiva previa (3 s, 5 s o inmediata).
  - Interruptor de audio de micrófono con solicitud de permisos en tiempo de ejecución.
  - Servicio en primer plano persistente con controles desde la barra de notificaciones.

- **Biblioteca Local de Grabaciones**:
  - Persistencia segura de metadatos mediante base de datos **Room**.
  - Reproductor de video nativo integrado en ventana emergente sin salir de la app.
  - Compartición directa a través de `FileProvider` (WhatsApp, Drive, Telegram, etc.).
  - Herramientas de edición rápida: renombrado de archivos y eliminación permanente.
  - Contador de espacio de almacenamiento consumido en MB.

- **Panel de Ajustes y Rendimiento**:
  - Selector de resolución: 1080p (Full HD), 720p (HD) y 480p (Ahorro).
  - Tasa de cuadros: 30 FPS y 60 FPS.
  - Tasa de bits (Bitrate): 12 Mbps, 8 Mbps y 4 Mbps.
  - Diagnóstico de hardware en vivo: detección de arquitectura de CPU, ABIs compatibles y estado del motor gráfico C++20.

- **Motor Nativo (C++20 y OpenGL ES)**:
  - Arquitectura preparada con `CMakeLists.txt` y NDK para regulación de fotogramas (*Frame Pacing*) y codificación por hardware sin pausas del Garbage Collector.

---

## 🛠️ Stack Tecnológico

| Capa | Tecnología | Propósito |
|---|---|---|
| **UI** | Jetpack Compose + Material 3 | Interfaz táctil moderna, navegación reactiva y temas visuales |
| **Arquitectura** | MVVM + Coroutines + StateFlow | Reactividad fluida y desacoplamiento de componentes |
| **Persistencia** | Room Database + SQLite | Almacenamiento local de grabaciones y metadatos |
| **Servicio de Captura** | Android MediaProjection API | Captura de pantalla en segundo plano con Foreground Service |
| **Motor Gráfico / Nativo** | C++20 + OpenGL ES 2.0/3.0 + EGL | Base para Frame Pacing y codificación de baja latencia |
| **Compilador Nativo** | CMake 3.22.1 + Clang NDK | Soporte multi-arquitectura automático (`arm64-v8a`, `armeabi-v7a`) |

---

## 📋 Requisitos del Sistema

- **Versión mínima de Android:** Android 7.0 (API 24)
- **Versión objetivo (Target SDK):** Android 15 (API 36)
- **Arquitecturas soportadas:**
  - `arm64-v8a` (64 bits - estándar en teléfonos modernos)
  - `armeabi-v7a` (32 bits - teléfonos de gama de entrada o antiguos)
  - `x86_64` y `x86` (Emuladores y Chromebooks)

---

## 📦 Compilación y Generación del APK

Para compilar el proyecto y generar el archivo APK:

```bash
# Compilación estándar de depuración
gradle assembleDebug

# El APK generado se ubicará en:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📂 Estructura General del Código

Para ver el desglose técnico detallado de cada archivo y carpeta, consulta el archivo [STRUCTURE.md](STRUCTURE.md).
