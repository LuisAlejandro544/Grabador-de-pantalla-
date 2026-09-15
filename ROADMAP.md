# Plan de Ruta (ROADMAP)

Este documento traza las fases de evolución del **Grabador de Pantalla**, desde el núcleo funcional actual hasta la integración completa de aceleración nativa y funciones multimedia avanzadas.

---

## 📍 Fase 1: Cimientos y Arquitectura Base *(Completada)*
- [x] Arquitectura MVVM desacoplada con Jetpack Compose y Material 3.
- [x] Navegación multi-pantalla mediante barra inferior (Grabar, Grabaciones, Ajustes).
- [x] Servicio de captura en primer plano (`ScreenRecorderService`) con notificación interactiva y tipo `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`.
- [x] Persistencia local con base de datos **Room** (`AppDatabase`, `RecordingDao`, `RecordingEntity`).
- [x] Reproductor de video nativo integrado (`VideoPlayerDialog`).
- [x] Integración de `FileProvider` para compartir videos con apps externas.
- [x] Configuración de **CMake** y **C++20** con enlaces a **OpenGL ES (`GLESv2`, `EGL`)** y **MediaNDK**.
- [x] Soporte multi-arquitectura para procesadores de 64 bits (`arm64-v8a`) y 32 bits (`armeabi-v7a`).

---

## 📍 Fase 2: Motor Gráfico OpenGL ES para Frame Pacing *(En Curso)*
- [ ] Implementar contexto EGL fuera de pantalla (*Off-screen EGLSurface*) en la capa C++.
- [ ] Creación de shader de paso simple (Pass-through vertex y fragment shader) para recibir el stream de `VirtualDisplay`.
- [ ] Regulación estricta de cadencia de fotogramas (*Frame Pacing*): sincronizar el refresco de pantallas de 90 Hz y 120 Hz a 60 FPS fijos sin sobrecargar la GPU.
- [ ] Reescalado acelerado por hardware en GPU (por ejemplo, escalar pantalla 1080x2400 a 720p sin tocar la CPU).

---

## 📍 Fase 3: Pipeline de Codificación Nativa con MediaNDK
- [ ] Conectar la salida del contexto EGL directamente a `AMediaCodec` mediante `ANativeWindow`.
- [ ] Implementar el bucle de escritura de paquetes MP4 con `AMediaMuxer` en C++20.
- [ ] **Beneficio clave:** Cero pausas de Recolección de Basura (*Garbage Collection*) de la máquina virtual durante partidas de videojuegos intensos.

---

## 📍 Fase 4: Herramientas Multimedia Avanzadas
- [ ] **Cámara frontal flotante (Facecam):** Ventana circular arrastrable sobre cualquier aplicación con soporte de transparencia.
- [ ] **Pincel y pizarra en pantalla:** Capa de dibujo flotante para realizar anotaciones o tutoriales mientras se graba.
- [ ] **Audio interno (Android 10+):** Captura directa del sonido de los juegos mediante la API `AudioPlaybackCaptureConfiguration`.
- [ ] **Opciones de temporizador de parada:** Detener automáticamente la grabación tras un tiempo especificado o por límite de tamaño de archivo.

---

## 📍 Fase 5: Optimización para Distribución Externa (Uptodown / APK)
- [ ] Configuración de firma release independiente sin dependencias exclusivas de Google Play.
- [ ] Pruebas y optimizaciones específicas para procesadores de gama de entrada (chips MediaTek Helio y Unisoc).
- [ ] Verificación de políticas y permisos estrictos sin solicitar accesos innecesarios al almacenamiento global.
