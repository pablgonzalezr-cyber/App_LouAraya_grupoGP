# Etapa 3: perfil, configuración y navegación

## Implementación

- Perfil para CLIENTE y ADMINISTRADOR: nombre editable, correo y rol de solo lectura.
- Nombre de 2 a 80 caracteres después de normalizar espacios; rechaza caracteres
  de control. Guardado con UPDATE en Room para el usuario de la sesión, sin aceptar
  un identificador editable desde la pantalla. Los cambios llegan al saludo de inicio.
- Configuración por cuenta en SharedPreferences: confirmar cierre/cambio de sesión
  y mostrar ayuda en el formulario de cotizaciones. Ambas opciones son persistentes
  y activas por defecto. No se guardan contraseñas en preferencias.
- Consulta de estado de cámara, ubicación aproximada y precisa; sin pedir permisos,
  abrir cámara ni obtener ubicación. Actualiza al regresar desde los ajustes del sistema.
  El manifiesto actual no declara estos permisos: se informa que no tienen uso actual.
- Dashboard cliente con catálogo, favoritos, cotización, historial y perfil.
  El acceso a favoritos usa la implementación existente del catálogo y su misma base.
- Dashboard administrador conserva solicitudes, filtros y estados, y añade perfil.
- Cierre/cambio de sesión limpia formularios, trabajos de autenticación y toda la
  pila de navegación. El cambio de usuario vuelve a login. No elimina cuentas ni datos.
- Política central de rutas aplicada antes de dibujar pantallas privadas y al
  restaurar la navegación. Cliente no entra a administración; ambos roles acceden
  a su perfil. La sesión sigue siendo en memoria: tras muerte del proceso se debe
  iniciar sesión, y una ruta privada restaurada se redirige a login.

Room conserva versión 3: solo cambia una consulta DAO para actualizar el nombre.
No cambian entidades, esquemas, identificadores, migraciones ni FavoritosDatabase.
No se añaden dependencias. minSdk sigue siendo 26.

## Archivos creados

Rutas Kotlin relativas a app/src/main/java/com/example/applouaraya_grupogp:

- validation/PerfilValidator.kt
- repository/PerfilRepository.kt
- repository/PreferenciasRepository.kt
- viewmodel/PerfilViewModel.kt
- viewmodel/ConfiguracionViewModel.kt
- ui/screens/PerfilScreen.kt
- navigation/PoliticaNavegacion.kt

Pruebas nuevas:

- app/src/test/java/com/example/applouaraya_grupogp/PerfilValidatorTest.kt
- app/src/test/java/com/example/applouaraya_grupogp/PoliticaNavegacionTest.kt
- app/src/androidTest/java/com/example/applouaraya_grupogp/PerfilPersistenciaTest.kt
- app/src/androidTest/java/com/example/applouaraya_grupogp/NavegacionPerfilTest.kt

## Archivos modificados

- data/UsuarioDao.kt: actualización exclusiva del nombre por id.
- repository/UsuarioRepository.kt: expone observación del usuario desde Room.
- viewmodel/SesionViewModel.kt: sincroniza datos del perfil y cancela trabajos al salir;
  permite repositorio de prueba aislado sin cambiar el comportamiento de autenticación.
- navigation/AppNavigation.kt: perfil, favoritos y guardas por sesión/rol; salida común;
  inyección de base y preferencias aisladas para pruebas.
- ui/screens/DashboardScreen.kt: accesos a favoritos y perfil.
- ui/screens/AdminScreens.kt: acceso a perfil y eliminación del botón Volver que cerraba sesión en el inicio administrativo.
- ui/screens/CatalogoScreen.kt: solo parámetro opcional para abrir su vista de favoritos.
- ui/screens/CotizacionScreen.kt: parámetro opcional para mostrar/ocultar la ayuda.
- Este documento registra el resultado y el procedimiento de comprobación.

## Revisión del backend de la asignatura

Se revisaron los archivos y documentos presentes en el repositorio, incluyendo
Gradle, manifiesto, fuentes, configuración y docs/PRUEBAS_ETAPA_1.md y ETAPA_2.md.
No se encontró URL base del backend, Swagger/OpenAPI, definición de endpoints,
cliente HTTP ni configuración de servidor de la asignatura. Los enlaces existentes
corresponden a documentación técnica de Android/Gradle y descarga de herramientas.
No hay permiso de Internet en el manifiesto. Los módulos actuales son locales.

Para integrar REST falta obtener el contrato real: URL por entorno, operaciones
y modelos JSON, autenticación y autorización/roles, errores, credenciales ficticias
de prueba y reglas de sincronización entre ids locales y remotos. Luego habrá que
decidir caché, conflictos y conectividad a partir de ese contrato. No se implementó
REST ni se crearon endpoints ficticios en esta etapa.

## Comprobación automática

Resultado de esta implementación:

- assembleDebug: aprobado.
- testDebugUnitTest: 31 pruebas aprobadas, 0 fallos y 0 errores; incluye las 21 anteriores.
- lintDebug: 0 errores, 21 advertencias (versiones/convenios/recursos y sugerencias KTX).
- assembleDebugAndroidTest: aprobado; compila los 12 casos.
- Ejecución instrumentada en teléfono físico R5CY42JVXMT: 12 pruebas ejecutadas,
  12 aprobadas y 0 fallidas, duración 40,707 segundos: `OK (12 tests)`.
  Registro completo en app/build/reports/instrumentadas-etapa3-corregidas.txt.
  No se inició ningún emulador.
- No cambian AppDatabase, sus migraciones ni sus esquemas exportados.
- git diff --check: sin errores de espacios; la rama sigue siendo feature/cotizaciones.

