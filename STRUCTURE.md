# Estructura del Proyecto (STRUCTURE)

Este documento describe la organización modular de los archivos, carpetas y componentes del Grabador de Pantalla, diseñado para garantizar un código desacoplado, mantenible y escalable.

---

## 🌳 Árbol de Directorios

```text
/
├── app/
│   ├── build.gradle.kts                   # Configuración de compilación de la app, NDK y dependencias
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml        # Declaración de permisos, servicios y FileProvider
│   │   │   ├── cpp/                       # MÓDULO NATIVO (C++20 y OpenGL ES)
│   │   │   │   ├── CMakeLists.txt         # Configuración de CMake y enlaces (GLESv2, EGL, mediandk, log)
│   │   │   │   └── native-lib.cpp         # Implementación C++20 con JNI y headers nativos
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt        # Actividad principal, Scaffold y navegación con barra inferior
│   │   │   │   ├── data/                  # CAPA DE DATOS Y PERSISTENCIA
│   │   │   │   │   ├── AppDatabase.kt     # Base de datos Room
│   │   │   │   │   ├── RecordingDao.kt    # Data Access Object con consultas SQL
│   │   │   │   │   ├── RecordingRepository.kt # Repositorio para gestión de videos y ficheros
│   │   │   │   │   └── RecorderConfig.kt  # Clases y enums de configuración (FPS, Bitrate, Resolución)
│   │   │   │   ├── model/                 # ENTIDADES Y MODELOS
│   │   │   │   │   └── RecordingEntity.kt # Entidad Room para representar cada video grabado
│   │   │   │   ├── service/               # SERVICIOS Y COMUNICACIÓN NATIVA
│   │   │   │   │   ├── ScreenRecorderService.kt # Foreground Service con MediaProjection
│   │   │   │   │   └── NativeBridge.kt    # Enlace JNI entre Kotlin y C++20/OpenGL
│   │   │   │   ├── ui/                    # CAPA DE INTERFAZ (Jetpack Compose)
│   │   │   │   │   ├── components/
│   │   │   │   │   │   └── VideoPlayerDialog.kt # Reproductor nativo de video integrado
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   └── Screen.kt      # Definición de rutas e iconos de las 3 pantallas
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── RecordScreen.kt # Pantalla 1: Consola de grabación y estado en vivo
│   │   │   │   │   │   ├── LibraryScreen.kt# Pantalla 2: Biblioteca de videos, compartir y renombrar
│   │   │   │   │   │   └── SettingsScreen.kt# Pantalla 3: Ajustes técnicos e info de hardware
│   │   │   │   │   └── theme/             # ESTILOS Y TEMAS MATERIAL 3
│   │   │   │   │       ├── Color.kt       # Paleta de colores de estudio de grabación
│   │   │   │   │       ├── Theme.kt       # Esquemas Dark y Light
│   │   │   │   │       └── Type.kt        # Tipografías
│   │   │   │   └── viewmodel/
│   │   │   │       └── RecorderViewModel.kt # ViewModel central con StateFlow
│   │   │   └── res/                       # RECURSOS (Iconos, Strings, XML)
│   │   │       ├── values/strings.xml     # Textos localizados
│   │   │       └── xml/file_paths.xml     # Rutas seguras para FileProvider
│   │   └── test/                          # PRUEBAS LOCALES JVM
├── AGENTS.md                              # Reglas e instrucciones persistentes para agentes IA
├── AI_CONTEXT.md                          # Contexto técnico y decisiones de ingeniería
├── ROADMAP.md                             # Plan de fases futuras de desarrollo
├── STRUCTURE.md                           # Mapa estructural del código fuente
├── README.md                              # Documentación general y guía de compilación
├── metadata.json                          # Metadatos del entorno AI Studio
└── settings.gradle.kts                    # Configuración raíz de Gradle
```

---

## 🧩 Responsabilidad por Módulo

1. **`cpp/` (Motor Nativo):**
   - Compilado con el estándar **C++20** (`-std=c++20`, `-O3`).
   - Conectado a `GLESv2` (OpenGL ES) y `EGL` para futuras operaciones de filtrado gráfico y regulación de FPS.
   - Conectado a `mediandk` para acceso directo a `AMediaCodec` y `AMediaMuxer`.

2. **`service/` (Capa de Ejecución en Segundo Plano):**
   - Gestiona el ciclo de vida de `MediaProjection` y `MediaRecorder`.
   - Garantiza que la grabación no se interrumpa cuando el usuario sale a jugar o usar otra app mediante un `Foreground Service` tipado.
   - Emite el estado y la duración en tiempo real a través de un `MutableStateFlow` estático para que cualquier pantalla pueda observarlo.

3. **`data/` y `model/` (Persistencia Local):**
   - **Room Database** almacena los registros con su duración exacta, tamaño en bytes, resolución, fecha y estado de audio.
   - El repositorio limpia automáticamente los archivos físicos huérfanos del almacenamiento cuando se elimina un registro.

4. **`ui/` (Presentación Modular):**
   - Totalmente separada en 3 pantallas independientes conectadas por un `NavHost` y una barra inferior de navegación (`NavigationBar`).
   - Diseñada siguiendo Material Design 3 con estados animados y soporte para modo oscuro y claro.
