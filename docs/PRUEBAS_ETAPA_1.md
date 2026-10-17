# Cotizaciones: verificación y prueba en Redmi

## Archivos de la etapa

Modificados: app/build.gradle.kts, data/AppDatabase.kt,
navigation/AppNavigation.kt y ui/screens/DashboardScreen.kt.

Creados bajo app/src/main/java/com/example/applouaraya_grupogp:

- data/CotizacionDao.kt
- model/CotizacionEntity.kt y model/CotizacionUiState.kt
- repository/CotizacionRepository.kt
- validation/CotizacionValidator.kt
- viewmodel/CotizacionViewModel.kt y viewmodel/HistorialCotizacionesViewModel.kt
- ui/screens/CotizacionScreen.kt
- ui/screens/HistorialCotizacionesScreen.kt
- ui/screens/DetalleCotizacionScreen.kt

También se agregaron los esquemas 1.json y 2.json, CotizacionValidatorTest.kt
en src/test y AppDatabaseMigrationTest.kt y CotizacionDaoTest.kt en src/androidTest.
No se modificaron login, registro, catálogo ni favoritos.

## Alcance

Formulario Material 3 con descripción, categoría, material, ancho y largo/alto en cm,
cantidad, fecha requerida opcional y producto de referencia opcional. Guarda en Room
para el usuario autenticado; historial y detalle consultan siempre por propietario.
Estado inicial: PENDIENTE. Persistencia exclusivamente local, sin envío remoto.

El formulario bloquea el envío mientras guarda. Un token conservado en SavedStateHandle
y un índice único en Room hacen idempotentes los reintentos del mismo formulario.
Una solicitud nueva puede tener el mismo contenido si se abre un formulario nuevo.

## Migración

Se exportó el esquema 1 antes de incorporar la tabla nueva. Su hash histórico es
1139322b452b8bb04ea8f5eda1adbe03. Los esquemas están en app/schemas.
MIGRATION_1_2 solo crea cotizaciones y sus índices; no modifica usuarios.
El nombre de base sigue siendo lou_araya.db. FavoritosDatabase no cambia.
No hay fallbackToDestructiveMigration.

AppDatabaseMigrationTest crea una base temporal exclusiva de prueba desde el esquema
1, inserta una cuenta ficticia con hash PBKDF2, migra y valida contra el esquema 2,
comprueba login, guarda una solicitud y vuelve a abrir la base. La limpieza de esa
base temporal no afecta lou_araya.db ni favoritos.

## Comandos desde PowerShell en la raíz

Resultado de la reanudación del 17 de octubre de 2026:

- assembleDebug: aprobado.
- testDebugUnitTest: repetido y aprobado, 17 pruebas, 0 fallos y 0 errores
  (9 validaciones, 4 filtros de catálogo, 3 QR y 1 prueba de plantilla).
- lintDebug: aprobado, 0 errores y 18 advertencias de versiones, recursos,
  lectura de recursos en ImagenProducto y convenciones de dependencias/manifiesto.
- assembleDebugAndroidTest: aprobado; incluye las dos pruebas nuevas de Room.
- Los esquemas exportados de usuarios en versiones 1 y 2 son idénticos.
- connectedDebugAndroidTest: no ejecutó casos. Primer intento offline: faltaba
  com.android.tools.utp:gradle-work-action:32.4.1 en caché. Se comprobó Google Maven
  con HTTP 200 y el reintento online resolvió las dependencias, pero terminó con
  "No online devices found": emulator-5554 permaneció OFFLINE.

Pendiente: ejecutar AppDatabaseMigrationTest y CotizacionDaoTest en Android,
validar el recorrido visual en Redmi y comprobar Android API 26 físicamente.
No se considera aprobada la migración en dispositivo hasta ejecutar su prueba.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --offline --no-configuration-cache
.\gradlew.bat :app:connectedDebugAndroidTest --no-configuration-cache
```

Quitar --offline si faltan dependencias y la conexión a los repositorios funciona.
Las pruebas instrumentadas requieren un dispositivo o emulador autorizado.
Si Gradle necesita componentes UTP adicionales, también pueden ejecutarse los APK
de pruebas directamente usando adb install -r y am instrument.

## Actualizar y probar en Redmi sin borrar datos

1. Conservar la aplicación instalada y sus cuentas. Conectar el Redmi por USB,
   habilitar depuración USB y autorizar el computador. Si Xiaomi lo solicita,
   habilitar instalación por USB en opciones de desarrollador.
2. Seleccionar el Redmi en Android Studio y ejecutar app con Run. Debe actualizar
   con el mismo applicationId y firma. Si aparece un error de firma o versión,
   detenerse: no desinstalar ni borrar almacenamiento.
3. Alternativa desde PowerShell:

   ```powershell
   & "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" devices
   & "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" -s SERIAL_DEL_REDMI install -r app/build/outputs/apk/debug/app-debug.apk
   ```

4. Iniciar sesión con una cuenta anterior. Comprobar que catálogo y favoritos
   siguen mostrando lo esperado.
5. Abrir Cotización personalizada desde el dashboard. Enviar vacío y comprobar
   mensajes de error. Probar medidas 0, negativas o texto, cantidad 1.5 y una fecha pasada.
6. Completar: descripción "Tapiz personalizado para living", categoría Interiores,
   material Algodón, ancho 45,5; largo 70; cantidad 2. Dejar fecha y referencia vacías.
   Guardar y comprobar detalle con estado PENDIENTE.
7. Volver a Mis solicitudes: debe haber una sola solicitud, incluso al pulsar
   rápidamente Guardar. Crear otra con producto de referencia y fecha futura válida.
8. Cerrar y abrir la aplicación, iniciar sesión si se solicita y comprobar persistencia.
9. Cerrar sesión e ingresar con una segunda cuenta ficticia. No debe ver las
   solicitudes de la primera. Regresar a la primera cuenta y comprobar su historial.
10. Rotar el teléfono durante la edición y revisar que los campos se conservan.

## Límites de esta etapa

La sesión existente no se restaura tras muerte del proceso: volver a iniciar sesión.
No se implementa administración ni cambio de estado. No hay sincronización entre
teléfonos. minSdk sigue siendo 26; la prueba física en Android 8 queda pendiente
si no hay un dispositivo o emulador API 26 disponible.
