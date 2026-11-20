# Lou Araya - ms-cotizaciones

Microservicio **independiente** del catalogo `backend/`: Spring Boot 3.5.16, Java 17, Spring Security, Spring Data JPA, H2 en archivo (`data/lou-cotizaciones.mv.db`) y puerto 8096. El catalogo usa su propio puerto 8095 y otra BD.

## Ejecutar desde la carpeta ms-cotizaciones (PowerShell)

```powershell
java -version  # Debe ser 17; corrige JAVA_HOME si es necesario
$env:LOU_COT_ADMIN_USUARIO = 'admin-cotizaciones'
$env:LOU_COT_ADMIN_CLAVE = '<TU_CLAVE_LOCAL_DE_AL_MENOS_12_CARACTERES>'
.\mvnw.cmd -B -ntp clean verify
.\mvnw.cmd spring-boot:run
```

- Salud: http://127.0.0.1:8096/api/salud
- Swagger: http://127.0.0.1:8096/swagger-ui/index.html
- OpenAPI JSON: http://127.0.0.1:8096/v3/api-docs
- `POST /api/clientes/registro` publica `{ "correo": "...", "clave": "..." }`; cliente recibe ID nuevo del servidor.
- Un cliente registrado usa **sus propias credenciales** (HTTP Basic) para `POST /api/cotizaciones`, `GET /api/cotizaciones/mias` y `GET /api/cotizaciones/mias/{id}`.
- POST crea `201`; repetir el mismo `tokenEnvio` UUID con el mismo contenido devuelve `200` y el mismo ID; contenido distinto con el mismo token devuelve `409`.
- El JSON de solicitud utiliza: `tokenEnvio`, `descripcion`, `categoria` (Interiores/Matrimonios/Festivales/Otro), `material`, `anchoCm`, `largoCm`, `cantidad`, `fechaRequerida` (`AAAA-MM-DD` o null), `productoReferenciaId` y `productoReferenciaNombre` (ambos null o ambos presentes).
- El servidor decide `usuarioId` desde la cuenta autenticada, guarda `creadaEn` en milisegundos y comienza en estado `PENDIENTE`.

## Importante para Android y seguridad

**No hay conexion Retrofit con Android todavia.** La cuenta de Room NO equivale automaticamente a la cuenta del servidor: mas adelante debera registrarse/conectarse de forma coherente; el ID local puede diferir del ID remoto.
HTTP Basic sin TLS es exclusivamente para laboratorio en `127.0.0.1`; **no poner passwords en codigo ni exponer el puerto a Internet o Wi-Fi sin autenticacion segura sobre HTTPS**. La integracion final necesita decidir el mecanismo de sesion/autenticacion entre Android y los servicios. Los productos se referencian por ID y nombre historico, NO por FK entre bases. La existencia/visibilidad de productos se validara con el catalogo en la etapa de integracion.

Para ejecutar pruebas aisladas se usa perfil `test` con H2 en memoria. No borres ninguna BD real.