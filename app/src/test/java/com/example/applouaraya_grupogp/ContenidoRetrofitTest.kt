package com.example.applouaraya_grupogp

import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.validation.*
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.HttpException

class ContenidoRetrofitTest {
    @Test fun formularioEnviaFechasIsoYEstadosIndependientes() {
        val datos = CampaniaFormulario(titulo = " Verano ", inicio = "28/12/2026", termino = "01/01/2027",
            publicada = true, activa = false).datos()
        assertEquals("Verano", datos.titulo)
        assertEquals("2026-12-28", datos.inicio)
        assertEquals("2027-01-01", datos.termino)
        assertTrue(datos.publicada)
        assertFalse(datos.activa)
    }

    @Test fun coleccionConservaIdsDeMiembrosAlEditar() {
        val datos = ColeccionFormulario(nombre = " Taller ", productos = setOf(1, 22), publicada = false).datos()
        assertEquals("Taller", datos.nombre)
        assertEquals(setOf(1, 22), datos.productos)
        assertFalse(datos.publicada)
    }

    @Test fun cicloDeContenidoContraSpringAislado() {
        val url = System.getenv("LOU_PRUEBA_RETROFIT_URL")
        assumeTrue(!url.isNullOrBlank())
        val propiedades = mapOf(
            "javax.net.ssl.trustStore" to requireNotNull(System.getenv("LOU_PRUEBA_RETROFIT_TRUSTSTORE")),
            "javax.net.ssl.trustStorePassword" to requireNotNull(System.getenv("LOU_PRUEBA_RETROFIT_TRUSTSTORE_CLAVE"))
        )
        val previas = propiedades.mapValues { (k, v) -> System.setProperty(k, v) }
        try {
            val api = ProductosRemotos.retrofit(requireNotNull(url), true).create(ContenidoApi::class.java)
            val productos = ProductosRemotos.api(url, true)
            val cuenta = ProductosRemotos.credencial(requireNotNull(System.getenv("LOU_ADMIN_USUARIO")),
                requireNotNull(System.getenv("LOU_ADMIN_CLAVE")))
            runBlocking {
                comprobarHttp(401) { api.coleccionesAdmin("Basic invalid") }
                comprobarHttp(401) { api.campaniasAdmin("Basic invalid") }
                val uno = "1".toBigDecimal()
                val oculto = productos.crear(cuenta, DatosProducto("Prueba de asociación", "Producto oculto de prueba",
                    "Otro", "Algodón", uno, uno, uno, true, false)).id
                val borrador = DatosColeccion("Colección Retrofit", "Descripción de prueba", true, false, setOf(1, oculto.toInt()))
                val id = api.crearColeccion(cuenta, borrador).id
                assertFalse(api.colecciones().any { it.id == id })
                api.publicarColeccion(cuenta, id, PublicacionContenido(true))
                assertEquals(listOf(1L), api.colecciones().single { it.id == id }.productos.map { it.id })
                api.desactivarColeccion(cuenta, id)
                assertFalse(api.colecciones().any { it.id == id })
                val desactivada = api.coleccionesAdmin(cuenta).single { it.id == id }
                assertFalse(desactivada.activa)
                assertTrue(desactivada.publicada)
                assertEquals(setOf(1L, oculto), desactivada.productos.toSet())
                api.editarColeccion(cuenta, id, borrador.copy(nombre = "Colección editada", publicada = true))
                assertEquals("Colección editada", api.colecciones().single { it.id == id }.nombre)
                comprobarHttp(400) { api.editarColeccion(cuenta, id, borrador.copy(publicada = true, productos = emptySet())) }
                api.publicarColeccion(cuenta, id, PublicacionContenido(false))
                assertFalse(api.colecciones().any { it.id == id })
                productos.archivar(cuenta, oculto)

                val hoy = LocalDate.now(ZoneId.of("America/Santiago"))
                val vigente = DatosCampania("Campaña Retrofit", "Descripción de prueba", hoy.toString(), hoy.toString(), true, true)
                val c = api.crearCampania(cuenta, vigente).id
                assertTrue(api.campanias().any { it.id == c })
                api.publicarCampania(cuenta, c, PublicacionContenido(false))
                comprobarHttp(404) { api.campania(c) }
                api.publicarCampania(cuenta, c, PublicacionContenido(true))
                assertTrue(api.campanias().any { it.id == c })
                api.editarCampania(cuenta, c, vigente.copy(inicio = hoy.minusDays(2).toString(), termino = hoy.minusDays(1).toString()))
                assertFalse(api.campanias().any { it.id == c })
                api.editarCampania(cuenta, c, vigente.copy(inicio = hoy.plusDays(1).toString(), termino = hoy.plusDays(2).toString()))
                assertFalse(api.campanias().any { it.id == c })
                comprobarHttp(400) { api.editarCampania(cuenta, c, vigente.copy(termino = hoy.minusDays(1).toString())) }
                api.desactivarCampania(cuenta, c)
                assertFalse(api.campaniasAdmin(cuenta).single { it.id == c }.activa)
                api.editarCampania(cuenta, c, vigente.copy(titulo = "Campaña reactivada"))
                assertEquals("Campaña reactivada", api.campania(c).titulo)
            }
        } finally {
            previas.forEach { (k, valor) -> if (valor == null) System.clearProperty(k) else System.setProperty(k, valor) }
        }
    }

    private suspend fun comprobarHttp(codigo: Int, accion: suspend () -> Unit) {
        try { accion(); fail("Se esperaba HTTP $codigo") }
        catch (e: HttpException) { assertEquals(codigo, e.code()) }
    }
}
