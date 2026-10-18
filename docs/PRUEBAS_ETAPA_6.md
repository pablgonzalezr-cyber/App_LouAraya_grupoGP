# Etapa 6 — Notificaciones y centro de avisos

Implementación del 17 de octubre de 2026 en `feature/cotizaciones`. Avisos **locales**, sin Firebase, backend REST, credenciales externas ni sincronización entre teléfonos. Se conserva minSdk 26 y no se añaden dependencias.

## Comportamiento implementado

El Dashboard de CLIENTE y el de ADMINISTRADOR tienen «Centro de avisos (N sin leer)». El centro muestra fecha, título, mensaje, estado de lectura, «Marcar como leída» y «Ver contenido». El contador incluye todos los avisos no leídos de esa cuenta, independientemente de las preferencias de alertas de Android. Abrir un contenido válido también marca el aviso como leído.

| Evento local nuevo | Destinatarios | Destino autorizado |
| --- | --- | --- |
| Crear una cotización | Administradores existentes | Detalle administrativo de esa solicitud |
| Cambiar válidamente el estado de una cotización | Propietario de la solicitud | Su detalle de solicitud |
| Guardar una campaña activa y publicada, no vencida | Clientes existentes | Área de colecciones y campañas, si la campaña está vigente |

La solicitud y sus avisos se guardan en una transacción Room; lo mismo ocurre con cambio de estado y aviso, y con campaña y avisos. La clave única por destinatario/evento evita duplicar avisos al reenviar el mismo token, editar una publicación o repetir una escritura. Una campaña genera un solo aviso por cliente y campaña; republicarla no produce otro aviso para clientes que ya recibieron ese evento.

La migración no inventa eventos históricos. Los cambios anteriores a esta etapa permanecen intactos y no generan avisos retroactivos. Los eventos nuevos se notifican a las cuentas que existen cuando ocurren. Una campaña futura publicada genera el aviso de publicación, pero «Ver contenido» explica que aún no está vigente hasta su fecha de inicio. No se programa otra alerta al comenzar esa fecha. Las campañas desactivadas, borradores o vencidas no pueden abrirse desde un aviso; el historial se conserva.

## Protección de la información

- `AvisosRepository` obtiene el usuario desde la sesión, sin aceptar el destinatario desde la UI. Consulta y marca por `usuarioId` e ID de aviso.
- Al abrir contenido, vuelve a consultar en Room al usuario, su rol, el propietario de la solicitud o la publicación/vigencia de la campaña. La navegación también conserva el guardián general de sesión y roles.
- Los Flows y el contador se limpian al cambiar/cerrar sesión. La UI descarta inmediatamente cualquier estado cuyo usuario no coincida con la sesión actual.
- El sistema Android **nunca recibe** nombres, descripciones, números de solicitud, estados, destinatarios ni títulos de campañas. Solo muestra «Lou Araya Macramé» y «Tienes avisos sin leer. Abre tu centro de avisos.»
- Se usa `VISIBILITY_SECRET` tanto en notificación como en canal. Aunque Android permita al usuario cambiar la presentación del canal, el texto publicado sigue siendo genérico.
- Solo se publican resúmenes de avisos no leídos del usuario activo. Un cambio administrativo guarda el aviso del cliente en Room, sin publicarlo durante la sesión administrativa.
- Al cerrar/cambiar sesión se retiran sincrónicamente los resúmenes de esta función. También se limpian al iniciar la actividad sin una sesión restaurada y al desechar el host de navegación. Si Android mata el proceso abruptamente, podría quedar el resumen genérico; no contiene información privada y al abrirlo se exige autenticación.
- El PendingIntent es explícito e inmutable y no lleva identificadores ni una ruta de contenido. Abre la actividad; si no hay sesión, primero se inicia sesión y después se abre únicamente el centro de esa cuenta. No autentica automáticamente a ningún destinatario.

## Permiso y preferencias

