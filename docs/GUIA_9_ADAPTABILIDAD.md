# Guía 9: diseño adaptable

## Implementación

La dependencia `androidx.compose.material3:material3-window-size-class` usa la versión 1.4.0 seleccionada por el BOM Compose 2026.02.01 existente. No se actualizó el BOM ni el SDK mínimo (26).

`AdaptiveSurface` obtiene el espacio disponible con `BoxWithConstraints` y lo clasifica con **Material 3 `WindowSizeClass.calculateFromSize`**. La clasificación ocurre antes de limitar el ancho del contenido. Al redimensionar la ventana se recompone la distribución; no se depende de un modelo de dispositivo u orientación fija.

| Ancho disponible | Clase Material 3 | Dashboard y catálogo | Formularios |
|---|---|---|---|
| Menos de 600 dp | Compact | Una columna | Ancho disponible |
| Desde 600 hasta menos de 840 dp | Medium | Dos columnas | Centrados, contenedor máximo 520 dp |
| Desde 840 dp | Expanded | Tres columnas | Centrados, contenedor máximo 560 dp |

Los máximos de formularios incluyen el padding. Bienvenida conserva 28 dp y los otros formularios 24 dp. Los dashboards tienen un contenedor máximo de 1200 dp. `AdaptiveColumn` comparte márgenes seguros, `imePadding` y desplazamiento vertical. Bienvenida y Login siguen centrados cuando hay altura suficiente y permiten desplazamiento cuando falta altura. Registro y Resumen conservan sus campos, estado y callbacks.

El catálogo reutiliza las tarjetas originales y los mismos filtros, detalles y callbacks de favoritos. Su encabezado ocupa todo el ancho de la cuadrícula y se desplaza con los productos, para permitir acceso al contenido en horizontal y con teclado.

La extracción de `CatalogoContenido` y `AdminDashboardContent` separa únicamente presentación de sus wrappers existentes. Las previews usan estados ficticios sin bases de datos ni sesión real. No cambian ViewModels, repositorios, navegación, Room, migraciones ni backend.

