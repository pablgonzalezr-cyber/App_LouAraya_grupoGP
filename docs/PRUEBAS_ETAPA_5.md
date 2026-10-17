# Etapa 5 — Escáner QR y mapa

Implementación del 17 de octubre de 2026 en `feature/cotizaciones`. Persistencia local; sin integración REST ni sincronización entre teléfonos. Se conserva AppDatabase versión 5, sus esquemas y todas las migraciones anteriores. Favoritos y catálogo existentes se reutilizan sin cambios en sus bases.

## Funcionalidades

- Cámara real con CameraX Preview + ImageAnalysis, detector ML Kit limitado a QR y modelo incluido en el APK. No necesita descargar el modelo al escanear.
- Permiso de cámara solicitado únicamente con «Permitir cámara», dentro del escáner. Denegación, ajustes del sistema, reintento, ausencia de cámara y errores de uso gestionados. Se liberan los recursos al salir.
- Formato estricto `LOUARAYA:PRODUCTO:<id positivo>`. La existencia y publicación se verifican en Room, incluyendo productos creados posteriormente a los ocho iniciales. No se aceptan automáticamente URLs ni códigos arbitrarios.
- Se bloquean nuevas lecturas durante la consulta y hasta reintentar un error. Un resultado válido abre el detalle existente; no crea otra implementación del catálogo.
- Generador de QR para cualquier producto publicado. Exportación PNG mediante selector de documentos, sin permiso general de almacenamiento.
- Mapa nativo interactivo MapLibre OpenGL: desplazamiento, zoom táctil/botones, marcadores seleccionables y fichas con nombre, descripción y dirección de demostración.
- Tres puntos explícitamente ficticios en coordenadas de demostración de Santiago. No representan locales ni eventos confirmados. La ficha puede abrir una aplicación externa de mapas; si no existe una compatible, se informa.
- MapLibre + OpenFreeMap no requiere clave API. Se conserva atribución del proveedor y de los datos. Necesita Internet para cargar cartografía; incluye error, espera máxima y reintento. No se implementan descargas offline.
- No se solicita ubicación. Los permisos de ubicación opcionales que aporta la biblioteca se excluyen explícitamente del manifiesto combinado.
- Accesos desde Dashboard cliente. Rutas `escaner`, `qr-demo` y `mapa` protegidas por la política de sesión/rol actual; no se amplían privilegios administrativos.

## Archivos creados

Rutas relativas a `app/src/main/java/com/example/applouaraya_grupogp/`:

- `model/PuntoDemostracion.kt`
- `repository/QrRepository.kt`, `repository/QrImagen.kt`, `repository/PuntosDemostracion.kt`
- `viewmodel/QrViewModels.kt`, `viewmodel/MapaViewModel.kt`
- `ui/components/CamaraQr.kt`, `ui/components/MapaInteractivo.kt`
- `ui/screens/QrScreens.kt`, `ui/screens/MapaScreen.kt`

En los siguientes archivos, `...` corresponde exactamente a `com/example/applouaraya_grupogp`:

- `app/src/test/java/.../MapaTest.kt`
- `app/src/androidTest/java/.../QrInstrumentadoTest.kt`
- `app/src/androidTest/java/.../CamaraQrTest.kt`
- `app/src/androidTest/java/.../MapaInstrumentadoTest.kt`
- Este documento: `docs/PRUEBAS_ETAPA_5.md`.

## Archivos modificados

- `app/build.gradle.kts`: CameraX 1.5.3, ML Kit barcode-scanning 17.3.0, ZXing core 3.5.4, MapLibre android-sdk-opengl 13.7.1 y regla de permiso exclusivamente para pruebas.
- `app/src/main/AndroidManifest.xml`: cámara opcional, permiso de cámara, conectividad y exclusión de permisos de ubicación de la biblioteca.
- `navigation/AppNavigation.kt`, `navigation/PoliticaNavegacion.kt`: rutas y controles existentes.
- `ui/screens/DashboardScreen.kt`: dos accesos.
- `repository/ProductoQr.kt`: análisis del formato independiente de la lista estática, permitiendo IDs nuevos en Room.
- `app/src/test/java/.../ProductoQrTest.kt`, `PoliticaNavegacionTest.kt`: formato y rutas nuevas.
- `app/src/androidTest/java/.../NavegacionPerfilTest.kt`: rechazo de las tres rutas nuevas sin sesión.

