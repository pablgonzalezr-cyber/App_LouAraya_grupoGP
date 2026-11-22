package com.example.applouaraya_grupogp

import com.example.applouaraya_grupogp.repository.DatosProducto
import com.example.applouaraya_grupogp.repository.ProductosRemotos
import com.example.applouaraya_grupogp.repository.PublicacionProducto
import java.net.ServerSocket
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.ResponseBody

class ProductosRetrofitTest {
    @Test fun administracionRechazaHttpLocal() {
        assertThrows(IllegalArgumentException::class.java) {
            ProductosRemotos.api("http://127.0.0.1:8095/", administrativa = true)
        }
    }

    @Test fun conexionRechazadaTieneMensajeParaReintentar() {
        val puerto = ServerSocket(0).use { it.localPort }
        val api = ProductosRemotos.api("http://127.0.0.1:$puerto/", administrativa = false)
        val error = assertThrows(IllegalStateException::class.java) {
            runBlocking { ProductosRemotos.consultar { api.publicados() } }
        }
        assertTrue(error.message.orEmpty().contains("conexión"))
    }

    @Test fun respuesta403IdentificaPermisosDelServidor() = runBlocking {
        comprobarHttp(403) { throw HttpException(Response.error<Unit>(403, ResponseBody.create(null, ""))) }
    }

    // Ejecutar solo contra la instancia descartable creada para esta prueba.
    @Test fun contratoCompletoContraSpringBoot() {
        val url = System.getenv("LOU_PRUEBA_RETROFIT_URL")
        assumeTrue(!url.isNullOrBlank())
        val almacen = requireNotNull(System.getenv("LOU_PRUEBA_RETROFIT_TRUSTSTORE"))
        val claveAlmacen = requireNotNull(System.getenv("LOU_PRUEBA_RETROFIT_TRUSTSTORE_CLAVE"))
        val anterior = System.setProperty("javax.net.ssl.trustStore", almacen)
        val anteriorClave = System.setProperty("javax.net.ssl.trustStorePassword", claveAlmacen)
        try {
            val publica = ProductosRemotos.api(requireNotNull(url), administrativa = false)
            val admin = ProductosRemotos.api(url, administrativa = true)
            val credencial = ProductosRemotos.credencial(requireNotNull(System.getenv("LOU_ADMIN_USUARIO")),
                requireNotNull(System.getenv("LOU_ADMIN_CLAVE")))
            runBlocking {
                assertEquals((1L..8L).toList(), publica.publicados().map { it.id })
                comprobarHttp(401) { admin.administracion("Basic invalid") }
                val datos = DatosProducto("Control 3 Retrofit", "Producto de prueba aislada", "Interiores",
                    "Algodón", "25.25".toBigDecimal(), "32.75".toBigDecimal(), "24990.50".toBigDecimal(), true, true)
                val id = admin.crear(credencial, datos).id
                assertEquals(datos.altoCm, publica.detalle(id).altoCm)
                assertTrue(publica.publicados().any { it.id == id })
                admin.editar(credencial, id, datos.copy(nombre = "Control 3 editado"))
                assertEquals("Control 3 editado", publica.detalle(id).nombre)
                assertEquals(datos.precio, publica.detalle(id).precio)
                admin.publicar(credencial, id, PublicacionProducto(false))
                comprobarHttp(404) { publica.detalle(id) }
                assertFalse(publica.publicados().any { it.id == id })
                assertFalse(admin.administracion(credencial).single { it.id == id }.publicado)
                admin.publicar(credencial, id, PublicacionProducto(true))
                assertTrue(publica.detalle(id).publicado)
                admin.archivar(credencial, id)
                assertFalse(admin.administracion(credencial).any { it.id == id })
                assertFalse(publica.publicados().any { it.id == id })
                comprobarHttp(404) { publica.detalle(id) }
                comprobarHttp(404) { admin.editar(credencial, id, datos) }
            }
        } finally {
            if (anterior == null) System.clearProperty("javax.net.ssl.trustStore")
            else System.setProperty("javax.net.ssl.trustStore", anterior)
            if (anteriorClave == null) System.clearProperty("javax.net.ssl.trustStorePassword")
            else System.setProperty("javax.net.ssl.trustStorePassword", anteriorClave)
        }
    }

    private suspend fun comprobarHttp(codigo: Int, accion: suspend () -> Unit) {
        try {
            ProductosRemotos.consultar(accion)
            fail("Se esperaba HTTP $codigo")
        } catch (e: IllegalStateException) {
            assertEquals(codigo, (e.cause as HttpException).code())
            assertTrue(e.message.orEmpty().contains(codigo.toString()))
        }
    }
}
