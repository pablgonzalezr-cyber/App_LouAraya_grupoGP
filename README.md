Lou Araya Macramé
Aplicación móvil desarrollada para Desarrollo de Aplicaciones Móviles (DSY1105, sección 005D). Permite explorar productos de macramé, solicitar cotizaciones y gestionar contenido desde un panel de administración.

Integrantes: Pablo González y Gabriel Chodil.
Funcionalidades
- Catálogo de productos con detalles, favoritos y lectura de códigos QR.
- Colecciones, campañas, novedades y administración de contenido.
- Registro de usuarios, perfiles y navegación según rol.
- Solicitud, historial y seguimiento de cotizaciones, con revisión y cambios de estado desde administración.
- Avisos en la aplicación y conversión referencial de CLP a USD mediante la API de Mi Indicador.
Tecnologías y estructura
Carpeta	Tecnologías / función
app/	Android, Kotlin, Jetpack Compose, MVVM, Room, Retrofit y OkHttp
backend/	Spring Boot (Java 17), H2 y API REST de productos, colecciones y campañas; puerto 8095
ms-cotizaciones/	Spring Boot (Java 17), H2 y API REST de clientes y cotizaciones; puerto 8096


Los dos microservicios inicializan su base H2 mediante schema.sql y data.sql, ubicados en src/main/resources/.
Ejecución local
Requisitos: Android Studio con SDK Android, JDK 17 para Spring Boot y un teléfono Android o emulador.

En dos terminales PowerShell diferentes, configura las variables de administración e inicia cada servicio desde su carpeta:

# Terminal 1: catálogo
cd backend
$env:LOU_ADMIN_USUARIO = "admin-demo"
$clave = Read-Host "Clave administrativa" -AsSecureString
$env:LOU_ADMIN_CLAVE = [System.Net.NetworkCredential]::new("", $clave).Password
.\mvnw.cmd spring-boot:run

# Terminal 2: cotizaciones (desde la raíz del repositorio)
cd ms-cotizaciones
$env:LOU_COT_ADMIN_USUARIO = "admin-cotizaciones"
$clave = Read-Host "Clave administrativa" -AsSecureString
$env:LOU_COT_ADMIN_CLAVE = [System.Net.NetworkCredential]::new("", $clave).Password
.\mvnw.cmd spring-boot:run
Para ejecutar Android, abre el proyecto raíz en Android Studio. En una tercera terminal, desde la raíz del repositorio:
adb reverse tcp:8095 tcp:8095
adb reverse tcp:8096 tcp:8096
.\gradlew.bat :app:assembleDebug "-PlouCatalogoUrl=http://127.0.0.1:8095/" "-PlouCotizacionesUrl=http://127.0.0.1:8096/"
Instala la variante debug desde Android Studio. Para la demostración con catálogo HTTPS se requiere configurar la URL y el certificado de desarrollo correspondientes. La APK release necesita direcciones de servidor accesibles desde el dispositivo; 127.0.0.1 con adb reverse es una configuración de laboratorio.
Pruebas y documentación
.\gradlew.bat :app:testDebugUnitTest
Cada microservicio también dispone de pruebas ejecutables con mvnw.cmd clean verify. Sus archivos README.md incluyen detalles de los endpoints y su configuración.

Proyecto académico — Lou Araya Macramé.
