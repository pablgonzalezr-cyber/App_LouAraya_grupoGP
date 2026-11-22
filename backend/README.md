# Lou Araya — Etapa 7A: backend de productos

Backend académico propio y provisional, independiente del supuesto servidor de la cátedra. No está conectado a Android ni a Room. Implementado y verificado el **18 de octubre de 2026** en `feature/cotizaciones`.

## Inicio desde PowerShell de Android Studio

Desde la raíz del repositorio, en PowerShell:

```powershell
$env:LOU_ADMIN_USUARIO = 'admin-demo'
$claveLou = Read-Host 'Clave demo de 12 o más caracteres ASCII (máximo 72)' -AsSecureString
$env:LOU_ADMIN_CLAVE = [System.Net.NetworkCredential]::new('', $claveLou).Password
powershell -NoProfile -ExecutionPolicy Bypass -File .\backend\Start-Backend.ps1
```

**Swagger:** <http://127.0.0.1:8095/swagger-ui/index.html> (también `/swagger-ui.html`).

**OpenAPI JSON:** <http://127.0.0.1:8095/v3/api-docs>.

**Catálogo público:** <http://127.0.0.1:8095/api/productos>.

`Start-Backend.ps1` selecciona el JDK portátil existente en `backend/.tools/jdk-17.0.20.1+1`, comprueba Java 17, cambia al directorio `backend` y ejecuta `mvnw.cmd spring-boot:run`. No cambia Java global ni el JDK de Gradle/Android Studio. Restaura el directorio y JAVA_HOME del proceso al terminar.

El servidor permanece en esa terminal. Para detenerlo usa **Ctrl+C**; si Windows pregunta «¿Desea terminar el trabajo por lotes?», responde **S**. Reinícialo con el mismo comando y el mismo usuario/clave de esa terminal.

Las variables se heredan por el proceso hijo y no se guardan en archivos ni en Git. No hay contraseña administrativa predeterminada. Las credenciales de Android no se utilizan aquí. Los archivos `.env` se ignoran, pero **no se cargan automáticamente**: configura las variables en PowerShell. La clave tiene mínimo 12 caracteres y máximo 72 bytes UTF-8; usar ASCII evita confusión con ese límite de BCrypt.

La opción `-ExecutionPolicy Bypass` se limita al proceso PowerShell que ejecuta el script y no cambia la política global. Fue necesaria porque este computador bloquea scripts por defecto.

### JDK y Maven en otro computador

