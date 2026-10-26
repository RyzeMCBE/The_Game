# El mundo de Noah — Android offline

Aplicación Android nativa en Kotlin para Noah, de 3 años. Todos los dibujos, voces, sonidos y juegos vienen dentro de la APK: funciona sin conexión desde el primer inicio. No contiene WebView, cuentas, publicidad, servidor ni permisos de red, micrófono o almacenamiento compartido.

## Edición 1.2.0

- Diseño infantil con escenarios suaves, Milo, tarjetas de papel, contornos claros y controles grandes.
- **200 láminas distintas**: 40 animales, 30 dinosaurios y escenas prehistóricas, 30 vehículos, 30 de naturaleza, 30 frutas/alimentos y 40 juguetes/objetos. Cada lámina tiene geometría propia; no son copias recoloreadas.
- Primero se abre el álbum. Al elegir una hoja, el lienzo ocupa su propia pantalla, con 12 colores, tres grosores, borrador y deshacer. En horizontal las herramientas pasan al lateral.
- El color sigue el movimiento del dedo. La ayuda recorta cada trazo a la zona donde comienza, incluidas divisiones internas y contornos; para otra parte se levanta el dedo. No se rellena toda la zona con un toque. Las áreas se calculan localmente a partir de los contornos y solo al abrir una hoja.
- «¡Terminé!» guarda la pintura, muestra la estrella feliz con fuegos artificiales y abre otra lámina. Se conserva también la hoja libre.
- La selección automática mezcla el catálogo sin repetir hasta recorrer las 200 hojas. Intenta alternar categorías y, en la siguiente vuelta, evita los últimos 20 dibujos mientras haya alternativas. El recorrido se guarda incluso al cerrar la app. Elegir voluntariamente un dibujo conocido en el álbum sigue siendo posible.
- Borradores por hoja, galería privada y compatibilidad con las pinturas/plantillas anteriores.
- **Contamos con Milo**, del 1 al 10: contar tocando cada objeto una sola vez, darle a Milo la cantidad de manzanas solicitada y relacionar cantidades con números. Rangos 1–3, 1–5 y 1–10, elegibles o con avance gradual por actividades completadas.
- **15 animales** con nombres hablados y grabaciones reales: vaca, perro, gato, pájaro, pato, cerdo, oveja, cabra, caballo, gallina, león, elefante, mono, rana y búho. Exploración libre y juego para reconocer el sonido.
- Cuatro juegos nuevos: encontrar sombras, grande/pequeño, clasificar por un solo atributo (color o forma) y seguir caminos anchos con el dedo.
- Circuito: colores → formas → conteo → animales → parejas → rompecabezas → sombras → tamaños → clasificar → caminos → colores. Se avanza tras terminar tanto la narración como la animación, sin botones de continuación.
- Milo usa 128 grabaciones con registro/timbre de personaje infantil ficticio, pronunciación española latinoamericana y personalización con Noah. No es la grabación de un niño real.

La estrella mantiene su rebote, sonrisa, guiño, saludo, fuegos artificiales, confeti y sonido suave. Inicio cancela las transiciones; pasar a segundo plano detiene voz y animación. La recreación conserva el progreso y evita duplicar las recompensas.

## Datos y actualización

Android **6.0+**. Se mantiene el identificador `com.ryzemc.pequenomundo.noah`, la firma del propietario y una versión superior (1.2.0, código 3), por lo que la entrega personal actualiza la edición Noah 1.1.0 conservando sus datos. La primera APK antigua, con otro identificador, mantiene sus propios datos independientes.

Las pinturas, borradores, recorrido y avances quedan en este dispositivo. Desinstalar la aplicación elimina sus datos. La zona Familia está protegida por una suma y permite ajustar voz, música suave, dificultad, progreso y descansos a los 5/10/15/20 minutos. El reloj cuenta solo el juego en primer plano. Allí se pueden consultar también los créditos de dibujos, voces y sonidos.

## Compilar

JDK 17, Android SDK 35 y Build Tools 35.0.0. Android Studio puede abrir la raíz. Configura `ANDROID_HOME` o `sdk.dir` en `local.properties`.

```sh
python3 tools/check_assets.py
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest
python3 tools/check_assets.py --apk app/build/outputs/apk/debug/app-debug.apk
./gradlew connectedDebugAndroidTest
```

La APK personal usa una clave de propietario conservada fuera de GitHub: `NOAH_SIGNING_STORE` (PKCS12), `NOAH_SIGNING_PASSWORD`, alias `noah-release`. Ejecuta `./gradlew assembleRelease`. Conserva en privado `mi-pequeno-mundo-noah-signing-backup.zip` y usa esa misma clave para próximas actualizaciones. Los artefactos de Actions llevan firma de depuración y el sufijo **test**, distinto de la entrega personal.

El wrapper verifica Gradle 8.9 por SHA-256. Las dependencias se descargan al compilar; nunca al jugar. Los 200 dibujos y las 145 pistas WAV están versionados: una compilación normal no requiere modelos de voz, Piper, WORLD, FFmpeg ni generadores de dibujos.

## Verificación

Las pruebas JVM cubren el circuito, conteo exacto hasta diez, errores y recompensas únicas, selección de las 200 hojas entre reinicios, historial reciente, clasificar, seguir caminos, parejas, piezas y reloj.

Las pruebas Android recorren la interfaz nativa con wifi/datos desactivados: separación de álbum/lienzo, todos los colores, pintura manual y borrador sin salir de las zonas, divisiones internas, las 200 láminas, guardado y siguiente dibujo, rotación y horizontal, formatos antiguos, persistencia de la selección, los tres juegos de conteo y el circuito completo. CI guarda informes y capturas para inspección interna. La prueba de uso en el dispositivo concreto de Noah sigue siendo necesaria.

`tools/check_assets.py` audita geometría distinta, cantidades por categoría, narraciones, licencias/procedencia/hashes de los animales, medios empaquetados sin compresión y ausencia de permisos/red/WebView/modelos.

## Contenido y autoría

`DrawingCatalog.kt` carga los vectores y separa las zonas. `PaintCanvasView.kt` pinta/borra dentro de ellas. `GameEngine.kt` contiene modelos independientes de Android; `GamePanels.kt` y `Storybook.kt`, los controles y escenas nativos; `MainActivity.kt`, navegación y ciclo de vida. `LocalStore.kt` conserva datos privados y escribe imágenes en segundo plano.

Las herramientas opcionales de autoría están en `tools/`: `generate_drawings.py` genera el catálogo original; `render_drawings_qa.py` crea hojas de contacto para revisión visual (`cairosvg` y `pillow` opcionales); `generate_audio.py` produce la narración infantil; `prepare_animal_audio.py` prepara las grabaciones licenciadas. Procedencia y licencias: `THIRD_PARTY_NOTICES.md` y los créditos dentro de la APK.