Hay dos canales Android: «Cotizaciones y solicitudes» y «Campañas», creados con NotificationManager para Android 8.0 o superior. Como máximo se mantiene un resumen por canal.

En Android 13 o superior, POST_NOTIFICATIONS se pide **solo al pulsar «Permitir avisos» en el centro**. No se solicita al iniciar sesión ni desde Dashboard/Perfil. Denegarlo no bloquea el centro Room. Se incluye acceso a los ajustes Android y se revisa el permiso al volver a la aplicación; también se respeta el bloqueo global o de cada canal.

En Perfil hay dos preferencias por usuario: «Avisos de cotizaciones» y «Avisos de campañas». Controlan las alertas Android; no eliminan ni ocultan el historial persistente. Se guardan en las preferencias locales existentes con claves separadas por cuenta. Silenciar una categoría retira su resumen; reactivarla puede mostrar avisos todavía no leídos. Android mantiene el permiso y los canales por aplicación; las preferencias complementan ese control para cada cuenta local.

## Room y preservación de datos

- AppDatabase pasa de 5 a 6 con `MIGRATION_5_6` explícita.
- Se crea solamente `avisos`, su clave foránea a usuarios, índice de destinatario e índice único `(usuarioId, claveEvento)`.
- Se conserva el esquema 5 histórico y se genera `6.json` mediante KSP/Room.
- La comparación de los esquemas exportados confirma que usuarios, cotizaciones, productos, colecciones, asociaciones y campañas no cambiaron.
- Se mantienen las migraciones 1 → 2 → 3 → 4 → 5 y se añade 5 → 6. Sin migraciones destructivas.
- No se modifica la base independiente de favoritos ni los IDs de productos existentes.

## Archivos creados

Prefijo de código: `app/src/main/java/com/example/applouaraya_grupogp/`.

- `model/AvisoEntity.kt`
- `data/AvisoDao.kt`
- `repository/AvisosRepository.kt` (incluye política de tipos, preferencias y destinos)
- `viewmodel/AvisosViewModel.kt`
- `notifications/NotificacionesLocales.kt`
- `ui/screens/AvisosScreen.kt`
- `app/src/main/res/drawable/ic_aviso.xml`
- `app/schemas/com.example.applouaraya_grupogp.data.AppDatabase/6.json`
- `app/src/test/java/com/example/applouaraya_grupogp/PoliticaAvisosTest.kt`
- `app/src/androidTest/java/com/example/applouaraya_grupogp/AvisosPersistenciaTest.kt`
- `app/src/androidTest/java/com/example/applouaraya_grupogp/AvisosSesionTest.kt`
- `app/src/androidTest/java/com/example/applouaraya_grupogp/AvisosUiTest.kt`
- `app/src/androidTest/java/com/example/applouaraya_grupogp/NotificacionesSistemaTest.kt`
- `docs/PRUEBAS_ETAPA_6.md`

## Archivos modificados

Con el mismo prefijo de código:

- `MainActivity.kt`: recepción de la acción genérica del sistema y limpieza de resúmenes al arrancar.
- `data/AppDatabase.kt`: entidad, DAO y migración.
- `data/CotizacionDao.kt`: eventos transaccionales de creación y cambio de estado.
- `data/ContenidoDao.kt`: consulta por ID para validar el destino de campañas.
- `repository/ContenidoRepository.kt`: publicación de avisos dentro de la transacción existente.
- `repository/PreferenciasRepository.kt`: preferencias individuales de categorías.
- `navigation/AppNavigation.kt`, `navigation/PoliticaNavegacion.kt`: centro compartido protegido, contador, cancelación al salir y apertura autorizada.
- `ui/screens/DashboardScreen.kt`, `ui/screens/AdminScreens.kt`: acceso y contador.
- `ui/screens/PerfilScreen.kt`: preferencias de avisos.
- `viewmodel/HistorialCotizacionesViewModel.kt`, `ui/screens/DetalleCotizacionScreen.kt`: permiten inyectar el repositorio del detalle existente. AppNavigation utiliza la misma base para el aviso y su detalle; la prueba de navegación puede así abrir el detalle sin tocar la base personal.
- `app/src/main/AndroidManifest.xml`: POST_NOTIFICATIONS.
- `app/src/androidTest/java/com/example/applouaraya_grupogp/NavegacionPerfilTest.kt`: comprueba también el rechazo de `avisos` sin sesión.

