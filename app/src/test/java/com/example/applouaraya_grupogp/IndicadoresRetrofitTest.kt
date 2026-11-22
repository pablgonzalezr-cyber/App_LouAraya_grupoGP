package com.example.applouaraya_grupogp

import com.example.applouaraya_grupogp.repository.*
import java.time.LocalDate
import kotlinx.coroutines.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*

class IndicadoresRetrofitTest {
    private val servidor = MockWebServer()
    private var ahora = 0L
    private lateinit var repo: IndicadoresRepository
    private val valido = """{"dolar":{"codigo":"dolar","unidad_medida":"Pesos","fecha":"2026-11-20T03:00:00.000Z","valor":1000.00}}"""

    @Before fun iniciar() {
        servidor.start()
        repo = IndicadoresRepository(IndicadoresRepository.servicio(servidor.url("/").toString())) { ahora }
    }
    @After fun cerrar() = servidor.shutdown()

    @Test fun cotizacionYRedondeoHalfUp() = runBlocking {
        servidor.enqueue(MockResponse().setBody(valido))
        val dolar = requireNotNull(repo.obtener().indicador)
        assertEquals(LocalDate.of(2026, 11, 20), dolar.fecha)
        assertEquals("24.99", dolar.convertir(24990).toPlainString())
        assertEquals("1.01", dolar.convertir(1005).toPlainString())
        assertEquals("0.00", dolar.convertir(0).toPlainString())
        val peticion = servidor.takeRequest()
        assertEquals("GET", peticion.method)
        assertEquals("/api", peticion.path)
        assertNull(peticion.getHeader("Authorization"))
    }

    @Test fun concurrenciaCacheCaducidadYReintento() = runBlocking {
        servidor.enqueue(MockResponse().setBody(valido))
        val datos = (1..6).map { async { repo.obtener() } }.awaitAll()
        assertTrue(datos.all { it == datos.first() })
        repo.obtener(actualizar = true)
        ahora += 59 * 60_000
        repo.obtener()
        assertEquals(1, servidor.requestCount)
        ahora += 60_000
        servidor.enqueue(MockResponse().setResponseCode(503))
        val antigua = repo.obtener()
        assertNotNull(antigua.error)
        assertEquals(datos.first().indicador, antigua.indicador)
        repo.obtener(true)
        assertEquals(2, servidor.requestCount)
        ahora += 30_000
        servidor.enqueue(MockResponse().setBody(valido.replace("1000.00", "900")))
        assertNull(repo.obtener(true).error)
        assertEquals(3, servidor.requestCount)
    }

    @Test fun jsonInvalidoNulosYValoresNoPositivos() = runBlocking {
        for (body in listOf("{}", "null", "malformado", valido.replace("1000.00","null"),
                valido.replace("1000.00","0"), valido.replace("1000.00","-2"),
                valido.replace("03:00:00.000Z","invalida"), valido.replace("Pesos","Dólares"),
                valido.replace("\"codigo\":\"dolar\"", "\"codigo\":\"euro\""))) {
            servidor.enqueue(MockResponse().setBody(body))
            val resultado = repo.obtener(true)
            assertNull(resultado.indicador)
            assertNotNull(resultado.error)
            ahora += 31_000
        }
    }

    @Test fun sinConexionNoInventaPrecio() = runBlocking {
        servidor.shutdown()
        val resultado = repo.obtener()
        assertNull(resultado.indicador)
        assertNotNull(resultado.error)
    }
}
