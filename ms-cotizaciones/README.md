# Lou Araya - Microservicio de Cotizaciones

## Arquitectura

Java 17 + Spring Boot 3.5.16 + Spring Data JPA + Bean Validation + Spring Security + H2 2.3.x + Swagger / springdoc 2.8.17.

Este es un servicio **independiente del catalogo**. No edites `backend/`: el catalogo usa `8095` y H2 en `backend/data`; cotizaciones usa `8096` y H2 en `ms-cotizaciones/data/lou-cotizaciones.mv.db`. Ambas son bases de datos distintas. El modelo producto se referencia por ID externo (`Long`) y nombre historico; **no se crea foreign key entre bases**.

## Ejecutar en Windows PowerShell

Desde la raiz del repositorio:

```powershell
cd .\ms-cotizaciones
java -version  # Se necesita Java 17; configurar JAVA_HOME si no lo es
$env:LOU_COT_ADMIN_USUARIO = 'admin-cotizaciones'
$env:LOU_COT_ADMIN_CLAVE = '<CLAVE_FUERTE_DE_PRUEBAS_NO_PUBLICADA>'
.\mvnw.cmd -B -ntp clean verify
.\mvnw.cmd spring-boot:run
```

Swagger: http://127.0.0.1:8096/swagger-ui/index.html

Salud: http://127.0.0.1:8096/api/salud

Tests en memoria: `mvnw.cmd -B -ntp clean verify`. La base real no se toca en los tests.

## Endpoints REST

| Metodo | Ruta | Acceso | Objetivo |
|---|---|---|---|
| GET | /api/salud | Publico | Verificar arranque |
| POST | /api/clientes/registro | Publico | Crear cliente en servidor (email y clave) |
| POST | /api/cotizaciones | CLIENTE | Enviar cotizacion (201; repetir idem = 200; conflicto = 409) |
| GET | /api/cotizaciones/mias | CLIENTE | Historial propio |
| GET | /api/cotizaciones/mias/{id} | CLIENTE | Detalle propio, otro cliente = 404 |
| GET | /api/admin/cotizaciones | ADMIN | Todas; filtro opcional ?estado=PENDIENTE |
| GET | /api/admin/cotizaciones/{id} | ADMIN | Detalle administrativo |
| PATCH | /api/admin/cotizaciones/{id}/estado | ADMIN | Cambiar estado con precondicion esperado |

### Ejemplo de POST /api/cotizaciones

```json
{
  "tokenEnvio": "a0bc8c35-9167-4d7d-bad0-15105c6a9991",
  "descripcion": "Necesito un tapiz de macrame para mi sala de estar",
  "categoria": "Interiores",
  "material": "Algodon natural",
  "anchoCm": 60.0,
  "largoCm": 80.0,
  "cantidad": 1,
  "fechaRequerida": "2026-12-10",
  "productoReferenciaId": 1,
  "productoReferenciaNombre": "Tapiz mural"
}
```

El `usuarioId` no se acepta del cliente: lo asigna el backend a la cuenta autenticada. `creadaEn` usa milisegundos de epoch y el estado inicial siempre es `PENDIENTE`.

### PATCH /api/admin/cotizaciones/{id}/estado

```json
{"esperado":"PENDIENTE","nuevo":"EN_REVISION"}
```

Transiciones permitidas exactamente como en Android: `PENDIENTE -> EN_REVISION -> APROBADA` o `RECHAZADA`. Una precondicion desactualizada devuelve `409`; transicion prohibida `400`. No hay borrado fisico de solicitudes: se conserva el historial.

## Limitaciones que requieren integracion posterior

- Android todavia usa Room y cuentas locales. Se necesita mapear/autenticar correctamente las cuentas remotas antes de sustituir los repositorios Android por Retrofit; el ID local no equivale al ID del servidor.
- Basic con contrasenas funciona **solo en localhost para laboratorio**; no expongas el servidor ni envíes credenciales sin TLS; antes de una distribucion real se necesita un esquema de sesiones seguro y HTTPS.
- Las cotizaciones guardan una referencia historica a productos, sin FK entre microservicios. Durante la integracion sera necesario comprobar con el catalogo si el producto es valido para nueva solicitud. La fecha usa America/Santiago.
- Los avisos al usuario/administrador siguen siendo locales de Android: falta integracion para reflejar actualizaciones remotas en el centro de avisos.
- La pauta solicita cobertura >= 80% de logica del Android; las pruebas de este backend NO sustituyen ese informe de cobertura.

## Colaboracion

La guia de Gabriel incluye 3 etapas verificables; cada cambio y commit debe reflejar su trabajo real de integracion, pruebas y correcciones. No se suben datos H2, contrasenas, ni backups.