## Pruebas y aislamiento

Las pruebas instrumentadas se inspeccionaron antes de ejecutarse. Usan Room en memoria o nombres exclusivos `prueba-...` con UUID; solo eliminan sus archivos de prueba. Las preferencias también usan nombres exclusivos. No se borra ni se desinstala la aplicación principal y no se usan emuladores.

`NotificacionesSistemaTest` publica mediante NotificationManager real, con etiquetas y canales `prueba-avisos-<UUID>`. Solo retira sus resúmenes y elimina sus propios canales al terminar. Verifica visibilidad SECRET, texto genérico, PendingIntent inmutable, exclusión de avisos ajenos, cambio de cuenta, preferencias, lectura y cierre de sesión. No abre MainActivity ni sus datos. La regla de prueba puede dejar POST_NOTIFICATIONS concedido; para comprobar denegación manual cambia solo ese permiso en Ajustes Android.

Las pantallas con AppDatabase de prueba inyectada no publican ni cancelan los resúmenes de producción. La prueba de interfaz verifica el centro, contador, lectura y apertura del detalle existente contra Room en memoria.

Los ocho casos nuevos instrumentados son:

1. Eventos aislados, sin duplicados, lectura ajena rechazada y destinos según propietario/rol.
2. Campañas publicadas sin duplicados y vigencia/publicación revalidada al abrir.
3. Avisos leídos y preferencias conservados al cerrar y reabrir la base/repositorio.
4. Migración 5 → 6 preservando las seis tablas previas y credenciales/ediciones de producto.
5. Cadena 1 → 6 conservando usuario y asignando el rol histórico correcto.
6. Cambio de cuenta y cierre: estado y contador limpios.
7. Centro cliente: solo sus avisos, lectura, contador y detalle de su solicitud.
8. Resúmenes del sistema reales y privacidad.

Las cuatro unitarias nuevas cubren roles/destinos, referencias/tipos inválidos, preferencias por categoría y acceso al centro con sesión válida.

Durante la ampliación de la prueba de navegación se corrigieron dos defectos de la propia prueba: el cierre manual anticipado de ActivityScenario interfería con la limpieza de Compose, y la aserción de la descripción omitía el prefijo «Descripción:» usado por la pantalla existente. Se ordenaron los recursos con RuleChain y se ajustó el texto esperado; no se relajó la comprobación del detalle ni se sustituyó por una pantalla ficticia. El caso ampliado aprobó aislado en 9.862 segundos antes de repetir la suite completa.

También se ajustó la sincronización de `NotificacionesSistemaTest`: las llamadas notify/cancel al servicio Android no garantizan que su lista activa se actualice inmediatamente. La prueba conserva las aserciones de presencia y ausencia, pero espera el resultado hasta 5 segundos en lugar de consultar una sola vez. El caso con NotificationManager real aprobó aislado después del ajuste (0.238 segundos).

## Resultados finales

| Verificación | Resultado |
| --- | --- |
| `assembleDebug` | BUILD SUCCESSFUL |
| `assembleDebugAndroidTest` | BUILD SUCCESSFUL |
| Unitarias | 49/49 aprobadas; 0 fallos, 0 errores |
| Instrumentadas en teléfono físico | OK (33 tests); 103.01 segundos; 0 fallos, 0 omisiones |
| `lintDebug` | BUILD SUCCESSFUL; 0 errores, 40 advertencias |
| Migraciones 5 → 6 y cadena 1 → 6 | Aprobadas contra bases exclusivas de prueba |
| Comparación de esquema 5/6 | Las seis tablas anteriores conservan su definición |
| `git diff --check` | Sin errores de formato |