El JDK y sus ZIP no se versionan. Para preparar uno portátil en Windows x64:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\backend\Setup-Java17.ps1
```

El script descarga Eclipse Temurin 17 desde la API oficial Adoptium, comprueba SHA-256 antes de extraerlo y reutiliza un JDK 17 que ya exista. Requiere Internet y `curl.exe` de Windows. También puedes proporcionar tu propio JDK 17 mediante JAVA_HOME; no es necesario instalarlo globalmente.

Maven Wrapper descarga Maven 3.9.16 automáticamente la primera vez. No requiere el Maven instalado en el escritorio del autor. El wrapper es la variante oficial `only-script`, sin JAR adicional de wrapper.

## Modelo y endpoints

| Campo JSON | Significado / validación |
| --- | --- |
| `id` | Long generado por el servidor; no se envía en POST/PUT |
| `nombre` | Obligatorio, no vacío, máximo 100 caracteres |
| `descripcion` | Obligatoria, no vacía, máximo 2000 |
| `categoria`, `material` | Obligatorios, no vacíos, máximo 100 cada uno |
| `anchoCm`, `altoCm` | Decimales positivos entre 0.01 y 10000 cm, hasta dos decimales |
| `precio` | Referencial en CLP, positivo, hasta nueve dígitos enteros y dos decimales |
| `disponible` | Boolean obligatorio: stock/capacidad de fabricación |
| `publicado` | Boolean obligatorio: visible en consultas públicas |

Se usa BigDecimal para precio y medidas. `largoCm` de Android corresponde a `altoCm`; `activo` corresponde a `publicado`. **Un producto publicado puede no estar disponible**: la Cortina Macramé inicial (ID 6) demuestra esa diferencia. Ocultar cambia solo publicación, no disponibilidad ni ID.

| Método y ruta | Acceso | Resultado |
| --- | --- | --- |
| GET `/api/productos` | Público | 200, lista publicada ordenada por ID |
| GET `/api/productos/{id}` | Público | 200 publicado; 404 inexistente, oculto o archivado |
| GET `/api/admin/productos` | ADMIN | 200, publicados y ocultos, excluidos archivados |
| POST `/api/admin/productos` | ADMIN | 201, DTO creado y cabecera Location |
| PUT `/api/admin/productos/{id}` | ADMIN | 200, edición completa conservando ID; 404 inexistente o archivado |
| PATCH `/api/admin/productos/{id}/publicacion` | ADMIN | 200, cambia solo `publicado`; 404 inexistente o archivado |
| DELETE `/api/admin/productos/{id}` | ADMIN | 204 sin cuerpo al archivar; 404 inexistente o ya archivado |

PUT requiere todos los campos editables. PATCH acepta únicamente `{"publicado":false}` o `{"publicado":true}`. JSON con campos desconocidos se rechaza, incluyendo intentar asignar `id` desde el cliente.

DELETE realiza una baja lógica. Marca la fila como `archivado` y conserva su ID y datos para futuras referencias de cotizaciones. Los archivados quedan fuera de ambos listados, del detalle público y de PUT y PATCH. El API actual carece de una operación de restauración. Si solo necesitas retirar temporalmente un producto del catálogo público, usa PATCH de publicación.

Los errores de validación devuelven **400** con ProblemDetail y un mapa `errores` por campo. JSON ilegible o identificadores no numéricos devuelven 400. Credenciales ausentes/incorrectas devuelven **401**, y un rol no administrativo **403**. La autorización se verifica tanto en filtros HTTP como en métodos de servicio. Solo se configura una cuenta ADMIN de demostración; CLIENTE se simula exclusivamente en las pruebas para comprobar el rechazo.

### Seguridad local

- Bind explícito `127.0.0.1:8095`: no se expone al Samsung ni a la LAN.
- HTTP Basic sin sesiones; la contraseña se compara con BCrypt generado al arrancar desde el entorno.
- Swagger y consultas públicas son accesibles sin autenticación; las escrituras y el listado administrativo requieren ADMIN.
- No se habilita CORS ni consola web de H2. CSRF está desactivado para esta API JSON stateless de demostración local.
- No expongas esta configuración a una red pública sin HTTPS y una revisión de autenticación/CSRF/despliegue. No contiene JWT, Firebase ni registro de usuarios remoto.
- La interfaz Swagger no conserva autorización entre recargas mediante su opción persistAuthorization; el navegador puede conservar autenticación Basic durante su sesión.

## Colecciones y campañas

Swagger documenta los recursos públicos `/api/colecciones` y `/api/campanias` y sus equivalentes administrativos bajo `/api/admin`. Las rutas administrativas usan el mismo HTTP Basic del catálogo.

| Operación | Ruta pública | Ruta administrativa | Éxito |
| --- | --- | --- | --- |
| Listar | GET `/api/{recurso}` | GET `/api/admin/{recurso}` | 200 |
| Consultar | GET `/api/{recurso}/{id}` | GET `/api/admin/{recurso}/{id}` | 200 |
| Crear | | POST `/api/admin/{recurso}` | 201 con `Location` del detalle administrativo |
| Actualizar | | PUT `/api/admin/{recurso}/{id}` | 200 |
| Cambiar publicación | | PATCH `/api/admin/{recurso}/{id}/publicacion` | 200 |
| Desactivar | | DELETE `/api/admin/{recurso}/{id}` | 204 sin cuerpo |

Usa `colecciones` o `campanias` en lugar de `{recurso}`. DELETE establece `activa:false` y conserva el registro, su publicación y sus vínculos. Repetirlo devuelve 204 mientras exista el registro. Los administradores pueden consultar los inactivos y reactivarlos mediante PUT. PATCH recibe `{"publicada":true}` o `{"publicada":false}` y conserva `activa`. Un detalle público no visible o un recurso inexistente devuelve 404. Datos o IDs inválidos devuelven 400, y las rutas administrativas devuelven 401 o 403 según las credenciales y el rol.

**Colección.** POST y PUT reciben `nombre`, `descripcion`, `activa`, `publicada` y `productos`, un arreglo de IDs existentes. PUT reemplaza el conjunto completo de asociaciones en una transacción. Para publicar hace falta al menos una asociación. La respuesta administrativa devuelve `productos` como IDs, incluidos los vínculos a productos ocultos o archivados. La respuesta pública usa el mismo campo con objetos de la API de productos y solo incluye productos publicados y vigentes en el catálogo (sin archivar), ordenados por ID. Un producto publicado con `disponible:false` sigue visible con ese estado. Una colección activa y publicada puede aparecer con `productos:[]` cuando ninguno de sus miembros es visible.

**Campaña.** POST y PUT reciben `titulo`, `descripcion`, `inicio`, `termino`, `publicada` y `activa`. Las fechas viajan como `AAAA-MM-DD` y el término debe ser igual o posterior al inicio. El listado y el detalle públicos requieren que la campaña esté activa, publicada y vigente, con ambos límites incluidos según la fecha en `America/Santiago`. Las campañas no tienen asociaciones a productos en Room ni en el backend. La publicación local de Android puede generar avisos en Room. El endpoint guarda el estado comercial y esa integración queda pendiente.

Las tablas `colecciones`, `campanias` y `coleccion_productos` se agregan a la misma H2 del catálogo mediante la configuración de esquema existente. La tabla de asociaciones usa una clave compuesta y referencias a ambas tablas. Cambiar asociaciones o desactivar una colección conserva los productos.

## Prueba manual con Swagger y reinicios

1. Inicia el servidor y abre Swagger. Ejecuta GET `/api/productos`: la base de desarrollo preparada contiene **8 productos**, IDs 1–8. GET `/api/productos/6` debe mostrar `disponible:false`, `publicado:true`.
2. Pulsa **Authorize**, selecciona `basicAuth` y escribe el usuario y clave que definiste en PowerShell. No son los de Android.
3. Ejecuta POST con este ejemplo; espera **201**, anota el ID y la cabecera Location:

```json
{
  "nombre": "Tapiz demo de Swagger",
  "descripcion": "Producto ficticio para la demostración académica",
  "categoria": "Interiores",
  "material": "Algodón natural",
  "anchoCm": 45,
  "altoCm": 70,
  "precio": 29990,
  "disponible": true,
  "publicado": true
}
```

4. Consulta su ID desde GET público. Edita con PUT el mismo JSON y `precio:34990`; comprueba que el catálogo público devuelve el precio nuevo.
5. PATCH su publicación con `{"publicado":false}`: GET público por ID devuelve 404 y desaparece del listado público; GET administrativo lo conserva con el mismo ID.
6. Detén con Ctrl+C y vuelve a iniciar desde la misma terminal. GET administrativo debe conservar el precio y la ocultación. Comprueba también una edición a un producto inicial: la carga de semillas no debe restablecerla.
7. PATCH con `{"publicado":true}`: reaparece sin cambiar disponibilidad. Reinicia otra vez y comprueba que permanece y no hay duplicados.
8. Envía precio negativo, nombre vacío o PATCH sin `publicado`: espera 400. Usa Logout de Authorize y prueba una operación administrativa: espera 401. Si el navegador mantiene Basic, usa una ventana privada o las pruebas automatizadas para esa comprobación.

Los cambios manuales en desarrollo quedan en `backend/data/lou-araya.mv.db`. **No borres ese archivo para reiniciar**. El script de inicio siempre cambia a `backend`, por lo que la ruta relativa es estable.

## Persistencia e inicialización

Este microservicio es dueño de la tabla `productos` y de su secuencia de IDs. El futuro microservicio de cotizaciones deberá ejecutarse en otro puerto y tener su propia base de datos, con un archivo distinto si también usa H2. El perfil `test` usa una base H2 en memoria separada y la verificación HTTP crea su propio archivo bajo `backend/verification/`.

H2 de desarrollo: `jdbc:h2:file:./data/lou-araya;DB_CLOSE_ON_EXIT=FALSE`. Hibernate usa `ddl-auto=update` para esta etapa académica. No hay `create-drop` en desarrollo. Los ocho productos se copiaron de ProductosDemo.kt conservando nombres, descripciones, medidas, precios, disponibilidad e IDs 1–8. El INSERT inicial solo actúa si falta ese ID; no sobrescribe un producto existente. Una secuencia independiente comienza en 9, evitando colisionar con los IDs reservados y conservando su avance en disco.

Al actualizar una base existente, Hibernate agrega `archivado` con valor predeterminado `false`. La carga inicial comprueba la existencia del ID en la tabla, por lo que una semilla archivada permanece archivada tras reiniciar.

Las fotografías y sus nombres de recurso Android no se migran: este backend no sirve ni sube imágenes. Tampoco copia ediciones del Room del Samsung. Es un catálogo independiente.

## Compilación y pruebas automatizadas

Desde la raíz, en una terminal dedicada al backend:

```powershell
Set-Location .\backend
$env:JAVA_HOME = (Get-ChildItem .\.tools -Directory -Filter 'jdk-17*' | Select-Object -First 1).FullName
.\mvnw.cmd --version
.\mvnw.cmd -B -ntp verify
```

Ese JAVA_HOME afecta solo a esta terminal; no cambies el JDK de Gradle en Android Studio. Si prefieres un JDK propio, asigna su ruta a JAVA_HOME. Las pruebas verifican explícitamente que se ejecutan en Java 17 y con una URL H2 `mem`, de modo que `verify` no aprueba por accidente en Java 24 ni contra la base de desarrollo.

`ProductoApiTest` utiliza el perfil `test`, H2 en memoria con nombre aleatorio, transacciones revertidas y credenciales ficticias de pruebas definidas solo en `src/test/resources`. No utiliza las variables de credenciales de desarrollo. Maven produce un JAR ejecutable en `target/backend-0.0.1-SNAPSHOT.jar`.

### Verificación HTTP y persistencia reales, aisladas

Con el puerto 8095 libre y después de `verify`:

```powershell
# Desde backend:
powershell -NoProfile -ExecutionPolicy Bypass -File .\Verify-Backend.ps1 -JavaHome $env:JAVA_HOME
```

El script genera credenciales aleatorias solo para sus procesos, inicia tres veces el JAR real en 8095 y comprueba 31 condiciones. Usa una H2 de archivo nueva bajo `backend/verification/<UUID>/persistencia.mv.db`, **nunca `data/lou-araya`**. Verifica el CRUD de productos, la persistencia de los vínculos de una colección y las fechas, publicación y desactivación de una campaña. Al archivar un producto, comprueba que el vínculo sobreviva al reinicio y que el producto quede fuera de la colección pública. Detiene únicamente los procesos Java que creó. Los reinicios del script usan terminación del proceso y WRITE_DELAY=0 en esa base aislada. Conserva los archivos y logs de evidencia.

## Resultados exactos — 18/10/2026

| Verificación | Resultado |
| --- | --- |
| Maven Wrapper 3.9.16 con Java 17 | Correcto |
| `mvnw.cmd -B -ntp verify` | **BUILD SUCCESS**, 11.073 s en la última ejecución |
| Pruebas JUnit / MockMvc | **10 ejecutadas, 0 fallos, 0 errores, 0 omitidas**, 7.935 s |
| Verificación HTTP con JAR real | **18/18 comprobaciones aprobadas** |
| Swagger UI, JavaScript y JSON OpenAPI reales | HTTP 200 |
| Inicio 1 y dos reinicios de H2 de archivo | Precio, ocultación/reactivación y edición de semilla conservados; 9 IDs distintos al finalizar |
| Inicio normal con Start-Backend.ps1 | Java 17, bind 127.0.0.1:8095, base de desarrollo con 8 productos |
| Protección de la base de desarrollo durante Maven | SHA-256 idéntico antes y después de `verify` |
| Android / Room / Samsung | Sin modificaciones ni acciones sobre el dispositivo |

Las 10 pruebas comprueban: semillas y disponibilidad independiente; creación; edición e idempotencia; ocultación/reactivación; 401 de todas las rutas administrativas y credenciales erróneas; 403 en HTTP y servicio; datos inválidos/JSON desconocido/ID no numérico; inexistentes 404; Swagger/OpenAPI y requisitos Basic; aislamiento en memoria y Java 17.

Evidencia local ignorada por Git:

- `.tools/maven-verify.log`
- `target/surefire-reports/cl.louaraya.backend.ProductoApiTest.txt` y XML correspondiente.
- `verification/9a01e953-d605-4576-a75a-c9b62ead68e6/resultados.txt`, tres logs de arranque y su base persistente de prueba.

El servidor quedó **detenido** al terminar, para que puedas iniciarlo con tus credenciales. La base de desarrollo conserva los ocho productos originales; los cambios de verificación quedaron exclusivamente en la base aislada.

## Estructura y archivos

Todo el trabajo está dentro de `backend/`; no se modificaron archivos preexistentes de Android ni del repositorio raíz.

```text
backend/
  .gitignore
  .mvn/wrapper/maven-wrapper.properties
  mvnw, mvnw.cmd
  pom.xml
  Setup-Java17.ps1
  Start-Backend.ps1
  Verify-Backend.ps1
  README.md
  src/main/resources/application.properties
  src/main/java/cl/louaraya/backend/
    BackendApplication.java
    controller/ProductoController.java
    service/ProductoService.java
    repository/ProductoRepository.java
    model/entity/Producto.java
    dto/ProductoRequest.java
    dto/ProductoResponse.java
    dto/PublicacionRequest.java
    config/SecurityConfig.java
    config/OpenApiConfig.java
    config/DatosIniciales.java
    exception/ApiExceptionHandler.java
    exception/ProductoNoEncontrado.java
  src/test/resources/application-test.properties
  src/test/java/cl/louaraya/backend/ProductoApiTest.java
