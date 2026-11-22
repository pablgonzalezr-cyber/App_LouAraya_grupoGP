package com.example.applouaraya_grupogp

import com.example.applouaraya_grupogp.repository.*
import com.google.gson.JsonParser
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*

class CotizacionesRemotasTest {
    private val servidor = MockWebServer()
    private lateinit var repo: CotizacionesRemotasRepository
    private val clave = java.util.UUID.randomUUID().toString()
    private val usuario = "cliente@example.test"
    private val token = "a0bc8c35-9167-4d7d-bad0-15105c6a9991"
    private val respuesta = """{"id":15,"usuarioId":3,"tokenEnvio":"$token","descripcion":"Solicitud de un tapiz","categoria":"Interiores","material":"Algodón","anchoCm":2.25,"largoCm":8,"cantidad":1,"fechaRequerida":null,"productoReferenciaId":null,"productoReferenciaNombre":null,"creadaEn":12345,"estado":"PENDIENTE"}"""
    private val pendiente = EstadoCotizacionRemota.PENDIENTE
    private val revision = EstadoCotizacionRemota.EN_REVISION

    @Before fun iniciar() {
        servidor.start()
        repo = CotizacionesRemotasRepository(servidor.url("/").newBuilder().host("127.0.0.1").build().toString())
    }
    @After fun cerrar() = servidor.shutdown()

    private fun respuesta(body: String, codigo: Int = 200) = servidor.enqueue(MockResponse().setResponseCode(codigo).setBody(body))
    private fun peticion(metodo: String, ruta: String, basic: String?): RecordedRequest {
        val r = servidor.takeRequest()
        assertEquals(metodo, r.method)
        assertEquals(ruta, r.path)
        assertEquals(basic, r.getHeader("Authorization"))
        return r
    }

    @Test fun registroYFlujoCliente() = runBlocking {
        respuesta("""{"id":3,"correo":"$usuario"}""", 201)
        assertEquals(3L, repo.registrar(usuario, clave).id)
        val registro = JsonParser.parseString(peticion("POST","/api/clientes/registro",null).body.readUtf8()).asJsonObject
        assertEquals(setOf("correo","clave"), registro.keySet())
        assertEquals(clave, registro["clave"].asString)
        val basic = Credentials.basic(usuario, clave)
        respuesta("[]")
        val cuenta = repo.iniciarSesionCliente(usuario, clave)
        peticion("GET","/api/cotizaciones/mias",basic)
        val datos = NuevaCotizacion(token, "Solicitud de un tapiz", "Interiores","Algodón",
            BigDecimal("2.25"), BigDecimal("8"), 1, "2027-01-20", 7L, "Diseño histórico")
        for (codigo in listOf(201, 200)) {
            respuesta(respuesta, codigo)
            assertEquals(15L, repo.crear(cuenta, datos).id)
            val json = JsonParser.parseString(peticion("POST","/api/cotizaciones",basic).body.readUtf8()).asJsonObject
            assertEquals(token, json["tokenEnvio"].asString)
            assertEquals("2027-01-20", json["fechaRequerida"].asString)
            assertEquals(7L, json["productoReferenciaId"].asLong)
            assertEquals(BigDecimal("2.25"), json["anchoCm"].asBigDecimal)
            assertFalse(json.has("usuarioId"))
        }
        respuesta("[$respuesta]")
        assertEquals(pendiente, repo.historial(cuenta).single().estado)
        peticion("GET","/api/cotizaciones/mias",basic)
        respuesta(respuesta)
        assertNull(repo.propia(cuenta,15).fechaRequerida)
        peticion("GET","/api/cotizaciones/mias/15",basic)
        Unit
    }

    @Test fun consultasAdminYPrecondicionDeEstado() = runBlocking {
        val basic = Credentials.basic("gestora", clave)
        respuesta("[]")
        val cuenta = repo.iniciarSesionAdmin("gestora", clave)
        peticion("GET","/api/admin/cotizaciones",basic)
        respuesta("[$respuesta]")
        assertEquals(12345L, repo.solicitudes(cuenta, pendiente).single().creadaEn)
        peticion("GET","/api/admin/cotizaciones?estado=PENDIENTE",basic)
        respuesta(respuesta)
        assertEquals(15L, repo.detalleAdmin(cuenta,15).id)
        peticion("GET","/api/admin/cotizaciones/15",basic)
        respuesta(respuesta.replace("PENDIENTE","EN_REVISION"))
        assertEquals(revision, repo.cambiarEstado(cuenta,15,pendiente,revision).estado)
        val json = JsonParser.parseString(peticion("PATCH","/api/admin/cotizaciones/15/estado",basic).body.readUtf8()).asJsonObject
        assertEquals("PENDIENTE", json["esperado"].asString)
        assertEquals("EN_REVISION", json["nuevo"].asString)
    }

    @Test fun erroresHttpYRedSeDistinguen() {
        for (codigo in listOf(400,401,403,404,409,503)) {
            respuesta("{}", codigo)
            val e = assertThrows(ErrorCotizaciones::class.java) { runBlocking { repo.iniciarSesionCliente(usuario, clave) } }
            assertEquals(codigo,e.codigo)
        }
        servidor.shutdown()
        assertThrows(java.io.IOException::class.java) { runBlocking { repo.iniciarSesionCliente(usuario, clave) } }
    }

    @Test fun urlYCredencialInvalidasNoEnvianPeticiones() {
        for (url in listOf("", "http://example.org/", "https://a:b@example.org/", "https://example.org/ruta")) {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { CotizacionesRemotasRepository(url).iniciarSesionAdmin(usuario, clave) }
            }
        }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.iniciarSesionCliente("a:b", clave) } }
        assertEquals(0, servidor.requestCount)
    }
}
