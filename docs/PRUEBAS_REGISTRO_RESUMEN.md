# Guía 11 — Registro, validación reactiva y resumen

Corrección implementada el 18 de octubre de 2026 en `feature/cotizaciones`. Se conserva Kotlin, Compose Material 3, MVVM, StateFlow y minSdk 26. No se modifica Room, sus esquemas ni las migraciones; tampoco se añaden dependencias.

## Flujo implementado

1. Bienvenida o Login abre el formulario existente.
2. Nombre, correo, clave y aceptación de términos se mantienen en `RegistroViewModel.estado`, expuesto como StateFlow y observado mediante `collectAsState`.
3. Cada cambio de campo revalida inmediatamente ese campo. Los errores de los demás campos se conservan; no se oculta un error simplemente por introducir otro valor todavía inválido. Al pulsar «Continuar» se validan todos los campos, incluidos los no tocados.
4. Con datos válidos se navega a la ruta literal `resumen`, sin argumentos. Continuar no llama al repositorio ni guarda al usuario.
5. Resumen recibe exactamente la misma instancia de RegistroViewModel creada por encima del NavHost. Muestra nombre, correo, términos aceptados y tipo de cuenta Cliente. No muestra, enmascara ni lee la clave para representarla; no hay contraseña en los argumentos de navegación.
6. «Volver a editar» y Atrás regresan al formulario conservando el borrador. Editar invalida la autorización anterior del resumen y requiere continuar otra vez.
7. «Confirmar registro» revalida y reutiliza `SesionViewModel.registrar` → `UsuarioRepository.registrar`. Solo entonces se guarda el usuario con el mecanismo existente.
8. La confirmación y el regreso quedan bloqueados durante la escritura. SesionViewModel bloquea inmediatamente llamadas repetidas antes de iniciar la corrutina y también cuando ya existe una sesión autenticada.
9. Si el correo existe, el error aparece en Resumen. Volver a editar limpia ese error general y permite corregir el correo sin alterar la cuenta previa.
10. Al registrar correctamente, la navegación existente reemplaza toda la pila por el inicio del rol y limpia el formulario, incluida la credencial en memoria. Atrás no conserva un resumen pendiente de confirmar.

Se mantienen las reglas del formulario: nombre obligatorio, correo con formato válido, contraseña de al menos seis caracteres y términos aceptados. Se usa `PatternsCompat.EMAIL_ADDRESS` de la dependencia Core ya existente para poder verificar la validación también mediante pruebas JVM.

El borrador y la clave no se guardan en SavedStateHandle, rutas ni almacenamiento adicional. Si Android pierde el proceso y restaura un destino Resumen sin borrador válido, se redirige al formulario; no se ofrece confirmar datos vacíos. La persistencia del borrador tras muerte del proceso queda deliberadamente fuera de esta corrección.

## Archivos modificados

Prefijo de producción: `app/src/main/java/com/example/applouaraya_grupogp/`.

- `model/RegistroUiState.kt`: elegibilidad del resumen, sin duplicar datos del formulario.
- `viewmodel/RegistroViewModel.kt`: validación reactiva, validación completa y regreso a edición conservando estado.
- `viewmodel/SesionViewModel.kt`: bloqueo síncrono contra confirmaciones repetidas o posteriores al éxito; conserva repositorio/autenticación existentes.
- `ui/screens/RegistroScreen.kt`: Continuar en lugar de guardar directamente; campos bloqueados durante una operación.
- `ui/screens/ResumenScreen.kt`: pantalla integrada con datos no sensibles, edición, confirmación y errores, manteniendo crema/café/terracota.
- `navigation/AppNavigation.kt`: misma instancia compartida, nueva ruta, guardia del borrador y limpieza del flujo al autenticar.
- `navigation/PoliticaNavegacion.kt`: reconocimiento del destino público Resumen. La elegibilidad del borrador se comprueba además en AppNavigation.
- `app/src/test/java/com/example/applouaraya_grupogp/PoliticaNavegacionTest.kt`: nueva ruta pública.

## Archivos creados

- `app/src/main/java/com/example/applouaraya_grupogp/validation/RegistroValidator.kt`
- `app/src/test/java/com/example/applouaraya_grupogp/RegistroViewModelTest.kt`
- `app/src/androidTest/java/com/example/applouaraya_grupogp/RegistroFlujoTest.kt`
- `app/src/androidTest/java/com/example/applouaraya_grupogp/RegistroConfirmacionTest.kt`
- `docs/PRUEBAS_REGISTRO_RESUMEN.md`

No se modificaron DAO, bases, esquemas, Gradle, manifiesto, productos, favoritos, QR, mapas, campañas ni el módulo de notificaciones. La integración de navegación solo incorpora Resumen al tratamiento de destinos del registro. La carpeta backend no se inspeccionó ni modificó. No se implementó diseño adaptable.

## Resultados exactos

| Verificación | Resultado |
| --- | --- |
| `assembleDebug` | BUILD SUCCESSFUL |
| `assembleDebugAndroidTest` | BUILD SUCCESSFUL |
| Unitarias | **56/56**, 0 fallos; incluye 7 pruebas nuevas de RegistroViewModel |
| `lintDebug` | BUILD SUCCESSFUL; 0 errores y 40 advertencias, igual que la auditoría previa |
| Instrumentadas pertinentes en Samsung físico | **OK (8 tests)**, **64.683 segundos**, 0 fallos y 0 omisiones |
| Esquemas Room / DAO / dependencias | Sin cambios |
| `git diff --check` | Sin errores |