## Seguridad de las pruebas

Se inspeccionaron los casos instrumentados antes de ejecutarlos. Las pruebas de persistencia usan Room en memoria o nombres exclusivos `prueba-...` con UUID; solo eliminan esas bases de prueba al terminar. Las pruebas de UI existentes inyectan sus repositorios de prueba y preferencias independientes. No se abre ni elimina la base principal ni la de favoritos reales.

Los casos nuevos de cámara y mapa no abren bases de datos. La prueba de cámara concede CAMERA mediante GrantPermissionRule y puede dejar ese permiso concedido tras finalizar. Para probar la denegación, cambia únicamente ese permiso desde Ajustes Android; no borres datos ni desinstales.

Se usa `adb install -r` para actualizar ambos APK y se invoca el runner directamente: no se ejecuta una tarea que desinstale la aplicación al terminar. Teléfono físico ADB `R5CY42JVXMT`, Samsung SM-A166M; ningún emulador.

## Resultados finales

| Comprobación | Resultado |
| --- | --- |
| `assembleDebug` | BUILD SUCCESSFUL |
| `assembleDebugAndroidTest` | BUILD SUCCESSFUL |
| `testDebugUnitTest` | 45 pruebas, 0 fallos, 0 errores |
| `lintDebug` | BUILD SUCCESSFUL; 0 errores, 40 advertencias |
| Instrumentadas en teléfono físico | OK (25 tests), 84.733 segundos |
| Esquema Room | Versión 5 sin cambios ni nuevas migraciones |
| Manifiesto combinado | minSdk 26; sin ACCESS_FINE_LOCATION ni ACCESS_COARSE_LOCATION |

Las 25 instrumentadas incluyen los 20 casos existentes y 5 nuevos: cámara física (1), resolución QR en Room/decodificación real/control de duplicados (3), mapa nativo con estilo remoto, zoom y ficha (1). Se aprobaron también las migraciones históricas, los permisos administrativos, persistencia de productos y favoritos, colecciones/campañas, solicitudes y perfil/navegación.

Secuencia de verificación: primero aprobaron las 4 instrumentadas del escáner antes de incorporar el mapa (7.596 s). La primera suite completa terminó con 24/25: la prueba de cámara consultaba el árbol semántico Compose demasiado pronto (`No compose hierarchies found`). Se corrigió solo esa sincronización: ahora espera el callback real de fotograma analizado y comprueba ausencia de errores. Un intento posterior perdió la conexión ADB antes de completar el reporte; no se contabilizó como resultado final. La última suite completa aprobó 25/25.

Las advertencias de lint incluyen versiones más nuevas de dependencias, uso recomendado de catálogo de versiones/KTX, recursos sin uso, lectura de recursos desde LocalContext y etiqueta redundante. No impidieron compilar; no se actualizaron módulos ajenos para resolverlas.

Evidencia local generada:

- `app/build/reports/instrumentadas-etapa5-qr.txt`
- `app/build/reports/instrumentadas-etapa5-final.txt`
- `app/build/reports/instrumentadas-etapa5-primer-intento.txt`
- `app/build/reports/instrumentadas-etapa5-interrumpida-adb.txt`
- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/reports/lint-results-debug.html`

Estos informes están bajo `build` y no forman parte de los archivos fuente a versionar. No se hicieron commits, push ni merges. No se desinstaló ni se borraron datos de la aplicación principal.

## Comandos reproducibles

Desde PowerShell en la raíz del proyecto:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --offline --no-configuration-cache --max-workers=1
$adbLou = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
& $adbLou devices -l
& $adbLou -s R5CY42JVXMT install -r app/build/outputs/apk/debug/app-debug.apk
& $adbLou -s R5CY42JVXMT install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adbLou -s R5CY42JVXMT shell am instrument -w -r com.example.applouaraya_grupogp.test/androidx.test.runner.AndroidJUnitRunner
```

Las dependencias ya se descargaron durante esta implementación. En un computador sin caché, omite `--offline` y permite que Gradle acceda a los repositorios configurados. El mapa instrumentado requiere Internet en el teléfono.

## Prueba manual en el teléfono

