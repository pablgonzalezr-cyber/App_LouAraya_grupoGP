package com.example.applouaraya_grupogp

import com.example.applouaraya_grupogp.model.PuntoDemostracion
import com.example.applouaraya_grupogp.repository.PuntosDemostracion
import com.example.applouaraya_grupogp.viewmodel.MapaViewModel
import org.junit.Assert.*
import org.junit.Test

class MapaTest {
    @Test fun puntosSonFicticiosYNoAfirmanDireccionesConfirmadas() {
        assertEquals(3,PuntosDemostracion.puntos.size)
        assertEquals(3,PuntosDemostracion.puntos.map { it.id }.distinct().size)
        PuntosDemostracion.puntos.forEach { p -> assertTrue(p.nombre.contains("demo")); assertTrue(p.direccion.contains("Sin dirección real confirmada")) }
    }
    @Test fun coordenadasInvalidasSeRechazan() {
        listOf(Double.NaN,Double.POSITIVE_INFINITY,91.0,-91.0).forEach { lat ->
            assertThrows(IllegalArgumentException::class.java) { PuntoDemostracion("id","Punto ficticio","Descripción","Sin dirección",lat,0.0) }
        }
        assertThrows(IllegalArgumentException::class.java) { PuntoDemostracion("id","Punto ficticio","Descripción","Sin dirección",0.0,181.0) }
    }
    @Test fun indicacionesUsanCoordenadasSinSolicitarOrigen() {
        val p=PuntosDemostracion.puntos.first()
        assertEquals("google.navigation:q=-33.442,-70.654",PuntosDemostracion.navegacion(p))
        assertTrue(PuntosDemostracion.ubicacion(p).startsWith("geo:-33.442,-70.654?q=-33.442,-70.654("))
        assertFalse(PuntosDemostracion.ubicacion(p).contains(" "))
    }
    @Test fun seleccionYReintentoConservanLosPuntos() {
        val vm=MapaViewModel()
        vm.seleccionar("inexistente");assertNull(vm.estado.value.seleccionado)
        vm.seleccionar("feria-a");assertEquals("feria-a",vm.estado.value.seleccionado)
        vm.error("Sin red");assertFalse(vm.estado.value.cargando)
        vm.reintentar();assertTrue(vm.estado.value.cargando);assertNull(vm.estado.value.error)
        vm.listo();assertFalse(vm.estado.value.cargando)
    }
}