Referencia de la API: [Material 3 WindowSizeClass](https://developer.android.com/reference/kotlin/androidx/compose/material3/windowsizeclass/WindowSizeClass).

## Archivos

Creados:

- `app/src/main/java/com/example/applouaraya_grupogp/ui/adaptive/AdaptiveLayout.kt`: clasificación, formulario y acciones reutilizables.
- `app/src/main/java/com/example/applouaraya_grupogp/ui/adaptive/AdaptabilidadPreviews.kt`: 21 previews de las siete pantallas y una adicional horizontal.
- `app/src/test/java/com/example/applouaraya_grupogp/AdaptabilidadUnitTest.kt`: seis casos de clasificación y límites.
- `app/src/androidTest/java/com/example/applouaraya_grupogp/AdaptabilidadTest.kt`: ocho casos de distribución, formularios y teclado, exclusivamente con UI ficticia.
- Este documento.

Modificados:

- `gradle/libs.versions.toml` y `app/build.gradle.kts`: dependencia Material 3 alineada al BOM.
- `ui/screens/BienvenidaScreen.kt`, `LoginScreen.kt`, `RegistroScreen.kt`, `ResumenScreen.kt`: contenedor compartido.
- `ui/screens/DashboardScreen.kt`: acciones distribuidas por tamaño.
- `ui/screens/AdminScreens.kt`: presentación del dashboard administrativo.
- `ui/screens/CatalogoScreen.kt`: cuadrícula adaptable y presentación independiente para previews.

## Mostrar previews y capturar evidencia

1. En Android Studio, sincronizar Gradle y abrir `app/src/main/java/com/example/applouaraya_grupogp/ui/adaptive/AdaptabilidadPreviews.kt`.
2. Seleccionar **Split** o **Design** en el editor. Pulsar **Build & Refresh** en Compose Preview si aparecen previews pendientes.
3. Elegir el grupo **Guía 9**, o filtrar por la función: `BienvenidaAdaptablePreview`, `LoginAdaptablePreview`, `RegistroAdaptablePreview`, `ResumenAdaptablePreview`, `ClienteAdaptablePreview`, `AdministradorAdaptablePreview`, `CatalogoAdaptablePreview`.
4. Cada función genera **Compacto 360**, **Mediano 720** y **Expandido 1100**. Usar zoom **Fit** para ver el tamaño completo; enfocar una preview si se necesita más espacio.
5. Capturar las tres versiones del dashboard y del catálogo, donde se distinguen 1/2/3 columnas; capturar también Login o Registro para evidenciar el ancho limitado en ventanas amplias.
6. Capturar `LoginHorizontalPreview` para evidenciar altura reducida. En modo interactivo desplazar el formulario para llegar a los botones.
7. Guardar las capturas con nombres como `guia9_dashboard_360.png`, `guia9_dashboard_720.png` y `guia9_dashboard_1100.png`. Incluir el nombre y tamaño de la preview en la evidencia.

Las previews representan tamaños lógicos, no pruebas físicas de tablet. Si el equipo no puede renderizar todas simultáneamente, filtrar una función y refrescarla.

## Comprobación manual en Samsung

1. Ejecutar la configuración `app` sobre el Samsung conectado, actualizando la instalación existente. No desinstalar ni limpiar almacenamiento.
2. Revisar Bienvenida y Login en vertical: botones de ancho normal y colores crema, café y terracota.
3. Abrir el teclado en los campos de Login y Registro. Desplazar hasta los botones; comprobar que permanecen accesibles sin cerrar obligatoriamente el teclado.
4. Girar el teléfono a horizontal con rotación automática habilitada. Comprobar Login, Registro y Resumen mediante desplazamiento. No confirmar cuentas nuevas salvo que se quiera crear una cuenta ficticia de demostración.
5. Con una cuenta cliente, comprobar dashboard, catálogo, búsqueda y filtros. En vertical debe verse una columna; en horizontal la clase depende del ancho real disponible, no de la orientación por sí sola.
6. Con una cuenta administradora, comprobar que se conservan los accesos y que las acciones se distribuyen al ampliar la ventana.
7. Regresar a vertical y comprobar Atrás y cierre de sesión. Para la evidencia de tamaños amplios usar las previews, sin modificar densidad ni resolución del Samsung.

## Reproducir verificaciones

Desde la raíz del proyecto, en PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --offline --no-configuration-cache --max-workers=1
```

Las pruebas nuevas no abren Room ni preferencias. Las pruebas de integración seleccionadas inyectan Room en memoria y preferencias con nombres de prueba UUID; la prueba comercial inyecta también favoritos en memoria. Se ejecutan con `am instrument` tras instalar los APK con `adb install -r`, sin desinstalar la aplicación ni limpiar sus datos. Los tamaños amplios de `AdaptabilidadTest` se ajustan mediante la densidad de su composición de prueba: no cambian ajustes del teléfono.

Los informes locales se encuentran en `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/reports/lint-results-debug.html` y `app/build/reports/instrumentadas-adaptabilidad-cierre.txt`.

## Resultado de esta ejecución

- `assembleDebug`: aprobado.
- `testDebugUnitTest`: **62 aprobadas, 0 fallidas, 0 omitidas** (56 existentes y 6 nuevas).
- `lintDebug`: **0 errores, 40 advertencias**. La dependencia nueva usa el catálogo de versiones; no añade advertencias al total anterior.
- `assembleDebugAndroidTest`: aprobado.
- Ejecución física de cierre del 18 de octubre de 2026: **18 ejecutadas, 18 aprobadas, 0 fallidas**, en **117,667 segundos**, sobre Samsung SM-A166M. Resultado del runner: `OK (18 tests)`.
- Desglose: adaptabilidad 8/8, flujo Registro → Resumen 3/3, confirmación única 1/1, navegación/perfil 2/2, administración 2/2 y catálogo/campañas 2/2.
- Los fallos de la primera ejecución (codificación del dashboard y teclado físico sobre lienzo escalado) quedaron resueltos y las comprobaciones aprobaron en la ejecución final. No se eliminaron aserciones ni se necesitaron nuevas modificaciones funcionales durante el cierre.
- Ambos APK se instalaron con `adb install -r`: `Success`. La aplicación conserva `firstInstallTime=2026-10-14 21:52:49`; cambió únicamente su fecha de actualización a `2026-10-18 16:16:34`. No hubo desinstalación ni limpieza de almacenamiento.
- **Guía 9: verificación técnica completada.** Las capturas académicas se pueden preparar con las instrucciones de previews anteriores.

El teclado real y la altura horizontal simulada aprobaron en casos separados. Los tamaños mediano y expandido son simulaciones de composición sobre el Samsung y previews; no constituyen una prueba física en tablet. La combinación horizontal más teclado puede demostrarse manualmente con los pasos anteriores. No se utilizó emulador, ni se desinstaló la aplicación o limpiaron sus datos.

Para repetir las 18 pruebas pertinentes tras reconectar el Samsung, ejecutar desde la raíz:

```powershell
$adbLou = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adbLou devices -l
$serialLou = 'REEMPLAZAR_POR_SERIAL_ADB'
& $adbLou -s $serialLou install -r app/build/outputs/apk/debug/app-debug.apk
& $adbLou -s $serialLou install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
$clasesLou = 'com.example.applouaraya_grupogp.AdaptabilidadTest,com.example.applouaraya_grupogp.RegistroFlujoTest,com.example.applouaraya_grupogp.RegistroConfirmacionTest,com.example.applouaraya_grupogp.NavegacionPerfilTest,com.example.applouaraya_grupogp.AdministracionTest,com.example.applouaraya_grupogp.ComercialUiTest'
& $adbLou -s $serialLou shell am instrument -w -r -e class $clasesLou com.example.applouaraya_grupogp.test/androidx.test.runner.AndroidJUnitRunner |
    Tee-Object -FilePath app/build/reports/instrumentadas-adaptabilidad-cierre.txt
```

Dejar el teléfono desbloqueado y sin interactuar durante las pruebas. Si falla una instalación, resolver ese error antes de iniciar la instrumentación. `install -r` actualiza conservando datos; no sustituirlo por una desinstalación.