```

No se versionan `.tools`, `target`, `data`, `verification`, `.env`, claves/certificados, logs ni temporales. Los scripts PowerShell se guardan como UTF-8 con BOM para ser compatibles con Windows PowerShell 5.1 y los textos en español.

## Versiones resueltas

| Componente | Versión |
| --- | --- |
| Eclipse Temurin JDK portátil | 17.0.20.1+1 |
| Maven / Maven Wrapper | 3.9.16 / 3.3.4 (only-script) |
| Spring Boot | 3.5.16 |
| Spring Data JPA | 3.5.13 (gestionado por Boot) |
| Hibernate ORM | 6.6.53.Final (gestionado por Boot) |
| Spring Security | 6.5.11 (gestionado por Boot) |
| H2 | 2.3.232 (gestionado por Boot) |
| springdoc-openapi WebMVC UI | 2.8.17 |
| Swagger UI | 5.32.2 (transitiva) |

El POM incluye Spring Web, Data JPA, Bean Validation, Security, H2, Spring Boot Test, Security Test y springdoc. No incorpora dependencias al módulo Android.

## Problemas resueltos y límites

- Descarga de JDK interrumpida: se completó y verificó el SHA-256 oficial.
- Wrapper apuntaba a Maven `3` por interpretación de un argumento sin comillas en PowerShell: se regeneró con `'-Dmaven=3.9.16'` y se ejecutó correctamente.
- BCrypt estaba creado sin registrar el PasswordEncoder de autenticación: se añadió el bean y las pruebas Basic reales aprobaron.
- Una prueba de rol consultaba el servicio después de que MockMvc limpiara SecurityContext: se ordenaron las comprobaciones y se mantienen ambas aserciones HTTP/servicio.
- Scripts bloqueados por la política local y lectura ANSI de acentos: se usa PowerShell hijo con política de proceso y archivos UTF-8 BOM, sin cambiar políticas globales.
- Mockito muestra un aviso de CDS del JDK durante pruebas; no implica fallo. springdoc advierte que Swagger está habilitado: aquí es intencional para la demostración local.

No quedan fallos de compilación/pruebas pendientes. Swagger fue verificado mediante HTTP, recursos JavaScript y documento OpenAPI; el recorrido visual y las credenciales que elijas se prueban manualmente en tu navegador. H2 y `ddl-auto=update` son para demostración, no una política de migraciones de producción. No hay paginación, carga de fotografías, control optimista de ediciones simultáneas, usuarios persistidos, TLS ni conexión móvil; no se añadieron porque quedan fuera de esta etapa pequeña. Antes de cualquier exposición pública se requiere revisar esos aspectos y la versión soportada del stack.

Sin commits, push ni merges. No se avanzó a cotizaciones, Retrofit ni otras etapas.

## Fuentes oficiales consultadas

- [Spring Boot 3.5: requisitos Java y Maven](https://docs.spring.io/spring-boot/3.5/system-requirements.html).
- [Publicación de Spring Boot 3.5.16](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/).
- [Compatibilidad springdoc 2.x / Boot 3.5.x](https://springdoc.org/v2/).
- [Maven Wrapper](https://maven.apache.org/wrapper/).
- [API oficial de Eclipse Adoptium](https://api.adoptium.net/).

## Android con Retrofit para productos

El APK debug consulta `http://127.0.0.1:8095/` de forma predeterminada. En un Samsung, esa dirección pertenece al teléfono. ADB reenvía el puerto del teléfono al mismo puerto en Windows por USB mientras el dispositivo esté conectado y autorizado. Spring puede seguir escuchando en `127.0.0.1` en Windows.