La causa del fallo anterior fue una aserción no vinculada al requisito de seguridad:
exigía DESTROYED tras Atrás y Android mantuvo la actividad en RESUMED. Se corrigió
únicamente salirLimpiaPilaYBotonAtrasNoRecuperaPerfil y sus auxiliares/configuración
para verificar Bienvenida al salir, ausencia de contenido privado y pila previa
vacía. Con la sesión cerrada se intentan siete destinos (perfil, cotizacion,
solicitudes, solicitud/1, admin, admin/solicitudes, admin/solicitud/1): todos deben
redirigir a login sin montar pantallas privadas ni conservar destinos anteriores.
Después se comprueba Atrás desde la raíz pública: solo puede quedar Bienvenida
o ningún destino, y ninguna entrada anterior. No se exige un estado de actividad.

AppNavigation admite ahora un NavHostController opcional para que esta prueba
inspeccione e invoque el controlador real. Por defecto sigue creando el mismo
controlador con rememberNavController; no cambian la política ni las guardas.
No se detectó un fallo de acceso privado en los recorridos ejecutados y no fue
necesario modificar reglas de navegación, autenticación, esquemas ni bases de datos.

| Grupo/caso | Resultado |
|---|---|
| AdministracionTest: permisosProvisionEstadosYVisibilidad | Aprobado |
| AdministracionTest: correoReservadoExistenteNoSePromueveNiSobrescribe | Aprobado |
| AppDatabaseMigrationTest: migracionDosATresConservaUsuarioYSolicitud | Aprobado |
| AppDatabaseMigrationTest: migrarConservaCuentaPermiteLoginYReabrirSolicitud | Aprobado |
| AppDatabaseMigrationTest: migracionDirectaUnoATres | Aprobado |
| CotizacionDaoTest: aislamientoDetalleOrdenYEnvioIdempotente | Aprobado |
| ExampleInstrumentedTest: useAppContext | Aprobado |
| NavegacionPerfilTest: salirLimpiaPilaYBotonAtrasNoRecuperaPerfil | Aprobado |
| NavegacionPerfilTest: editarPerfilCambiarCuentaYVolverConRolesCorrectos | Aprobado |
| PerfilPersistenciaTest: nombreInvalidoYSesionAusenteNoModificanCuentas | Aprobado |
| PerfilPersistenciaTest: nombrePersisteTrasReabrirSinCambiarCorreoRolNiCredenciales | Aprobado |
| PerfilPersistenciaTest: preferenciasPersistenYSeSeparanPorUsuario | Aprobado |

Ambos APK se actualizaron con install -r (Success), conservando el almacenamiento.
Las pruebas solo accedieron a las bases/preferencias aisladas descritas abajo;
no se desinstaló la aplicación ni se borraron usuarios, favoritos o cotizaciones.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --offline --no-configuration-cache --max-workers=1
```

31 unitarias: las 21 anteriores más 4 de nombre y 6 de acceso por ruta y rol.
Instrumentadas: las 7 anteriores más 3 de perfil/preferencias y 2 de navegación UI.
Las nuevas pruebas solo usan Room en memoria o un archivo prueba-perfil-UUID.db y
archivos SharedPreferences prueba-preferencias/prueba-navegacion con UUID. La UI de
prueba recibe esas dependencias explícitamente; no abre la base real para perfil,
autenticación ni administración. La limpieza solo elimina sus archivos temporales.

Cuando exista un teléfono autorizado, actualizar los APK sin desinstalar y ejecutar
directamente por ADB para evitar procedimientos automáticos de limpieza:

```powershell
& "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" devices
& "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
& "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" -s SERIAL install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" -s SERIAL shell am instrument -w -r com.example.applouaraya_grupogp.test/androidx.test.runner.AndroidJUnitRunner
```

## Cómo probar en el teléfono sin borrar datos

1. Conectar el teléfono, autorizar depuración USB y seleccionar el dispositivo en
   Android Studio. Actualizar con Run o con adb install -r del APK debug.
   Ante un conflicto de firma, detenerse: no desinstalar ni borrar almacenamiento.
2. Iniciar sesión con una cuenta cliente anterior. Verificar catálogo, favoritos,
   cotizaciones e historial; desde inicio debe haber accesos directos a todos.
3. Abrir Perfil y configuración. Comprobar correo y rol. Probar nombre vacío,
   de un carácter y de más de 80: deben mostrar error. Guardar un nombre válido.
4. Volver al inicio y comprobar el saludo. Cerrar y volver a abrir la app, iniciar
   sesión y comprobar persistencia del nombre y las solicitudes anteriores.
5. Desactivar Mostrar ayuda en cotizaciones y verificar el formulario. Volver al
   perfil y reactivar; cerrar y abrir debe conservar la última selección.
6. Probar Confirmar cierre/cambio de usuario: activada muestra Confirmar/Cancelar;
   desactivada sale directamente. Cancelar debe mantener la sesión.
7. Revisar estados de permisos y abrir ajustes del sistema. No debe aparecer ningún
   diálogo de solicitud de cámara o ubicación por entrar al perfil.
8. Cambiar de usuario. Con otra cuenta comprobar nombre y preferencias propios.
   Tras cerrar sesión, pulsar Atrás no debe recuperar pantallas privadas.
9. Entrar como admin@louaraya.example / LouDemo2026!, abrir perfil y editar nombre.
   Volver a solicitudes y probar filtros y cambios de estado como en la Etapa 2.

La persistencia y configuración siguen siendo locales. No se implementan otras etapas.
