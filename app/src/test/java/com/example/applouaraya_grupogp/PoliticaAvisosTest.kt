package com.example.applouaraya_grupogp

import com.example.applouaraya_grupogp.navigation.PoliticaNavegacion
import com.example.applouaraya_grupogp.repository.*
import org.junit.Assert.*
import org.junit.Test

class PoliticaAvisosTest {
    @Test fun destinosRespetanRoles() {
        assertEquals("solicitud/7", PoliticaAvisos.ruta("ESTADO", "CLIENTE", 7))
        assertEquals("admin/solicitud/7", PoliticaAvisos.ruta("NUEVA_SOLICITUD", "ADMINISTRADOR", 7))
        assertEquals("novedades", PoliticaAvisos.ruta("CAMPANIA", "CLIENTE", 7))
        assertNull(PoliticaAvisos.ruta("ESTADO", "ADMINISTRADOR", 7))
        assertNull(PoliticaAvisos.ruta("NUEVA_SOLICITUD", "CLIENTE", 7))
        assertNull(PoliticaAvisos.ruta("CAMPANIA", "ADMINISTRADOR", 7))
    }
    @Test fun referenciasYTiposInvalidosNoNavegan() {
        assertNull(PoliticaAvisos.ruta("ESTADO", "CLIENTE", 0))
        assertNull(PoliticaAvisos.ruta("ESTADO", "CLIENTE", -1))
        assertNull(PoliticaAvisos.ruta("DESCONOCIDO", "CLIENTE", 2))
        assertNull(PoliticaAvisos.ruta("ESTADO", "OTRO", 2))
    }
    @Test fun preferenciasSeparanCampaniasYSolicitudes() {
        val preferencias = PreferenciasLocales(avisosCotizaciones = false, avisosCampanias = true)
        assertFalse(PoliticaAvisos.habilitado("ESTADO", preferencias))
        assertFalse(PoliticaAvisos.habilitado("NUEVA_SOLICITUD", preferencias))
        assertTrue(PoliticaAvisos.habilitado("CAMPANIA", preferencias))
        assertFalse(PoliticaAvisos.habilitado("OTRO", preferencias))
        assertFalse(PoliticaAvisos.habilitado("CAMPANIA", preferencias.copy(avisosCampanias = false)))
    }
    @Test fun centroExigeSesionYRolValido() {
        listOf("CLIENTE", "ADMINISTRADOR").forEach { rol ->
            assertTrue(PoliticaNavegacion.permite("avisos", 1, rol))
            assertFalse(PoliticaNavegacion.permite("avisos", null, rol))
        }
        assertFalse(PoliticaNavegacion.permite("avisos", 1, "OTRO"))
    }
}