```powershell
adb devices -l
adb reverse tcp:8095 tcp:8095
adb reverse --list
.\gradlew.bat assembleDebug
```

Usa `-PlouCatalogoUrl=http://10.0.2.2:8095/` para acceder al host desde un emulador Android estándar sin `adb reverse`. La excepción HTTP de debug está limitada a esas dos direcciones. Para release configura `-PlouCatalogoUrl=https://…/`; sin una URL, el catálogo mostrará un error de configuración. Conserva la barra final en ambas URLs.

### Demostración administrativa por USB

Usa Java 17 y activa TLS en la misma instancia de Spring. El almacén PKCS12 debe contener la clave privada y el certificado de servidor con SAN de tipo IP para `127.0.0.1`, firmado por una CA de desarrollo. Instala en el Samsung únicamente el certificado público de esa CA como certificado de usuario. El APK debug lo acepta en las direcciones locales configuradas y sigue verificando el certificado y el nombre. Mantén la clave privada y la contraseña del almacén fuera del repositorio.

```powershell
$env:SERVER_SSL_ENABLED = 'true'
$env:SERVER_SSL_KEY_STORE = 'C:\ruta\catalogo-dev.p12'
$env:SERVER_SSL_KEY_STORE_TYPE = 'PKCS12'
# Define SERVER_SSL_KEY_STORE_PASSWORD, LOU_ADMIN_USUARIO y LOU_ADMIN_CLAVE
# en esta terminal antes de iniciar el backend.
.\backend\Start-Backend.ps1
```

