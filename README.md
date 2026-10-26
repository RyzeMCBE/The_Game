# Mi pequeño mundo: Noah

Aplicación **Android nativa en Kotlin**, personalizada para Noah, de 3 años. El APK incluye las ilustraciones, la voz infantil sintética de Milo, grabaciones reales de animales, música y animaciones. Funciona sin conexión desde el primer inicio, sin WebView, servidor, cuentas, publicidad ni voz descargada del teléfono.

## Edición 1.1.0

- Milo saluda y felicita a Noah y lo acompaña durante los descansos.
- 55 grabaciones con registro y timbre de personaje infantil, pronunciación española latinoamericana y duración pausada.
- Una estrella amarilla feliz entra con rebote, sonríe, guiña y saluda; alrededor aparecen fuegos artificiales de colores y confeti. Incluye un sonido suave de celebración.
- Al acertar, termina la respuesta hablada, se presenta la estrella con la felicitación y se avanza automáticamente cuando terminan **tanto la narración como la animación**. La celebración visual dura al menos tres segundos, también con las animaciones del sistema deshabilitadas.
- Circuito guiado: colores → formas → números → animales → parejas → rompecabezas → colores, con retos variados. Se puede empezar desde cualquier actividad del menú.
- Parejas y rompecabezas avanzan cuando se completa el tablero. Pintar conserva el modo libre. No hay botones de continuación.
- Salir a Inicio cancela la transición. Al pasar a segundo plano se detienen voz, efectos y animación; al volver se retoma la celebración pendiente. Recrear la actividad conserva el reto completado sin repetir la recompensa.
- Los cinco sonidos de animales son **grabaciones reales CC0** de vaca, perro, gato, pájaro y pato. Primero se escucha «¿Qué animal escuchas?» y el sonido; el nombre se dice al acertar.

## Pintura y acompañamiento

Pincel libre, cinco hojas/plantillas, relleno por zonas, siete colores, sellos, deshacer, borrador persistente y galería privada. Cambiar de plantilla guarda el dibujo anterior. Las imágenes se escriben en segundo plano.

La zona de adultos está protegida por una suma y permite ajustar voz, música opcional, dificultad (2/3 opciones, 2/3 parejas, 2/4 piezas), progreso y descansos a los 5, 10, 15 o 20 minutos. El reloj cuenta únicamente juego en primer plano.

Los dibujos y avances se guardan en el dispositivo. No se solicitan permisos de internet, micrófono ni acceso al almacenamiento compartido. Desinstalar la aplicación borra sus datos.

## Instalar y conservar las versiones

Android **6.0 o posterior**. Esta edición usa el identificador `com.ryzemc.pequenomundo.noah` y el nombre «Mi pequeño mundo: Noah». Se instala junto a la primera APK de prueba, que conserva sus propios dibujos y avances. Los datos de ambas aplicaciones son independientes.

El APK personal entregado se firma con una clave de propietario que debe conservarse para futuras actualizaciones. Las compilaciones de [GitHub Actions](https://github.com/RyzeMCBE/The_Game/actions/workflows/android.yml) usan firma de depuración y se identifican como APK de **prueba**; esa firma es distinta a la entrega personal.

## Compilar

JDK 17, Android SDK 35 y Build Tools 35.0.0. Android Studio puede abrir la carpeta raíz. Configura `ANDROID_HOME` o `sdk.dir` en tu `local.properties` local.

```sh
python3 tools/check_assets.py
./gradlew testDebugUnitTest assembleDebug
python3 tools/check_assets.py --apk app/build/outputs/apk/debug/app-debug.apk
```

Para una entrega con la clave del propietario, configura fuera del repositorio `NOAH_SIGNING_STORE` (archivo PKCS12) y `NOAH_SIGNING_PASSWORD`; alias `noah-release`. Ejecuta `./gradlew assembleRelease`. Conserva el respaldo privado `mi-pequeno-mundo-noah-signing-backup.zip`; nunca lo añadas a GitHub.

El Gradle Wrapper verifica el SHA-256 de Gradle 8.9. Las dependencias se descargan al compilar, nunca al usar el APK. Los audios están versionados: la compilación habitual no requiere Piper, WORLD, FFmpeg ni modelos de voz.

## Verificación

- Pruebas JVM: opciones y variedad de retos, recompensa única, parejas, piezas, reloj, circuito completo y coordinación/cancelación de voz y animación.
- Pruebas Android: recorrido nativo sin permisos de red, pintura/galería, borrador y rotación, personalización de Noah, celebración, circuito automático por los seis juegos, cancelación al volver a Inicio y espera de la narración completa.
- Auditoría del APK: 55 voces, cinco grabaciones reales con procedencia y SHA-256, música y sonido de celebración, todos empaquetados sin compresión.
- CI ejecuta las pruebas en un emulador Android 15 con wifi y datos deshabilitados y guarda capturas para inspección interna.

```sh
./gradlew connectedDebugAndroidTest
```

La prueba final de uso sigue siendo en el celular o tablet de Noah.

## Organización

`GameEngine.kt` contiene la lógica independiente de Android; `CelebrationView.kt` dibuja la estrella y fuegos artificiales; `MainActivity.kt` coordina pantallas y ciclo de vida; `AudioController.kt` reproduce solo WAV locales; `Artwork.kt`, `PaintCanvasView.kt`, `PuzzlePanel.kt` y `LocalStore.kt` implementan ilustraciones, pintura, piezas y persistencia.

Las herramientas opcionales para regenerar voces y preparar grabaciones reales están en `tools/`. Procedencia y licencias: `THIRD_PARTY_NOTICES.md`.