Las 33 instrumentadas incluyen los 25 casos previos y los 8 nuevos descritos arriba. Se comprobaron persistencia y separación de cuentas, lectura/contador, apertura del detalle de la solicitud, permisos administrativos, migraciones históricas, catálogo/favoritos, colecciones/campañas, perfil/navegación, cámara QR y mapa real.

No quedan fallos automáticos pendientes. Las 40 advertencias de lint corresponden a etiqueta redundante (1), versiones de dependencias (13), lectura de recursos desde LocalContext (1), recursos sin uso (7), recomendaciones KTX (5) y catálogo de versiones (13); no se cambiaron dependencias ni módulos ajenos para resolverlas. Quedan para prueba manual el diálogo de denegación, presentación visual de bloqueo y recorrido al tocar la alerta Android con el proceso cerrado, según la guía de abajo.

Evidencia generada, disponible localmente bajo carpetas `build` ignoradas por Git:

- `app/build/reports/instrumentadas-etapa6-final.txt`: resultado final completo.
- `app/build/reports/instrumentadas-etapa6-ui.txt`: caso de interfaz ampliado aislado.
- `app/build/reports/instrumentadas-etapa6-sistema.txt`: caso NotificationManager aislado.
- `app/build/reports/tests/testDebugUnitTest/index.html`: resultados unitarios.
- `app/build/reports/lint-results-debug.html`: advertencias completas.
- Se conservaron también informes de los intentos anteriores para revisar los ajustes de pruebas.

Se actualizó la app y el APK de pruebas con `adb install -r`. No se desinstaló la aplicación, no se borraron datos personales ni se hicieron commits, push o merges. Los cambios quedan sin commit en `feature/cotizaciones`; no se avanzó al backend ni a otras etapas.

## Repetir compilación y pruebas