En otra terminal, compila debug con `-PlouCatalogoUrl=https://127.0.0.1:8095/` y conserva el mismo reenvío ADB. Si tienes un acceso HTTPS administrativo separado, usa además `-PlouCatalogoAdminUrl=https://…/`. Inicia sesión en Android con el rol administrativo local y escribe la cuenta de Spring en **Administrar productos**. La app conserva la credencial HTTP Basic en memoria mientras vive el panel. **Cambiar cuenta** permite descartarla y volver a autenticarse.

### Convivencia con Room

Retrofit obtiene la lista publicada y el detalle público. En administración, cada mutación confirmada vuelve a consultar el listado administrativo e invalida las consultas públicas activas. El catálogo también se actualiza al volver a la pantalla y admite actualización manual.

Los ocho IDs de muestra comparten las fotografías y favoritos históricos. Para el resto, los favoritos remotos usan claves distintas de los productos locales. Los favoritos anteriores siguen accesibles desde **Ver favoritos locales**. QR conserva su flujo local. Las nuevas colecciones y campañas se administran desde Spring. Los avisos de campañas anteriores conservan el acceso al contenido local. Las fotos elegidas en el panel se conservan en el dispositivo y se asocian al ID remoto después de que Spring confirma el guardado. El contrato no incluye campos de imagen ni subida de archivos.

