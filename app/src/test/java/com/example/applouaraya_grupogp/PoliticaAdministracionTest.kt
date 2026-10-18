package com.example.applouaraya_grupogp

import com.example.applouaraya_grupogp.model.RolUsuario
import com.example.applouaraya_grupogp.repository.EstadoCotizacion
import com.example.applouaraya_grupogp.repository.PoliticaAdministracion as Politica
import org.junit.Assert.*
import org.junit.Test

class PoliticaAdministracionTest {
    @Test fun rolDesconocidoNuncaOtorgaPermisos() {
        listOf(null, "", "admin", "CLIENTE", "ADMINISTRADOR ", "OTRO").forEach {
            assertFalse(Politica.esAdministrador(it))
            assertEquals(RolUsuario.CLIENTE, RolUsuario.desde(it))
        }
        assertTrue(Politica.esAdministrador("ADMINISTRADOR"))
        assertEquals(RolUsuario.ADMINISTRADOR, RolUsuario.desde("ADMINISTRADOR"))
    }
    @Test fun todasLasTransicionesSeValidan() {
        val permitidas = setOf("PENDIENTE" to "EN_REVISION", "EN_REVISION" to "APROBADA", "EN_REVISION" to "RECHAZADA")
        for (actual in EstadoCotizacion.entries) for (nuevo in EstadoCotizacion.entries) {
            if ((actual.name to nuevo.name) in permitidas) Politica.validarCambio("ADMINISTRADOR", actual.name, nuevo)
            else assertThrows(IllegalArgumentException::class.java) { Politica.validarCambio("ADMINISTRADOR", actual.name, nuevo) }
        }
    }
    @Test fun clienteOSesionAusenteNoPuedenCambiarNingunEstado() {
        for (rol in listOf(null, "CLIENTE", "desconocido")) for (actual in EstadoCotizacion.entries) for (nuevo in EstadoCotizacion.entries) {
            assertThrows(SecurityException::class.java) { Politica.validarCambio(rol, actual.name, nuevo) }
        }
    }
    @Test fun estadosFinalesYDesconocidosNoTienenDestinos() {
        listOf("APROBADA", "RECHAZADA", "desconocido", "").forEach { assertTrue(Politica.destinos(it).isEmpty()) }
    }
}
