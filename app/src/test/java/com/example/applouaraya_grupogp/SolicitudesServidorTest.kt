package com.example.applouaraya_grupogp

import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.repository.EstadoCotizacionRemota.*
import com.example.applouaraya_grupogp.viewmodel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class SolicitudesServidorTest {
    private val servidor = MockWebServer()
    private val clave = UUID.randomUUID().toString()
    private val guardados = mutableMapOf<String, String>()
    private lateinit var repo: CotizacionesRemotasRepository

    @Before fun iniciar() {
        servidor.start()
        repo = CotizacionesRemotasRepository(servidor.url("/").newBuilder().host("127.0.0.1").build().toString())
    }
    @After fun cerrar() = servidor.shutdown()

    private fun vm(admin: Boolean = false) = SolicitudesServidorViewModel(repo, admin,
        SeguimientoSolicitudes(guardados::get) { k, v -> guardados[k] = v }, Dispatchers.Unconfined)

    private fun respuesta(cuerpo: String, codigo: Int = 200) {
        servidor.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo))
    }
    private fun dato(estado: EstadoCotizacionRemota) = """{"id":11,"usuarioId":3,"tokenEnvio":"abc","descripcion":"Tapiz","categoria":"Interiores","material":"Algodón","anchoCm":20,"largoCm":30,"cantidad":2,"creadaEn":12345,"estado":"$estado"}"""
    private fun carga(estado: EstadoCotizacionRemota, detalle: Boolean) {
        respuesta("[${dato(estado)}]")
        if (detalle) respuesta(dato(estado))
    }
    private fun esperar(vm: SolicitudesServidorViewModel) = runBlocking {
        withTimeout(5000) { vm.estado.first { !it.cargando } }
    }
    private fun abrirAdmin(): SolicitudesServidorViewModel {
        val vm = vm(true)
        respuesta("[]")
        carga(PENDIENTE, true)
        vm.conectar("admin", clave, 11)
        assertEquals(PENDIENTE, esperar(vm).detalle?.estado)
        return vm
    }

    @Test fun historialDetalleAvisoUnicoYCambioDeCuenta() {
        val vm = vm()
        respuesta("[]")
        carga(PENDIENTE, false)
        vm.conectar("uno", clave, null)
        assertEquals(11L, esperar(vm).lista.single().id)
        carga(EN_REVISION, true)
        vm.actualizar(11)
        val cambio = esperar(vm)
        assertEquals(EN_REVISION, cambio.detalle?.estado)
        assertTrue(cambio.avisos.single().contains("PENDIENTE → EN_REVISION"))
        val antes = servidor.requestCount
        vm.cambiar(11, EN_REVISION, APROBADA)
        assertEquals(antes, servidor.requestCount)
        carga(EN_REVISION, true)
        vm.actualizar(11)
        assertEquals(cambio.avisos, esperar(vm).avisos)
        vm.leerAvisos()
        vm.desconectar()
        assertEquals(SolicitudesServidorState(), vm.estado.value)
        respuesta("[]")
        carga(APROBADA, false)
        vm.conectar("dos", clave, null)
        assertTrue(esperar(vm).avisos.isEmpty())
        vm.desconectar()
        respuesta("[]")
        carga(APROBADA, false)
        vm.conectar("uno", clave, null)
        assertTrue(esperar(vm).avisos.single().contains("EN_REVISION → APROBADA"))
    }

    @Test fun historialVacioDetalleAjenoYErrores() {
        val vm = vm()
        respuesta("[]")
        respuesta("[]")
        vm.conectar("cliente", clave, null)
        val vacio = esperar(vm)
        assertTrue(vacio.conectada && vacio.lista.isEmpty() && vacio.error == null)
        respuesta("[]")
        respuesta("{}", 404)
        vm.actualizar(99)
        assertTrue(esperar(vm).error!!.contains("404"))
        assertNull(vm.estado.value.detalle)
        respuesta("{}", 503)
        vm.actualizar(null)
        assertTrue(esperar(vm).error!!.contains("503"))
        servidor.shutdown()
        vm.actualizar(null)
        assertTrue(esperar(vm).error!!.contains("servicio"))
    }

    @Test fun credencialesInvalidasYPermisosRevocados() {
        for (admin in listOf(false, true)) {
            val vm = vm(admin)
            respuesta("{}", 401)
            vm.conectar("usuario", clave, null)
            assertTrue(esperar(vm).error!!.contains("401"))
            respuesta("[]")
            carga(PENDIENTE, false)
            vm.conectar("usuario", clave, null)
            assertTrue(esperar(vm).conectada)
            respuesta("{}", 403)
            vm.actualizar(null)
            val fin = esperar(vm)
            assertTrue(fin.error!!.contains("403"))
            assertFalse(fin.conectada)
            assertTrue(fin.lista.isEmpty())
            assertNull(fin.detalle)
        }
    }

    @Test fun transicionesYEstadosFinales() {
        for (fin in listOf(APROBADA, RECHAZADA)) {
            val vm = abrirAdmin()
            val antes = servidor.requestCount
            vm.cambiar(11, PENDIENTE, fin)
            vm.cambiar(12, PENDIENTE, EN_REVISION)
            vm.cambiar(11, EN_REVISION, fin)
            assertEquals(antes, servidor.requestCount)
            for ((desde, hasta) in listOf(PENDIENTE to EN_REVISION, EN_REVISION to fin)) {
                respuesta(dato(hasta))
                carga(hasta, true)
                vm.cambiar(11, desde, hasta)
                val actual = esperar(vm)
                assertEquals(hasta, actual.detalle?.estado)
                assertEquals(hasta, actual.lista.single().estado)
                assertTrue(actual.mensaje!!.contains("confirmado"))
                assertTrue(actual.avisos.isEmpty())
            }
            val cuenta = servidor.requestCount
            vm.cambiar(11, fin, EN_REVISION)
            assertEquals(cuenta, servidor.requestCount)
        }
    }

    @Test fun falloDuranteElCambioNoConfirmaYSePuedeReintentar() {
        val vm = abrirAdmin()
        respuesta("{}", 503)
        vm.cambiar(11, PENDIENTE, EN_REVISION)
        val fallo = esperar(vm)
        assertTrue(fallo.conectada)
        assertEquals(PENDIENTE, fallo.lista.single().estado)
        assertNull(fallo.mensaje)
        assertNull(fallo.detalle)
        assertTrue(fallo.error!!.contains("503"))
        carga(PENDIENTE, true)
        vm.actualizar(11)
        assertEquals(PENDIENTE, esperar(vm).detalle?.estado)
        respuesta("{}", 403)
        vm.cambiar(11, PENDIENTE, EN_REVISION)
        val denegado = esperar(vm)
        assertTrue(denegado.error!!.contains("403"))
        assertFalse(denegado.conectada)
        assertTrue(denegado.lista.isEmpty())
    }

    @Test fun rutasSeparadasPorRolYSesion() {
        val politica = com.example.applouaraya_grupogp.navigation.PoliticaNavegacion
        for (ruta in listOf("solicitudes-servidor", "solicitudes-servidor/{id}")) {
            assertTrue(politica.permite(ruta, 1L, "CLIENTE"))
            assertFalse(politica.permite(ruta, 1L, "ADMINISTRADOR"))
            assertTrue(politica.permite("admin/$ruta", 1L, "ADMINISTRADOR"))
            assertFalse(politica.permite("admin/$ruta", 1L, "CLIENTE"))
            assertFalse(politica.permite(ruta, null, "CLIENTE"))
            assertFalse(politica.permite("admin/$ruta", null, "ADMINISTRADOR"))
        }
    }

    @Test fun conflictoRecargaSinSimularExito() {
        val vm = abrirAdmin()
        respuesta("{}", 409)
        carga(RECHAZADA, true)
        vm.cambiar(11, PENDIENTE, EN_REVISION)
        val s = esperar(vm)
        assertEquals(RECHAZADA, s.detalle?.estado)
        assertEquals(RECHAZADA, s.lista.single().estado)
        assertTrue(s.mensaje!!.contains("409"))
        assertFalse(s.mensaje.contains("confirmado"))
    }
}