El contrato de red lleva IDs `Long` y valores `BigDecimal`. Android comprueba el rango del ID antes de adaptarlo a `Int`. El formulario remoto conserva los decimales del servidor. Las tarjetas y la ficha reutilizan la presentación entera de Android.

### Colecciones y campañas en Android

En **Colecciones y campañas**, el administrador introduce su cuenta de Spring y puede asociar productos, configurar fechas, publicar, ocultar o desactivar contenido. Para reactivar un registro, abre **Editar**, activa **Activa** y guarda. Android envía las fechas en formato ISO y las presenta como día/mes/año. La vigencia pública la decide el servidor en la zona `America/Santiago`, incluidos los dos extremos del periodo.

Desde el inicio de cliente, abre **Colecciones y campañas** y pulsa **Actualizar**. Una colección pública muestra los productos que devuelve Spring. Al tocar un diseño se abre su ficha remota.

### Prueba de integración

`ProductosRetrofitTest` activa el recorrido completo cuando se define `LOU_PRUEBA_RETROFIT_URL`. Úsalo únicamente con una instancia desechable con H2 en memoria, datos iniciales y TLS. Define también `LOU_PRUEBA_RETROFIT_TRUSTSTORE`, `LOU_PRUEBA_RETROFIT_TRUSTSTORE_CLAVE`, `LOU_ADMIN_USUARIO` y `LOU_ADMIN_CLAVE` en la terminal de prueba. Ejecuta `.\gradlew.bat :app:testDebugUnitTest --rerun --tests "*ProductosRetrofitTest"`. La prueba crea, edita, oculta, republica y archiva un producto de prueba. Con el mismo entorno, ejecuta el filtro `--tests "*ContenidoRetrofitTest"` para las relaciones de colección, los estados de publicación, la reactivación y las fechas de campaña.
