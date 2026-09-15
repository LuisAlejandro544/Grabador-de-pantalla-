# Contexto Técnico del Proyecto (AI_CONTEXT)

Este archivo proporciona contexto esencial para modelos de inteligencia artificial y desarrolladores que continúen trabajando en esta base de código.

---

## 🎯 Perfil del Usuario y Entorno de Desarrollo

1. **Dispositivo del Usuario:** El usuario interactúa y prueba la aplicación directamente en un **teléfono móvil** (sin ordenador PC ni puente ADB por cable).
2. **Canal de Distribución:** La aplicación se distribuirá en plataformas abiertas como **Uptodown** o mediante **instalación directa de APKs** de terceros, no orientada prioritariamente a Google Play.
3. **Restricción de Marcas:** Prohibido utilizar nombres o marcas registradas con derechos de autor en el código, paquete o recursos que puedan poner en riesgo al usuario.
4. **Preferencia Estética:** Al usuario **no le gusta el minimalismo extremo**. La interfaz debe contar con elementos visuales ricos, tarjetas descriptivas, contadores en tiempo real, chips y estados claros, dividida en pantallas dedicadas en lugar de condensarlo todo en una sola pantalla.

---

## ⚙️ Directrices de Arquitectura y Tecnologías

### 1. Soporte de Arquitecturas (32 y 64 bits)
- Es estrictamente obligatorio mantener el soporte para las siguientes ABIs en `build.gradle.kts`:
  - `arm64-v8a` (ARM 64 bits)
  - `armeabi-v7a` (ARM 32 bits)
  - `x86_64` y `x86` (Entornos virtuales)
- Nunca asumas que el usuario tiene un procesador de 64 bits de gama alta; las optimizaciones deben funcionar sin sobrecargar chips MediaTek de 32 bits.

### 2. Motor Nativo (C++20 y OpenGL ES)
- El proyecto utiliza **CMake 3.22.1+** con estándar **C++20** (`-std=c++20`).
- Librerías del sistema enlazadas en `CMakeLists.txt`:
  - `GLESv2` (OpenGL ES 2.0/3.0)
  - `EGL` (Gestión de contextos de renderizado fuera de pantalla)
  - `mediandk` (`AMediaCodec`, `AMediaMuxer` para codificación de baja latencia)
  - `log` (`__android_log_print`)
  - `android` (`ANativeWindow`)
- El puente JNI se gestiona mediante `com.example.service.NativeBridge`.

### 3. Capa Kotlin y UI
- **Jetpack Compose + Material 3:** Navegación modular entre pantallas (`RecordScreen`, `LibraryScreen`, `SettingsScreen`) mediante `NavHost`.
- **Room Database:** Todas las grabaciones y sus metadatos (tamaño, duración, resolución, fecha) se persisten en local con `AppDatabase`.
- **Servicio en Primer Plano:** `ScreenRecorderService` maneja `MediaProjection` como servicio en primer plano para evitar que el sistema operativo lo cierre cuando el usuario abre otras aplicaciones o juegos.

---

## 📝 Documentación en Código
- Todos los archivos fuente de Kotlin y C++ deben incluir comentarios y docstrings explicativos en **español**, detallando la lógica de negocio y la justificación técnica de cada componente para facilitar su lectura en pantalla móvil.
