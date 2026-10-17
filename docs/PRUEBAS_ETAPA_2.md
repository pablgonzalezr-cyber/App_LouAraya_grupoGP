# Etapa 2: administración local

## Funcionalidades

- Rol CLIENTE/ADMINISTRADOR persistido en usuarios. El registro público siempre
  crea CLIENTE y no recibe un rol como parámetro.
- Login dirige al dashboard correspondiente al rol.
- Dashboard administrativo con total y conteos por estado; listado global local
  filtrable; detalle; confirmación antes de cambiar un estado.
- Flujo permitido: PENDIENTE → EN_REVISION → APROBADA o RECHAZADA.
  APROBADA y RECHAZADA son finales. Cancelar el diálogo no escribe nada.
- El repositorio verifica el usuario de la sesión y su rol en Room. Las consultas
  administrativas y la actualización SQL vuelven a exigir ADMINISTRADOR.
- La actualización compara el estado anterior para rechazar solicitudes obsoletas.
  El cliente consulta el estado actualizado desde su historial existente.

## Cuenta ficticia reservada

Correo: `admin@louaraya.example`

Contraseña: `LouDemo2026!`

Se provisiona desde UsuarioRepository antes de una operación de login/registro,
usando el mismo hash PBKDF2 que las cuentas normales. La provisión está protegida
por Mutex y solo inserta si no existe ese correo. No hay selector público de rol
ni operación pública para promover usuarios. No se sobrescriben cuentas existentes.
Si ese correo ya pertenecía a un cliente, se conserva y se informa el conflicto
al intentar entrar con la cuenta de demostración; no se le conceden privilegios.
Las credenciales conocidas corresponden exclusivamente a la demostración local.

## Migración sin pérdida de datos

AppDatabase versión 3 conserva MIGRATION_1_2 y registra MIGRATION_2_3:

```sql
ALTER TABLE usuarios ADD COLUMN rol TEXT NOT NULL DEFAULT 'CLIENTE';
```

Las cuentas anteriores quedan como clientes. Se conserva cotizaciones sin cambios
de esquema, y los archivos históricos 1.json y 2.json. Se agrega 3.json.
No se toca FavoritosDatabase ni se utiliza migración destructiva.

## Archivos

Rutas Kotlin relativas a app/src/main/java/com/example/applouaraya_grupogp:

Modificados:

- data/AppDatabase.kt, data/UsuarioDao.kt, data/CotizacionDao.kt
- model/UsuarioEntity.kt
- repository/UsuarioRepository.kt
- viewmodel/SesionViewModel.kt
- navigation/AppNavigation.kt

Creados:

- model/RolUsuario.kt
- repository/PoliticaAdministracion.kt
- repository/AdminCotizacionesRepository.kt
- viewmodel/AdminCotizacionesViewModel.kt
- ui/screens/AdminScreens.kt

Otros cambios: app/build.gradle.kts alinea las dependencias transitivas
kotlinx.serialization core/json con BOM 1.8.1 para compatibilidad de room-testing;
esquema 3.json; PoliticaAdministracionTest en src/test; AdministracionTest nuevo y
AppDatabaseMigrationTest ampliado en src/androidTest; este documento.

## Pruebas

Resultados de esta implementación:

- assembleDebug y assembleDebugAndroidTest: aprobados.
- testDebugUnitTest: 21 pruebas aprobadas, 0 fallos y 0 errores.
- lintDebug: 0 errores, 19 advertencias de versiones, convenciones y recursos.
- Recorrido instrumentado final por ADB en R5CY42JVXMT: `OK (7 tests)`,
  duración 5,604 segundos, 0 fallos. Reporte completo:
  app/build/reports/instrumentadas-etapa2.txt.
- El AbstractMethodError anterior del lector de esquemas no se reproduce tras
  alinear serialization core/json mediante BOM 1.8.1.
- Se verificó además que la definición exportada de cotizaciones es idéntica
  entre versiones 2 y 3. Las tres pruebas de migración en Android están aprobadas.

Resultado individual del recorrido final:

| Clase | Caso | Resultado |
|---|---|---|
| AppDatabaseMigrationTest | migrarConservaCuentaPermiteLoginYReabrirSolicitud (1→2, apertura en 3 y persistencia) | Aprobado |
| AppDatabaseMigrationTest | migracionDosATresConservaUsuarioYSolicitud | Aprobado |
| AppDatabaseMigrationTest | migracionDirectaUnoATres | Aprobado |
| AdministracionTest | permisosProvisionEstadosYVisibilidad | Aprobado |
| AdministracionTest | correoReservadoExistenteNoSePromueveNiSobrescribe | Aprobado |
| CotizacionDaoTest | aislamientoDetalleOrdenYEnvioIdempotente | Aprobado |
| ExampleInstrumentedTest | useAppContext | Aprobado |

La actualización final de ambos APK se realizó con install -r y terminó en
Success. No se desinstaló la aplicación ni se limpió su almacenamiento. Las pruebas
no abren la base principal ni favoritos: usan Room en memoria o archivos
prueba-migracion/prueba-rol/prueba-cadena con UUID; solo esos temporales se eliminan.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --offline --no-configuration-cache --max-workers=1
```

PoliticaAdministracionTest prueba roles desconocidos, permisos denegados y las
16 combinaciones posibles de transición. AdministracionTest usa Room en memoria
para probar provisión, correo reservado, permisos de lectura/escritura, cambio
obsoleto, estados finales y visibilidad desde el cliente.

AppDatabaseMigrationTest usa archivos temporales con UUID y esquemas históricos
para probar 2→3, 1→3 y conservación de credenciales y solicitudes al reabrir.
Solo elimina sus archivos temporales; nunca abre lou_araya.db para las pruebas.

Para instrumentación se usó instalación de actualización (`adb install -r`) y
`adb shell am instrument -w com.example.applouaraya_grupogp.test/androidx.test.runner.AndroidJUnitRunner`.
No se usa desinstalación, pm clear ni limpieza del almacenamiento de la app.

## Prueba manual en Redmi

1. Actualizar mediante Run de Android Studio o instalar app-debug.apk con `adb install -r`.
   Si hay conflicto de firma, detenerse; no desinstalar ni borrar almacenamiento.
2. Abrir la app e iniciar sesión con una cuenta cliente existente. Comprobar
   solicitudes anteriores, catálogo y favoritos. Crear dos solicitudes ficticias.
3. Cerrar sesión e ingresar con la cuenta administradora indicada arriba.
   Debe aparecer Administración Lou Araya y el total local de solicitudes.
4. Abrir Gestionar solicitudes y probar Todas, PENDIENTE, EN_REVISION,
   APROBADA y RECHAZADA. Abrir una solicitud pendiente.
5. Pulsar Cambiar a EN_REVISION y Cancelar: debe seguir pendiente. Repetir y
   Confirmar. Debe actualizarse el estado, el listado y los conteos del dashboard.
6. Desde EN_REVISION confirmar APROBADA. Esa solicitud ya no debe ofrecer cambios.
   Con la segunda solicitud, probar EN_REVISION y luego RECHAZADA.
7. Cerrar sesión y entrar como el cliente propietario: su historial debe reflejar
   los nuevos estados y seguir mostrando únicamente sus solicitudes.
8. Cerrar y volver a abrir la app, iniciar sesión y comprobar persistencia.

Solo se administran los datos de este dispositivo; no hay backend ni sincronización.
minSdk sigue siendo 26. Esta etapa no añade perfil ni otras funcionalidades.