PowerShell desde la raíz:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --offline --no-configuration-cache --max-workers=1
$adbLou = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
& $adbLou devices -l
& $adbLou -s R5CY42JVXMT install -r app/build/outputs/apk/debug/app-debug.apk
& $adbLou -s R5CY42JVXMT install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adbLou -s R5CY42JVXMT shell am instrument -w -r com.example.applouaraya_grupogp.test/androidx.test.runner.AndroidJUnitRunner
```

La instalación `-r` actualiza conservando datos. La invocación directa del runner evita tareas que desinstalen al finalizar. El teléfono observado es Samsung SM-A166M, serie `R5CY42JVXMT`; adaptar la serie si se usa otro. Las pruebas de mapa existentes necesitan Internet en el teléfono y las de cámara necesitan el teléfono disponible y desbloqueado. En otro computador sin caché de Gradle, omitir `--offline`.

## Guía detallada en el celular

### Preparación

1. Abre la app actualizada sin borrar datos. Al abrir Room se aplicará la migración; tus usuarios, favoritos, productos, solicitudes y campañas anteriores deben continuar disponibles.
2. Usa dos cuentas cliente de prueba si quieres comprobar separación, sin alterar cuentas personales. Ten disponibles las credenciales administrativas habituales. La cuenta demo existente es `admin@louaraya.example`, clave `LouDemo2026!`; no se modificó su creación controlada.
3. En cada cuenta, desplázate por Dashboard hasta «Centro de avisos». Es normal que el centro esté vacío antes de generar eventos nuevos.

### Solicitud nueva y cambio de estado

1. Como cliente de prueba crea una cotización ficticia nueva y anota su número.
2. Cambia al administrador. Su contador debe aumentar y el centro debe incluir «Nueva solicitud». «Ver contenido» abre su detalle administrativo.
3. Cambia PENDIENTE → EN_REVISION mediante la confirmación existente. El aviso se guarda para el cliente; no debe aparecer en el centro del administrador ni mostrar datos del cliente en Android.
4. Cambia al cliente propietario: aparecerá «Estado de cotización actualizado» con el estado y número de su solicitud. Si las alertas Android están permitidas y su preferencia activa, aparecerá únicamente el resumen genérico.
5. Pulsa «Ver contenido»: abre su detalle y el aviso queda leído. Vuelve al centro y comprueba el contador. En otro aviso sin leer prueba «Marcar como leída»; no debe desaparecer el historial.
6. Entra con el segundo cliente: no debe ver avisos ni contenidos privados del primero. Cierra sesión y pulsa Atrás; no debe recuperar el centro ni pantallas privadas.

### Campañas

1. Como administrador crea una campaña ficticia con fechas vigentes, activa y publicada. No añade descuentos automáticos.
2. Como cliente comprueba el aviso de campaña. «Ver contenido» abre el área actual de colecciones y campañas, que debe incluir esa publicación.
3. Edita la misma campaña: no debe duplicarse el aviso existente. Desactívala; al intentar abrir el aviso, el centro explica que no está vigente/publicada y conserva el historial.
4. Publica una campaña futura si quieres comprobar ese caso: se avisa su publicación y el contenido se habilita cuando esté vigente, sin programar una segunda alerta automática.

### Permiso, privacidad y preferencias

1. Abre el centro. En Android 13+ pulsa «Permitir avisos» si aparece. Si la prueba instrumentada ya concedió el permiso, el botón no será necesario.
2. Para comprobar la denegación, entra en «Ajustes Android» y desactiva las notificaciones de esta app. Vuelve: los avisos Room, lectura y contador deben seguir funcionando. No borres almacenamiento.
3. Activa de nuevo las alertas y revisa ambos canales. Con un aviso sin leer de tu cuenta, el panel Android debe mostrar únicamente el texto genérico, sin IDs, estados, nombres ni descripciones. En bloqueo se solicita ocultarlo completamente con SECRET; la presentación final depende de Android y de los ajustes del dispositivo.
4. Toca el resumen Android: con sesión activa abre su propio centro; si Android había cerrado el proceso, inicia sesión primero y comprueba que abre el centro de la cuenta que autenticaste.
5. Cierra/cambia sesión: los resúmenes anteriores se retiran. Durante la sesión administrativa no debe mostrarse un resumen originado por los avisos privados de otro cliente.
6. En Perfil desactiva «Avisos de cotizaciones» o «Avisos de campañas». Solo se silencian los resúmenes de esa categoría; el centro y el contador conservan el historial. Otro usuario mantiene sus propias preferencias.
7. Cierra la app normalmente y vuelve a entrar: se conservan los avisos, sus marcas de lectura y las preferencias individuales. Revisa también catálogo, favoritos, cotizaciones, perfil, administración, escáner y mapa habituales.

## Límites y comprobaciones manuales

No hay push remoto ni recepción de cambios hechos en otro dispositivo. Los eventos se producen por operaciones sobre la base de este teléfono. Sin un proceso de app activo no hay un servicio que genere nuevas alertas; al iniciar sesión se revisan los avisos locales no leídos. No se añaden servicios, alarmas ni trabajos programados.

La batería, canales y configuración de Android pueden silenciar las alertas del sistema; no afectan al historial Room. Las pruebas automatizadas comprueban el objeto publicado real y sus propiedades de privacidad, pero la presentación visual de la pantalla de bloqueo y el diálogo de permiso deben comprobarse manualmente en el teléfono.

## Referencias oficiales

- [Permiso de notificaciones en Android 13+](https://developer.android.com/develop/ui/views/notifications/notification-permission).
- [Canales de notificación desde Android 8.0](https://developer.android.com/develop/ui/views/notifications/channels).
- [Notification: visibilidad SECRET](https://developer.android.com/reference/android/app/Notification#VISIBILITY_SECRET).
- [PendingIntent e inmutabilidad](https://developer.android.com/reference/android/app/PendingIntent#FLAG_IMMUTABLE).