Las cuatro instrumentadas nuevas verifican:

- Registro → Resumen → Editar conserva datos; corregir nombre actualiza el error; no hay cuenta hasta confirmar; el detalle no sensible se muestra sin campo de contraseña ni argumento `clave`; éxito limpia credencial y pila de navegación.
- Correo duplicado muestra error, permite editar y registrar otro correo; la cuenta original conserva su entidad y credenciales válidas.
- Atrás desde Resumen conserva el borrador; acceso directo a Resumen sin un borrador validado vuelve al formulario.
- Diez confirmaciones rápidas y cinco llamadas posteriores al éxito mantienen una sola cuenta. Se comprueba la escritura real en Room en memoria y la ausencia de una segunda cuenta.

Se ejecutaron además dos casos existentes de NavegacionPerfilTest (roles, cambio de cuenta, cierre y Atrás) y dos de AdministracionTest (permisos y preservación de cuenta reservada). Las siete unitarias nuevas verifican estado inicial, nombre reactivo, correo reactivo, clave/términos reactivos, independencia de errores, elegibilidad y conservación del borrador, y limpieza de datos.

No quedan errores de compilación ni pruebas fallidas. Las advertencias de lint preexistentes no bloquean la compilación; no se hicieron cambios fuera del alcance para corregirlas.

## Aislamiento y comandos ejecutados

El dispositivo ADB reconocido fue Samsung SM-A166M, serie `R5CY42JVXMT`. Las pruebas seleccionadas utilizan exclusivamente Room en memoria y preferencias con nombres de prueba únicos. RegistroFlujoTest inyecta la base en AppNavigation y desmonta primero Compose antes de cerrar sus recursos. No se abre la base personal para esos casos ni se publican alertas Android desde la navegación de prueba.

Se actualizaron APK principal y de pruebas mediante `adb install -r`, conservando los datos. No se desinstaló la aplicación ni se borraron usuarios, cotizaciones o favoritos.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --offline --no-configuration-cache --max-workers=1
$adbLou = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
& $adbLou devices -l
& $adbLou -s R5CY42JVXMT install -r app/build/outputs/apk/debug/app-debug.apk
& $adbLou -s R5CY42JVXMT install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adbLou -s R5CY42JVXMT shell am instrument -w -r -e class 'com.example.applouaraya_grupogp.RegistroFlujoTest,com.example.applouaraya_grupogp.RegistroConfirmacionTest,com.example.applouaraya_grupogp.NavegacionPerfilTest,com.example.applouaraya_grupogp.AdministracionTest' com.example.applouaraya_grupogp.test/androidx.test.runner.AndroidJUnitRunner
```

Evidencias locales generadas:

- `app/build/reports/instrumentadas-registro-resumen.txt`
- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/test-results/testDebugUnitTest/TEST-com.example.applouaraya_grupogp.RegistroViewModelTest.xml`
- `app/build/reports/lint-results-debug.html`

## Prueba concreta en el celular

1. Abre la versión actualizada. Si tienes sesión, ciérrala normalmente para llegar a Bienvenida, conservando todos tus datos.
2. Pulsa «Crear una cuenta» y luego «Continuar» sin completar el formulario: deben aparecer los errores de nombre, correo, clave y términos.
3. Escribe un correo incompleto, por ejemplo `cliente@`: su error debe mantenerse. Corrígelo a un correo ficticio no utilizado, como `guia11-nuevo@example.test`: el error debe desaparecer inmediatamente, sin otro envío. Repite con nombre vacío/válido, clave corta/de seis o más caracteres y términos.
4. Completa el formulario y pulsa «Continuar». Debe abrirse «Resumen de registro» con nombre, correo, términos y tipo de cuenta. No debe aparecer la contraseña, ni puntos de enmascaramiento, ni un campo que permita revelarla.
5. Pulsa «Volver a editar»: nombre, correo, clave y términos siguen en el formulario. Cambia el nombre y continúa de nuevo; el resumen debe mostrar el cambio. Prueba también Atrás del sistema desde Resumen.
6. Pulsa «Confirmar registro». Mientras guarda, los botones quedan bloqueados; al terminar debe abrirse el Dashboard con el nombre correcto. Atrás no debe recuperar una confirmación anterior. La cuenta se conserva en Room por el mecanismo habitual.
7. Para comprobar duplicados sin alterar cuentas previas: cierra sesión, intenta registrar ese mismo correo, continúa y confirma. Debe aparecer «Este correo ya está registrado». Vuelve a editar, cambia a otro correo ficticio no ocupado y confirma nuevamente.
8. Comprueba que la cuenta previa todavía puede iniciar sesión con su contraseña original. No borres datos ni desinstales para repetir la demostración; utiliza correos ficticios nuevos o el caso de duplicado.

No se hicieron commits, push ni merges. La corrección se detiene en la Guía 11; adaptabilidad y las demás observaciones de la auditoría siguen pendientes de autorización.
