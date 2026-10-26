# Mi pequeño mundo

Aplicación **Android nativa en Kotlin**, para jugar, pintar y descubrir con un niño de 3 años. El APK contiene las ilustraciones, las voces en español, los sonidos y la música. Funciona sin conexión desde el primer inicio. No usa WebView, servidor, cuentas, publicidad ni servicios de voz del teléfono.

## Actividades incluidas

- Pintura libre, dibujos de casita, flor, osito y pez para colorear por zonas; siete colores, sellos y deshacer.
- Galería privada de dibujos PNG. El borrador se conserva al salir y al girar la pantalla. Al cambiar de plantilla se guarda el dibujo anterior.
- Colores y figuras: reconocer y tocar la opción correspondiente.
- Números del 1 al 5 relacionando manzanas y cantidades.
- Cinco animales, con ilustraciones y sonidos incluidos.
- Parejas con dos o tres pares y rompecabezas de dos o cuatro piezas; se puede tocar una pieza y luego su sitio, o arrastrarla.
- Indicaciones habladas, repetición de instrucciones y mensajes amables para volver a intentar.
- Zona de adultos protegida por una suma, con voz, música opcional, dificultad, progreso y recordatorio de descanso a los 5, 10, 15 o 20 minutos. El reloj solo cuenta tiempo de juego en primer plano.

Los datos se guardan en el almacenamiento privado de la aplicación. No se solicitan permisos de internet, micrófono ni acceso al almacenamiento compartido. Al desinstalar la aplicación, Android borra sus datos.

## Instalar

Compatible con **Android 6.0 o posterior**. Descarga el APK del artefacto `mi-pequeno-mundo-apk` de [GitHub Actions](https://github.com/RyzeMCBE/The_Game/actions/workflows/android.yml), extráelo e instálalo en el dispositivo. Android puede pedir permitir la instalación desde la aplicación con la que abriste el archivo.

La entrega inicial es un APK de prueba firmado con la clave de depuración. Antes de distribuir versiones públicas o establecer actualizaciones permanentes se debe configurar una clave de publicación privada y conservarla; no se debe añadir esa clave al repositorio.

## Compilar

Requisitos: JDK 17, Android SDK 35 y Build Tools 35.0.0. Android Studio puede abrir la carpeta raíz. Configura `ANDROID_HOME` o `sdk.dir` en tu `local.properties` local.

```sh
python3 tools/check_assets.py
./gradlew testDebugUnitTest assembleDebug
python3 tools/check_assets.py --apk app/build/outputs/apk/debug/app-debug.apk
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

El Gradle Wrapper verifica el SHA-256 de Gradle 8.9. Las dependencias se descargan **durante la compilación**, no cuando el niño instala o usa la aplicación. Los WAV están versionados: no hace falta instalar Piper ni descargar un modelo de voz para compilar.

## Verificación

- Pruebas JVM: generación y variedad de opciones, una recompensa por reto, parejas, colocación de piezas y reloj de primer plano.
- Pruebas Android: navegación nativa, recursos empaquetados, ausencia de permisos de red, pintura, persistencia al recrear la actividad, guardado/galería, aprendizaje y puerta de adultos; también serialización, relleno y deshacer.
- El flujo `Android APK` compila el APK, publica su checksum y ejecuta las pruebas Android en un emulador API 35 con wifi y datos deshabilitados.

```sh
./gradlew connectedDebugAndroidTest
```

Para probar manualmente: instala en modo avión, recorre las tres áreas, guarda un dibujo, cierra y abre la app, gira la pantalla durante una actividad y comprueba el recordatorio de descanso desde la zona de adultos. Las pruebas automatizadas no sustituyen una prueba en el teléfono o tablet del niño.

## Organización

- `GameEngine.kt`: lógica de retos, parejas, piezas y reloj, sin dependencia de Android.
- `Artwork.kt`: ilustraciones vectoriales originales dibujadas con Canvas.
- `PaintCanvasView.kt`: pintura táctil, regiones, sellos y borrador serializado.
- `LocalStore.kt`: ajustes, progreso y escrituras locales atómicas en segundo plano.
- `AudioController.kt`: reproducción exclusiva de WAV empaquetados y liberación al salir.
- `MainActivity.kt`, `PuzzlePanel.kt`: interfaz Android y navegación.
- `tools/generate_audio.py`: herramienta opcional para regenerar grabaciones; véase `THIRD_PARTY_NOTICES.md`.
