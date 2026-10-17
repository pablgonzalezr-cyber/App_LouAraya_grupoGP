package com.example.applouaraya_grupogp

import com.example.applouaraya_grupogp.navigation.PoliticaNavegacion as Politica
import org.junit.Assert.*
import org.junit.Test

class PoliticaNavegacionTest {
    private val clientes = listOf("dashboard", "catalogo", "favoritos", "cotizacion", "solicitudes", "solicitud/{id}", "novedades", "producto/{id}")
    private val admins = listOf("admin", "admin/solicitudes", "admin/solicitud/{id}", "admin/productos", "admin/contenido")
    @Test fun ningunaRutaPrivadaAdmiteSesionAusenteOInvalida() {
        for (ruta in clientes + admins + "perfil") for (id in listOf(null, 0L, -1L)) for (rol in listOf("CLIENTE", "ADMINISTRADOR")) {
            assertFalse(Politica.permite(ruta, id, rol))
        }
    }
    @Test fun clientesSoloAccedenASusModulosYPerfil() {
        (clientes + "perfil").forEach { assertTrue(Politica.permite(it, 1L, "CLIENTE")) }
        admins.forEach { assertFalse(Politica.permite(it, 1L, "CLIENTE")) }
    }
    @Test fun administradoresAccedenAAdministracionYPerfil() {
        (admins + "perfil").forEach { assertTrue(Politica.permite(it, 1L, "ADMINISTRADOR")) }
        clientes.forEach { assertFalse(Politica.permite(it, 1L, "ADMINISTRADOR")) }
    }
    @Test fun rutasPublicasSonAccesiblesSinSesion() {
        listOf("bienvenida", "login", "registro").forEach { assertTrue(Politica.permite(it, null, "CLIENTE")) }
    }
    @Test fun desconocidosNoOtorganAcceso() {
        (clientes + admins + "perfil").forEach { assertFalse(Politica.permite(it, 1L, "DESCONOCIDO")) }
        assertFalse(Politica.permite("ruta-inexistente", 1L, "ADMINISTRADOR"))
        assertFalse(Politica.permite(null, 1L, "CLIENTE"))
    }
    @Test fun inicioDiferenciado() { assertEquals("dashboard", Politica.inicio("CLIENTE")); assertEquals("admin", Politica.inicio("ADMINISTRADOR")) }
}