1. Abre la versión actualizada conservando tu cuenta e inicia sesión como CLIENTE. Desplázate hasta los nuevos accesos del Dashboard.
2. Entra a «Escanear QR». Si aún no está concedido, pulsa «Permitir cámara». Debe aparecer la imagen real y «Cámara activa».
3. Abre «Ver QR de demostración», elige un producto publicado y usa «Guardar QR como imagen». Muestra ese PNG en otro dispositivo o computador; vuelve al escáner y apunta hacia la imagen. Debe abrirse el detalle correspondiente con sus favoritos habituales. No se puede escanear con la cámara un QR mostrado simultáneamente en la misma pantalla.
4. Para comprobar errores, muestra QRs con `OTRO:42`, con `LOUARAYA:PRODUCTO:2147483647` si ese ID no existe, o con el ID de un producto desactivado. Espera el mensaje correspondiente y pulsa «Escanear de nuevo». No es necesario alterar datos personales para el primer y segundo caso.
5. Comprueba que mantener un código delante de la cámara no abra múltiples detalles. Usa Atrás, abre de nuevo el escáner y cambia entre aplicaciones para comprobar liberación y recuperación de cámara.
6. Opcionalmente, deniega solo CAMERA en Ajustes Android y abre el escáner. Debe explicar el permiso, permitir volver y abrir ajustes sin bloquear el resto de la app.
7. En Dashboard abre «Mapa de ferias y showrooms» con Internet. Debe cargar cartografía real, permitir arrastrar y pellizcar, y responder a «Zoom +/−».
8. Toca un marcador terracota o un selector inferior. Debe mostrar el punto ficticio, su descripción y las coordenadas sin presentar una dirección comercial inventada. «Indicaciones a punto ficticio» abre una app externa si está instalada; no confirma ninguna feria real.
9. Sin conectividad puede funcionar la caché existente o aparecer el mensaje de carga/error. Recupera Internet y pulsa «Reintentar mapa» si aparece. No se promete funcionamiento offline.
10. Cierra sesión: Atrás y accesos privados no deben recuperar las pantallas. Revisa catálogo, favoritos, perfil, solicitudes y administración con las cuentas habituales.

## Alcance de la verificación y limitaciones

La prueba de cámara verifica fotogramas del sensor físico procesados por el detector real. Otra prueba genera un bitmap QR con ZXing y lo decodifica con ML Kit. La lectura óptica completa de un QR externo y el flujo de permiso denegado requieren la comprobación manual anterior; no se presentan como automatizados.

La prueba del mapa utiliza el estilo remoto real, verifica la vista nativa, los cambios efectivos de zoom y la selección de una ficha. No sustituye el mapa por una imagen ni por un componente falso. La disponibilidad del proveedor público puede afectar esa prueba.

Los QRs identifican IDs locales: distintos teléfonos pueden tener productos diferentes con el mismo ID. No son códigos de autenticación ni implican sincronización remota. El mapa usa únicamente datos de demostración y el servidor cartográfico público indicado; no es un backend de la asignatura.

## Fuentes y condiciones de dependencias

- [CameraX: versiones](https://developer.android.com/jetpack/androidx/releases/camera) y [análisis de imagen](https://developer.android.com/media/camera/camerax/analyze).
- [ML Kit Android: escaneo y modelo incluido](https://developers.google.com/ml-kit/vision/barcode-scanning/android).
- [ZXing: código y licencia Apache 2.0](https://github.com/zxing/zxing).
- [MapLibre Native: instalación](https://maplibre.org/maplibre-native/android/examples/getting-started/) y [variantes OpenGL/Vulkan](https://maplibre.org/maplibre-native/android/examples/artifacts/). MapLibre Native usa licencia BSD de 2 cláusulas; conservar avisos de terceros al distribuir.
- [OpenFreeMap](https://openfreemap.org/), [estilo público y atribución](https://openfreemap.org/quick_start/) y [términos del servicio público](https://openfreemap.org/tos/): sin clave/cuenta obligatoria y sin garantía de disponibilidad. Se mantienen los avisos de OpenStreetMap y OpenMapTiles; no se redistribuyen bases cartográficas ni se hacen descargas masivas.
- [Intents de indicaciones](https://developer.android.com/guide/components/google-maps-intents).
