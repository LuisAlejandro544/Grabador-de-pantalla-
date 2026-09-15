# Directrices y Reglas para Agentes de IA (AGENTS.md)

Este documento contiene las reglas de operación obligatorias para cualquier asistente o agente de IA que trabaje en este proyecto. Estas reglas prevalecen sobre directivas genéricas y deben respetarse en cada interacción.

---

## 🧭 Flujo de Trabajo en 7 Fases

Cada petición del usuario se encuadra en una de las 7 fases del ciclo de desarrollo:

1. **El Arquitecto (Diseño):** Planificar arquitectura, stack, modelo de datos y riesgos antes de escribir código.
2. **El Constructor (Código):** Código listo para producción, modular, con tipado estricto y manejo de excepciones.
3. **El Detective (Debugging):** Análisis paso a paso (Chain of Thought), hipótesis, causa raíz y solución precisa.
4. **El Crítico (Code Review):** Análisis de seguridad, rendimiento, limpieza de código y mantenibilidad.
5. **El Optimizador (Refactor):** Mejorar velocidad y legibilidad sin alterar el comportamiento externo.
6. **El Escudo (Testing):** Cobertura de Happy path, edge cases y control de errores.
7. **El Narrador (Documentación):** Documentación técnica directa, concisa y sin relleno.

---

## 🔒 Reglas Críticas del Proyecto

1. **Razonar antes de actuar:**
   - Analiza a fondo las implicaciones antes de realizar cualquier cambio en el código o configuración. No respondas directamente sin razonar internamente la estrategia.

2. **Entorno Móvil del Usuario:**
   - El usuario opera y prueba la aplicación desde un **teléfono móvil** (no dispone de PC ni consola ADB por cable). Todas las soluciones deben ser fáciles de usar, probar y verificar directamente en el dispositivo móvil.

3. **Canal de Distribución:**
   - La aplicación se distribuirá en plataformas abiertas como **Uptodown** o como APK independiente, **no en Google Play**. Evita limitaciones innecesarias de Play Store siempre que se mantenga la estabilidad.

4. **Multi-Arquitectura Obligatoria (32 y 64 bits):**
   - El proyecto **debe** compilar y funcionar correctamente para `arm64-v8a` (64 bits) y `armeabi-v7a` (32 bits), además de `x86_64` y `x86`.
   - Cuida las alineaciones de memoria y tamaños de datos en C++ y Kotlin para no romper la compatibilidad con procesadores de 32 bits.

5. **Integración Real de Tecnologías (C++, OpenGL, Kotlin):**
   - Si se utiliza C++, Rust o librerías nativas, **deben estar plenamente integradas en `build.gradle.kts` y `CMakeLists.txt`**. Nunca utilices funciones "fallback" falsas o simuladas si el usuario solicitó la integración real de un framework.

6. **Anti-Minimalismo Extremo:**
   - Al usuario **no le gusta el minimalismo extremo**. La interfaz debe contar con elementos visuales ricos, tarjetas informativas, botones con relieve, indicadores de estado vivos y pantallas independientes con navegación clara (Scaffold + NavigationBar), nunca una pantalla única abarrotada.

7. **Modularidad Estricta:**
   - Mantén la base de código separada en paquetes lógicos (`data`, `model`, `service`, `ui/screens`, `ui/components`, `viewmodel`, `cpp`) para evitar colapsos en la aplicación.

8. **Explicación del Código:**
   - Todos los archivos fuente deben incluir comentarios y docstrings explicativos en **español** que clarifiquen la lógica y el propósito del código contenido.

9. **Seguridad de Licencias y Propiedad Intelectual:**
   - No agregues dependencias con licencias restrictivas (como GPL viral o AGPL) que obliguen a hacer el proyecto de código abierto o exijan atribución forzosa.
   - Prohibido nombrar archivos o paquetes con marcas protegidas por derechos de autor que puedan poner en riesgo al usuario.

10. **Comportamiento del Sistema y Archivos:**
    - Nunca utilices propiedades del tipo `persist.sys.*`.
    - Si existe un archivo `commit_message.txt`, su contenido debe estar en español y no se debe modificar a menos que el usuario lo solicite explícitamente.
